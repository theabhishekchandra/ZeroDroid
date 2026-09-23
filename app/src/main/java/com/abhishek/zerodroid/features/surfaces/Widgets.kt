package com.abhishek.zerodroid.features.surfaces

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.abhishek.zerodroid.MainActivity
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.notify.DeepLinkBus
import com.abhishek.zerodroid.core.sessions.Session
import com.abhishek.zerodroid.core.sessions.SessionRepository
import com.abhishek.zerodroid.features.sweep.domain.SweepPreset
import com.abhishek.zerodroid.features.sweep.viewmodel.SweepViewModel
import com.abhishek.zerodroid.features.watch.data.WatchRuleStore
import com.abhishek.zerodroid.features.watch.domain.BatteryCost
import com.abhishek.zerodroid.navigation.ZeroDroidScreen
import com.abhishek.zerodroid.navigation.sweepRunRoute
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SurfacesEntryPoint {
    fun alerts(): AlertCenterRepository
    fun sessions(): SessionRepository
    fun watchStore(): WatchRuleStore
}

/** What the widgets show; plain data so the text rules are testable. */
data class WidgetSnapshot(
    val openAlerts: Int,
    val lastSweep: Session?,
    val lastTrackerCheck: Session?,
    val activeRules: Int,
    val batteryLabel: String,
    val now: Long
) {
    val alertsLine: String
        get() = when (openAlerts) {
            0 -> "All clear"
            1 -> "1 open alert"
            else -> "$openAlerts open alerts"
        }

    val sweepLine: String
        get() = lastSweep?.let { "Last sweep ${ago(it.startedAt, now)}${it.place?.let { p -> " · $p" } ?: ""}" } ?: "No sweeps yet"

    val watchLine: String?
        get() = if (activeRules == 0) null else "Watching · $activeRules rule${if (activeRules > 1) "s" else ""} · $batteryLabel"

    val trackerTitle: String
        get() = when {
            lastTrackerCheck == null -> "Tracker check"
            lastTrackerCheck.findingCount == 0 -> "No trackers"
            else -> "${lastTrackerCheck.findingCount} found"
        }

    val trackerSubtitle: String
        get() = lastTrackerCheck?.let { "Checked ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.startedAt))}" } ?: "Not run yet"

    companion object {
        fun ago(then: Long, now: Long): String {
            val min = ((now - then) / 60_000).coerceAtLeast(0)
            return when {
                min < 1 -> "just now"
                min < 60 -> "${min}m ago"
                min < 24 * 60 -> "${min / 60}h ago"
                else -> "${min / (24 * 60)}d ago"
            }
        }

        suspend fun load(context: Context): WidgetSnapshot {
            val ep = EntryPointAccessors.fromApplication(context.applicationContext, SurfacesEntryPoint::class.java)
            val sessions = runCatching { ep.sessions().sessions.first() }.getOrDefault(emptyList())
            val sweeps = sessions.filter { it.tool == SweepViewModel.SESSION_TOOL }
            val active = ep.watchStore().active
            return WidgetSnapshot(
                openAlerts = runCatching { ep.alerts().openAlerts.first().size }.getOrDefault(0),
                lastSweep = sweeps.firstOrNull(),
                lastTrackerCheck = sweeps.firstOrNull { it.title == SweepPreset.TRACKER_CHECK.title },
                activeRules = active.size,
                batteryLabel = if (active.any { it.kind.battery == BatteryCost.MEDIUM }) "medium battery" else "low battery",
                now = System.currentTimeMillis()
            )
        }
    }
}

private val bg = ColorProvider(Color(0xFF0B0D0C))
private val surface = ColorProvider(Color(0xFF182019))
private val text = ColorProvider(Color(0xFFE6ECE8))
private val text3 = ColorProvider(Color(0xFF7C8982))
private val accent = ColorProvider(Color(0xFF52E08A))
private val accentBg = ColorProvider(Color(0xFF13301F))
private val high = ColorProvider(Color(0xFFFF8A4C))

private fun open(context: Context, route: String): Action = actionStartActivity(
    Intent(context, MainActivity::class.java)
        // A distinct data URI keeps each button's PendingIntent from overwriting the others.
        .setData("zerodroid://widget/${Uri.encode(route)}".toUri())
        .putExtra(DeepLinkBus.EXTRA_ROUTE, route)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
)

/** Status at a glance plus the three most-used actions. */
class StatusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = WidgetSnapshot.load(context)
        provideContent { StatusContent(context, snap) }
    }
}

@Composable
private fun StatusContent(context: Context, s: WidgetSnapshot) {
    Column(
        GlanceModifier.fillMaxSize().background(bg).cornerRadius(22.dp).padding(14.dp)
            .clickable(open(context, ZeroDroidScreen.AlertCenter.route))
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text(s.alertsLine, style = TextStyle(color = if (s.openAlerts > 0) high else accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                Text(s.sweepLine, style = TextStyle(color = text3, fontSize = 12.sp), maxLines = 1)
            }
            Text("zd", style = TextStyle(color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
        }
        Spacer(GlanceModifier.height(10.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            WidgetButton("Sweep", open(context, ZeroDroidScreen.Sweep.route), primary = true, modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            WidgetButton("Trackers", open(context, sweepRunRoute(SweepPreset.TRACKER_CHECK, "")), modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            WidgetButton("WiFi", open(context, ZeroDroidScreen.Wifi.route), modifier = GlanceModifier.defaultWeight())
        }
        s.watchLine?.let {
            Spacer(GlanceModifier.height(8.dp))
            Text(it, style = TextStyle(color = text3, fontSize = 11.sp, fontFamily = FontFamily.Monospace), maxLines = 1)
        }
    }
}

@Composable
private fun WidgetButton(label: String, action: Action, primary: Boolean = false, modifier: GlanceModifier = GlanceModifier) {
    Row(
        modifier.height(40.dp).background(if (primary) accentBg else surface).cornerRadius(10.dp).clickable(action),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = TextStyle(color = if (primary) accent else text, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
    }
}

/** Last tracker check; tap to run another. */
class TrackerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = WidgetSnapshot.load(context)
        provideContent {
            val clear = snap.lastTrackerCheck?.findingCount == 0
            Column(
                GlanceModifier.fillMaxSize().background(bg).cornerRadius(22.dp).padding(14.dp)
                    .clickable(open(context, sweepRunRoute(SweepPreset.TRACKER_CHECK, "")))
            ) {
                Text(snap.trackerTitle, style = TextStyle(color = if (snap.lastTrackerCheck == null || clear) accent else high, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                Text(snap.trackerSubtitle, style = TextStyle(color = text3, fontSize = 12.sp))
                Spacer(GlanceModifier.defaultWeight())
                Text("Tap to re-check", style = TextStyle(color = text, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
            }
        }
    }
}

class StatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatusWidget()
}

class TrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TrackerWidget()
}

object Widgets {
    suspend fun refresh(context: Context) {
        runCatching { StatusWidget().updateAll(context) }
        runCatching { TrackerWidget().updateAll(context) }
    }
}
