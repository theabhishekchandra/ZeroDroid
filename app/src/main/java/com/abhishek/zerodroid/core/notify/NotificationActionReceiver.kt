package com.abhishek.zerodroid.core.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.abhishek.zerodroid.core.prefs.AppSettings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Notification buttons that act without opening the app. */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var settings: AppSettings

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MINE -> {
                intent.getStringExtra(EXTRA_ADDRESS)?.let { settings.markMine(it) }
                NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
            }
        }
    }

    companion object {
        const val ACTION_MINE = "com.abhishek.zerodroid.action.MINE"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
