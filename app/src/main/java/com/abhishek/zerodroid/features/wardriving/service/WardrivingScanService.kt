package com.abhishek.zerodroid.features.wardriving.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.abhishek.zerodroid.R
import com.abhishek.zerodroid.features.wardriving.data.WardrivingRepository
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingCollector
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingSessionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

// Owns the GPS+WiFi collection loop directly so it keeps running independent of whatever screen
// is currently visible — a foreground service that only shows a notification while the real work
// happens in a ViewModel's viewModelScope gets killed the moment that ViewModel is cleared (e.g.
// navigating to any other screen), which defeats the point of "background" logging entirely.
@AndroidEntryPoint
class WardrivingScanService : Service() {

    @Inject lateinit var collector: WardrivingCollector
    @Inject lateinit var repository: WardrivingRepository
    @Inject lateinit var sessionState: WardrivingSessionState

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var collectJob: Job? = null

    companion object {
        const val CHANNEL_ID = "wardriving_scan"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_SESSION_ID = "session_id"
        const val ACTION_STOP = "com.abhishek.zerodroid.wardriving.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())

        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)
        if (sessionId != null && collectJob == null) {
            collectJob = serviceScope.launch {
                collector.collect()
                    .catch { stopSelf() }
                    .collect { records -> repository.saveRecords(sessionId, records) }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        collectJob?.cancel()
        serviceScope.cancel()
        sessionState.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Wardriving Scan",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active wardriving scan in progress"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wardriving Active")
            .setContentText("Scanning Wi-Fi networks with GPS logging")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .addAction(
                0,
                "Stop",
                PendingIntent.getService(
                    this, 0, Intent(this, WardrivingScanService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
    }
}
