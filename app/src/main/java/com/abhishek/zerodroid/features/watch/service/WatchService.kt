package com.abhishek.zerodroid.features.watch.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.core.notify.ZdNotifier
import com.abhishek.zerodroid.core.prefs.AppSettings
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerIdentifier
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerType
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RiskLevel
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApAnalyzer
import com.abhishek.zerodroid.features.watch.data.WatchRuleStore
import com.abhishek.zerodroid.features.watch.domain.AlertThrottle
import com.abhishek.zerodroid.features.watch.domain.BatteryCost
import com.abhishek.zerodroid.features.watch.domain.CellDowngradeWatcher
import com.abhishek.zerodroid.features.watch.domain.DisconnectWatcher
import com.abhishek.zerodroid.features.watch.domain.DwellWatcher
import com.abhishek.zerodroid.features.watch.domain.FollowMeWatcher
import com.abhishek.zerodroid.features.watch.domain.Heard
import com.abhishek.zerodroid.features.watch.domain.LatLon
import com.abhishek.zerodroid.features.watch.domain.RuleKind
import com.abhishek.zerodroid.features.watch.domain.RuleSensor
import com.abhishek.zerodroid.features.watch.domain.WatchRule
import com.abhishek.zerodroid.features.watch.domain.formatMinutes
import com.abhishek.zerodroid.features.watch.domain.networkGeneration
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import com.abhishek.zerodroid.navigation.ZeroDroidScreen
import com.abhishek.zerodroid.navigation.locateRoute
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Runs watch rules with short, spaced-out scans: a 10 s Bluetooth listen and one WiFi scan per
 * minute, plus passive network callbacks. A foreground service, so Android shows that it's on.
 */
@AndroidEntryPoint
class WatchService : Service() {

