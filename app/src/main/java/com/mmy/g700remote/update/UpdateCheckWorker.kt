package com.mmy.g700remote.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mmy.g700remote.MainActivity

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val update = runCatching { AppUpdateManager.fetchLatestUpdate(applicationContext) }
            .getOrElse { return Result.retry() }
            ?: return Result.success()

        showUpdateNotification(update.versionName)
        return Result.success()
    }

    private fun showUpdateNotification(versionName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "App updates",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
            .setAction(MainActivity.ACTION_SHOW_UPDATES)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            73,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(applicationContext, CHANNEL_ID)
        } else {
            android.app.Notification.Builder(applicationContext)
        }
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("G700 Remote update available")
            .setContentText("Version $versionName is ready to download")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "g700_remote_updates"
        private const val NOTIFICATION_ID = 7301
    }
}
