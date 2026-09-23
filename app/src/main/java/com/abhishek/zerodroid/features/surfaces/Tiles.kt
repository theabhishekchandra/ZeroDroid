package com.abhishek.zerodroid.features.surfaces

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.abhishek.zerodroid.MainActivity
import com.abhishek.zerodroid.core.notify.DeepLinkBus
import com.abhishek.zerodroid.core.sessions.SessionRepository
import com.abhishek.zerodroid.features.sweep.domain.SweepPreset
import com.abhishek.zerodroid.features.sweep.viewmodel.SweepViewModel
import com.abhishek.zerodroid.features.watch.data.WatchRuleStore
import com.abhishek.zerodroid.features.watch.service.WatchService
import com.abhishek.zerodroid.navigation.ZeroDroidScreen
import com.abhishek.zerodroid.navigation.sweepRunRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** Shared tile plumbing: open the app at a route and collapse the shade. */
abstract class RouteTile : TileService() {

    @SuppressLint("StartActivityAndCollapseDeprecated")
    protected fun openRoute(route: String) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(DeepLinkBus.EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, route.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    protected fun render(state: Int, subtitle: String) {
        val tile = qsTile ?: return
        tile.state = state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        tile.updateTile()
    }
}

class SweepTile : RouteTile() {
    override fun onStartListening() = render(Tile.STATE_ACTIVE, "Tap to start")
    override fun onClick() = openRoute(sweepRunRoute(SweepPreset.FULL, ""))
}

@AndroidEntryPoint
class TrackerCheckTile : RouteTile() {
    @Inject lateinit var sessions: SessionRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        render(Tile.STATE_ACTIVE, "Tap to scan")
        scope.launch {
            val last = runCatching {
                sessions.sessions.first().firstOrNull { it.tool == SweepViewModel.SESSION_TOOL && it.title == SweepPreset.TRACKER_CHECK.title }
            }.getOrNull() ?: return@launch
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(last.startedAt))
            render(Tile.STATE_ACTIVE, if (last.findingCount == 0) "Clear · $time" else "${last.findingCount} found · $time")
        }
    }

    override fun onClick() = openRoute(sweepRunRoute(SweepPreset.TRACKER_CHECK, ""))

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

/**
 * On: tap pauses every rule right here. Off: un-pauses and opens the app, which starts the
 * service on resume, since Android only lets a visible app start background location and
 * Bluetooth work.
 */
@AndroidEntryPoint
class WatchTile : RouteTile() {
    @Inject lateinit var store: WatchRuleStore

    override fun onStartListening() {
        val enabled = store.rules.value.count { it.enabled }
        when {
            store.active.isNotEmpty() -> render(Tile.STATE_ACTIVE, "$enabled on")
            enabled > 0 -> render(Tile.STATE_INACTIVE, "Paused")
            else -> render(Tile.STATE_INACTIVE, "Off")
        }
    }

    override fun onClick() {
        if (store.active.isNotEmpty()) {
            store.setPaused(true)
            WatchService.sync(this, store)
            onStartListening()
        } else {
            store.setPaused(false)
            openRoute("rules")
        }
    }
}

class NfcTile : RouteTile() {
    override fun onStartListening() = render(Tile.STATE_ACTIVE, "Opens reader")
    override fun onClick() = openRoute(ZeroDroidScreen.Nfc.route)
}

/** Long-pressing a tile opens the matching screen. */
object TileRoutes {
    fun forComponent(className: String?): String? = when (className) {
        SweepTile::class.java.name -> ZeroDroidScreen.Sweep.route
        TrackerCheckTile::class.java.name -> ZeroDroidScreen.BluetoothTracker.route
        WatchTile::class.java.name -> "rules"
        NfcTile::class.java.name -> ZeroDroidScreen.Nfc.route
        else -> null
    }
}