    @Inject lateinit var store: WatchRuleStore
    @Inject lateinit var bleScanner: BleScanner
    @Inject lateinit var wifiScanner: WifiScanner
    @Inject lateinit var alerts: AlertCenterRepository
    @Inject lateinit var settings: AppSettings
    @Inject lateinit var notifier: ZdNotifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private val throttle = AlertThrottle()
    private val trackerIdentifier = TrackerIdentifier()
    private val rogueAnalyzer = RogueApAnalyzer()
    private val followMe = FollowMeWatcher()
    private val dwell = mutableMapOf<String, Pair<WatchRule, DwellWatcher>>()
    private val disconnects = DisconnectWatcher()
    private val cell = CellDowngradeWatcher()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastWifiRssi: Int? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PAUSE) {
            store.setPaused(true)
            stopSelf()
            return START_NOT_STICKY
        }
        if (store.active.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(this, ZdNotifier.WATCH_ID, notification(store.active), serviceTypes())
        } catch (e: Exception) {
            // Not allowed from the background (Android 12+) or missing permission: nothing to run.
            stopSelf()
            return START_NOT_STICKY
        }
        if (loop == null) start()
        return START_STICKY
    }

    private fun start() {
        // Keep the ongoing notification in step with the rules, and stop when none are left.
        scope.launch {
            combine(store.rules, store.paused) { _, _ -> store.active }.collect { active ->
                if (active.isEmpty()) stopSelf()
                else notifier.watching(summary(active), pauseIntent()).let {
                    ContextCompat.getSystemService(this@WatchService, android.app.NotificationManager::class.java)
                        ?.notify(ZdNotifier.WATCH_ID, it)
                }
                syncDeauthCallback(active.any { it.kind == RuleKind.DEAUTH })
            }
        }
        loop = scope.launch {
            while (isActive) {
                val active = store.active
                runCatching { cycle(active) }
                delay(CYCLE_MS)
            }
        }
    }

    private suspend fun cycle(active: List<WatchRule>) {
        val now = System.currentTimeMillis()
        val kinds = active.map { it.kind }.toSet()
        val mine = settings.myDevices.value

        if (RuleKind.FOLLOW_ME in kinds || RuleKind.CUSTOM_BLE in kinds) {
            val devices = scanBle().filter { it.address.uppercase() !in mine }
            if (RuleKind.FOLLOW_ME in kinds) followMe(now, devices)
            customRules(now, devices, active.filter { it.kind == RuleKind.CUSTOM_BLE })
        }
        if (RuleKind.ROGUE_TRUSTED in kinds) rogue(now)
        if (RuleKind.CELL_2G in kinds) cell(now)
    }

    private suspend fun followMe(now: Long, devices: List<BleDevice>) {
        val trackers = devices.mapNotNull { d ->
            val type = trackerIdentifier.identify(d)
            if (type == TrackerType.UNKNOWN) null else Heard(d.address, type.label, d.rssi)
        }
        followMe.update(now, trackers, lastLocation()).forEach { hit ->
            val where = hit.movedMetres?.let { " while you moved ${if (it >= 1000) "%.1f km".format(it / 1000) else "${it.toInt()} m"}" } ?: " across ${hit.sightings} checks"
            val detail = "A ${hit.heard.label} has been near you for ${formatMinutes(hit.durationMs)}$where."
            alerts.record(AlertSource.BLUETOOTH_TRACKER, AlertSeverity.HIGH, "Unknown ${hit.heard.label} moving with you", detail)
            notifier.postTracker(
                address = hit.heard.key,
                label = hit.heard.label,
                detail = detail,
                investigateRoute = ZeroDroidScreen.BluetoothTracker.route,
                locateRoute = locateRoute(hit.heard.key, hit.heard.label)
            )
        }
    }

    private suspend fun customRules(now: Long, devices: List<BleDevice>, rules: List<WatchRule>) {
        dwell.keys.retainAll(rules.map { it.id }.toSet())
        val heard = devices.map { Heard(it.address, it.name ?: "Unnamed device", it.rssi) }
        rules.forEach { rule ->
            val (known, watcher) = dwell[rule.id] ?: (rule to DwellWatcher(rule.rssiThreshold, rule.minutes * 60_000L))
            val w = if (known == rule) watcher else DwellWatcher(rule.rssiThreshold, rule.minutes * 60_000L)
            dwell[rule.id] = rule to w
            w.update(now, heard).forEach { hit ->
                if (!throttle.allow("${rule.id}:${hit.heard.key}", now)) return@forEach
                val detail = "${hit.heard.label} (${hit.heard.key}) has been stronger than ${rule.rssiThreshold} dBm for ${formatMinutes(hit.durationMs)}."
                alerts.record(AlertSource.BLE_WATCH, AlertSeverity.MEDIUM, "Unknown BLE device nearby", detail)
                notifier.postAlert("${rule.id}:${hit.heard.key}", "Unknown BLE device nearby", detail, locateRoute(hit.heard.key, hit.heard.label), "Device nearby")
            }
        }
    }

    private suspend fun rogue(now: Long) {
        val trusted = settings.trustedNetworks.value
        if (trusted.isEmpty()) return
        val aps = withTimeoutOrNull(20_000) { wifiScanner.scan().catch { }.firstOrNull() } ?: return
        rogueAnalyzer.analyze(aps, trusted)
            .filter { (it.riskLevel == RiskLevel.CRITICAL || it.riskLevel == RiskLevel.HIGH) && it.suspiciousAp.ssid in trusted }
            .forEach { alert ->
                if (!throttle.allow("rogue:${alert.suspiciousAp.bssid}", now)) return@forEach
                val title = "${alert.threatType.label}: ${alert.suspiciousAp.ssid}"
                alerts.record(AlertSource.ROGUE_AP, AlertSeverity.HIGH, title, alert.description)
                notifier.postAlert("rogue:${alert.suspiciousAp.bssid}", title, alert.description, ZeroDroidScreen.RogueAp.route, "Network alert")
            }
    }

    @SuppressLint("MissingPermission")
    private suspend fun cell(now: Long) {
        if (!granted(Manifest.permission.READ_PHONE_STATE)) return
        val tm = getSystemService(TelephonyManager::class.java) ?: return
        val generation = networkGeneration(runCatching { tm.dataNetworkType }.getOrDefault(0))
        if (cell.update(generation) && throttle.allow("cell2g", now)) {
            val title = "Network forced down to 2G"
            val detail = "Your phone dropped from a faster network to 2G. Fake base stations (IMSI catchers) do this to weaken encryption; it can also just be poor coverage."
            alerts.record(AlertSource.CELL, AlertSeverity.HIGH, title, detail)
            notifier.postAlert("cell2g", title, detail, ZeroDroidScreen.CellTower.route, "Network alert")
        }
    }

    /** Deauth watching is passive: count WiFi drops that happen while the signal is good. */
    private fun syncDeauthCallback(on: Boolean) {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        if (on && networkCallback == null) {
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        caps.signalStrength.takeIf { it != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED }?.let { lastWifiRssi = it }
                    }
                }

                override fun onLost(network: Network) {
                    val now = System.currentTimeMillis()
                    val drops = disconnects.onDisconnect(now, lastWifiRssi) ?: return
                    if (!throttle.allow("deauth", now)) return
                    scope.launch {
                        val title = "Repeated WiFi disconnects"
                        val detail = "Dropped $drops times in 10 min with a good signal (${lastWifiRssi} dBm). That’s the pattern of a deauthentication attack."
                        alerts.record(AlertSource.DEAUTH, AlertSeverity.HIGH, title, detail)
                        notifier.postAlert("deauth", title, detail, ZeroDroidScreen.DeauthDetector.route, "Network alert")
                    }
                }
            }
            runCatching {
                cm.registerNetworkCallback(NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), cb)
                networkCallback = cb
            }
        } else if (!on) {
            networkCallback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
            networkCallback = null
        }
    }

    private suspend fun scanBle(): List<BleDevice> =
        bleScanner.scan(timeoutMs = BLE_LISTEN_MS).catch { }.toList().lastOrNull().orEmpty()

    @SuppressLint("MissingPermission")
    private suspend fun lastLocation(): LatLon? {
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION) && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        return withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine { cont ->
                LocationServices.getFusedLocationProviderClient(this@WatchService).lastLocation
                    .addOnSuccessListener { cont.resume(it?.let { l -> LatLon(l.latitude, l.longitude) }) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun serviceTypes(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        var types = 0
        if (granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        val bt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) granted(Manifest.permission.BLUETOOTH_SCAN) else true
        if (bt) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        return types
    }

    private fun notification(active: List<WatchRule>) = notifier.watching(summary(active), pauseIntent())

    private fun pauseIntent(): PendingIntent = PendingIntent.getService(
        this, 0, Intent(this, WatchService::class.java).setAction(ACTION_PAUSE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    override fun onDestroy() {
        syncDeauthCallback(false)
        scope.cancel()
        loop = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PAUSE = "com.abhishek.zerodroid.watch.PAUSE"
        const val CYCLE_MS = 60_000L
        const val BLE_LISTEN_MS = 10_000L

        fun summary(active: List<WatchRule>): String {
            val names = active.map { it.title }
            val cost = if (active.any { it.kind.battery == BatteryCost.MEDIUM }) "medium battery use" else "low battery use"
            return (names.take(2) + listOfNotNull(if (names.size > 2) "+${names.size - 2} more" else null) + cost).joinToString(" · ")
        }

        /** Permissions a set of rules needs before the service can do its job. */
        fun sensorsFor(rules: List<WatchRule>): Set<RuleSensor> = rules.flatMap { it.kind.sensors }.toSet()

        /** Starts or stops the service to match the rules; call from a visible screen. */
        fun sync(context: Context, store: WatchRuleStore) {
            val intent = Intent(context, WatchService::class.java)
            if (store.active.isEmpty()) {
                context.stopService(intent)
            } else {
                runCatching { ContextCompat.startForegroundService(context, intent) }
            }
        }
    }
}
