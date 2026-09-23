package com.abhishek.zerodroid.core.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.abhishek.zerodroid.MainActivity
import com.abhishek.zerodroid.R
import com.abhishek.zerodroid.core.prefs.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every notification ZeroDroid posts. Alerts are private on the lock screen by default: the
 * public version says only what kind of thing happened, never device names or places.
 */
@Singleton
class ZdNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: AppSettings
) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_ALERTS, "Threat alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Trackers following you, rogue access points and other watch-rule alerts"
                },
                NotificationChannel(CHANNEL_RESULTS, "Scan results", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "A sweep finished while you were in another app"
                },
                NotificationChannel(CHANNEL_WATCHING, "Background watching", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shown while watch rules run"
                }
            )
        )
    }

    val canNotify: Boolean
        get() = manager.areNotificationsEnabled() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private val appInForeground: Boolean
        get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    /** Opens the app at [route]. */
    fun openRoute(route: String): PendingIntent = PendingIntent.getActivity(
        context,
        route.hashCode(),
        Intent(context, MainActivity::class.java)
            .putExtra(DeepLinkBus.EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun alertBuilder(title: String, text: String, route: String, publicTitle: String) =
        NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openRoute(route))
            .applyLockScreenPrivacy(publicTitle)

    private fun NotificationCompat.Builder.applyLockScreenPrivacy(publicTitle: String) = apply {
        if (settings.hideOnLockScreen.value) {
            setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ALERTS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(publicTitle)
                    .setContentText("Unlock to see details")
                    .build()
            )
        } else {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
    }

    /** A tracker moving with the user, with Investigate / It's mine / Locate. */
    fun postTracker(address: String, label: String, detail: String, investigateRoute: String, locateRoute: String) {
        val id = ("tracker:" + address).hashCode()
        val mine = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_MINE)
                .putExtra(NotificationActionReceiver.EXTRA_ADDRESS, address)
                .putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = alertBuilder("Unknown tracker moving with you", detail, investigateRoute, "Possible tracker nearby")
            .setSubText(label)
            .addAction(0, "Investigate", openRoute(investigateRoute))
            .addAction(0, "It’s mine", mine)
            .addAction(0, "Locate", openRoute(locateRoute))
            .build()
        notify(id, n)
    }

    /** Any other watch-rule alert. */
    fun postAlert(key: String, title: String, text: String, route: String, publicTitle: String = "Security alert") {
        notify(key.hashCode(), alertBuilder(title, text, route, publicTitle).build())
    }

    /** Only when the person has left the app; otherwise they're looking at the report. */
    fun postSweepFinished(sessionId: String, place: String?, high: Int, medium: Int) {
        if (appInForeground) return
        val total = high + medium
        val title = if (total == 0) "Sweep finished: nothing needs a look" else "Sweep finished: $total thing${if (total > 1) "s" else ""} need a look"
        val parts = listOfNotNull(place, if (high > 0) "$high high" else null, if (medium > 0) "$medium medium" else null)
        val text = (parts.joinToString(", ").let { if (it.isEmpty()) "" else "$it · " }) + "tap to see the report."
        val route = "session/$sessionId"
        val n = NotificationCompat.Builder(context, CHANNEL_RESULTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openRoute(route))
            .addAction(0, "View report", openRoute(route))
            .applyLockScreenPrivacy("Sweep finished")
            .build()
        notify(SWEEP_ID, n)
    }

    /** The ongoing notification a foreground watch service must show. */
    fun watching(summary: String, pause: PendingIntent): Notification =
        NotificationCompat.Builder(context, CHANNEL_WATCHING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Watching in background")
            .setContentText(summary)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openRoute(RULES_ROUTE))
            .addAction(0, "Pause all", pause)
            .addAction(0, "Rules", openRoute(RULES_ROUTE))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    fun cancel(id: Int) = manager.cancel(id)

    private fun notify(id: Int, n: Notification) {
        if (!canNotify) return
        try {
            manager.notify(id, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    companion object {
        const val CHANNEL_ALERTS = "zd_alerts"
        const val CHANNEL_RESULTS = "zd_results"
        const val CHANNEL_WATCHING = "zd_watching"
        const val WATCH_ID = 2001
        const val SWEEP_ID = 2002
        const val RULES_ROUTE = "rules"
    }
}
