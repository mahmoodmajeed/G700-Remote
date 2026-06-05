package com.mmy.g700remote.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mmy.g700remote.MainActivity
import com.mmy.g700remote.R
import com.mmy.g700remote.analytics.G700Analytics
import com.mmy.g700remote.update.AppUpdateManager

class G700FirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        G700Analytics.settingChanged("fcm_token_refreshed", "true")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val forceUpdate = message.data["forceupdate"]?.trim() == "1"
        if (forceUpdate) {
            AppUpdateManager.markForceUpdateCheckPending(this)
            G700Analytics.settingChanged("fcm_force_update", "received")
        }
        val title = message.notification?.title
            ?: message.data["title"]
            ?: if (forceUpdate) getString(R.string.app_name) else null
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: if (forceUpdate) "Tap to check for the latest G700 Remote update." else null
            ?: return
        showNotification(title, body, forceUpdate)
    }

    private fun showNotification(title: String, body: String, forceUpdate: Boolean) {
        createChannel()
        val openIntent = PendingIntent.getActivity(
            this,
            if (forceUpdate) 7006 else 7005,
            Intent(this, MainActivity::class.java).apply {
                if (forceUpdate) {
                    action = MainActivity.ACTION_FORCE_UPDATE_CHECK
                    putExtra(MainActivity.EXTRA_FORCE_UPDATE, "1")
                }
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_jetour_logomark)
            .setColor(getColor(R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        }.onFailure { G700Analytics.nonFatal(it, "fcm_notification") }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "G700 Remote app messages"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "g700_remote_messages"
    }
}
