package com.mmy.g700remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.data.AppLanguage
import com.mmy.g700remote.data.LockCommandProgress
import com.mmy.g700remote.data.RemoteUiState
import com.mmy.g700remote.protocol.OnOffAction
import com.mmy.g700remote.protocol.RemoteCommand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConnectedCarNotificationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository by lazy { G700RemoteAppGraph.repository(applicationContext) }
    private val settings by lazy { G700RemoteAppGraph.settings(applicationContext) }
    private var observerJob: Job? = null
    private var wakeStarted = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START,
            ACTION_START_FROM_BLE_WAKE -> {
                wakeStarted = intent.action == ACTION_START_FROM_BLE_WAKE
                if (wakeStarted && repository.uiState.value.connectionState !is RemoteConnectionState.Ready) {
                    repository.connectSaved()
                }
            }
            ACTION_LOCK -> repository.send(RemoteCommand.Lock)
            ACTION_UNLOCK -> repository.send(RemoteCommand.Unlock)
            ACTION_HAZARDS_ON -> repository.send(RemoteCommand.Hazards(OnOffAction.On))
            ACTION_HAZARDS_OFF -> repository.send(RemoteCommand.Hazards(OnOffAction.Off))
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> Unit
        }

        val state = repository.uiState.value
        if (!shouldRun(state)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground(buildNotification(state))
        observeState()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeState() {
        if (observerJob?.isActive == true) return
        observerJob = serviceScope.launch {
            repository.uiState.collect { state ->
                if (shouldRun(state)) {
                    runCatching {
                        NotificationManagerCompat.from(this@ConnectedCarNotificationService)
                            .notify(NOTIFICATION_ID, buildNotification(state))
                    }
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun shouldRun(state: RemoteUiState): Boolean =
        state.connectedNotificationEnabled &&
            settings.isConnectedNotificationEnabled() &&
            (state.connectionState is RemoteConnectionState.Ready ||
                (wakeStarted && state.pairedDevice != null && state.connectionState !is RemoteConnectionState.Error))

    private fun startAsForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: RemoteUiState): Notification {
        val language = state.appLanguage
        val locked = state.telemetry.lockState == 1
        val lockAction = if (locked) ACTION_UNLOCK else ACTION_LOCK
        val lockLabel = when {
            state.pendingLockCommand == LockCommandProgress.Locking -> label(language, "Locking")
            state.pendingLockCommand == LockCommandProgress.Unlocking -> label(language, "Unlocking")
            locked -> label(language, "Unlock")
            else -> label(language, "Lock")
        }
        val hazardsOn = state.telemetry.hazardsOn == true
        val battery = state.telemetry.batterySoc?.let { "$it%" } ?: "--"
        val fuel = state.telemetry.fuelPercent?.let { "$it%" } ?: "--"
        val updated = state.lastStatusRefreshMillis?.let { formatNotificationTime(it, language) } ?: label(language, "No update yet")
        val content = "${label(language, "Battery")} $battery • ${label(language, "Fuel")} $fuel • ${label(language, "Updated")} $updated"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_jetour_logomark)
            .setContentTitle(
                if (state.connectionState is RemoteConnectionState.Ready) {
                    label(language, "G700 Remote connected")
                } else {
                    label(language, "G700 Remote connecting")
                },
            )
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(activityIntent())
            .setOngoing(true)
            .setAutoCancel(false)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(
                R.drawable.ic_jetour_logomark,
                lockLabel,
                serviceIntent(lockAction),
            )
            .addAction(
                R.drawable.ic_jetour_logomark,
                if (hazardsOn) label(language, "Hazards off") else label(language, "Hazards on"),
                serviceIntent(if (hazardsOn) ACTION_HAZARDS_OFF else ACTION_HAZARDS_ON),
            )
            .build()
        notification.flags = notification.flags or
            Notification.FLAG_ONGOING_EVENT or
            Notification.FLAG_NO_CLEAR or
            Notification.FLAG_FOREGROUND_SERVICE
        return notification
    }

    private fun activityIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun serviceIntent(action: String): PendingIntent =
        PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, ConnectedCarNotificationService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "G700 connected status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shown while G700 Remote is connected to DisplayMirror."
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun label(language: AppLanguage, key: String): String =
        if (language == AppLanguage.Arabic) {
            when (key) {
                "G700 Remote connected" -> "G700 Remote متصل"
                "Battery" -> "البطارية"
                "Fuel" -> "الوقود"
                "Updated" -> "آخر تحديث"
                "No update yet" -> "لا يوجد تحديث"
                "Lock" -> "قفل"
                "Unlock" -> "فتح"
                "Locking" -> "جار القفل"
                "Unlocking" -> "جار الفتح"
                "Hazards on" -> "تشغيل التحذير"
                "Hazards off" -> "إيقاف التحذير"
                else -> key
            }
        } else {
            key
        }

    private fun formatNotificationTime(value: Long, language: AppLanguage): String {
        val locale = if (language == AppLanguage.Arabic) Locale.forLanguageTag("ar-BH") else Locale.getDefault()
        return SimpleDateFormat("h:mm a", locale).format(Date(value))
    }

    companion object {
        private const val CHANNEL_ID = "g700_connected_status"
        private const val NOTIFICATION_ID = 7001
        private const val ACTION_START = "com.mmy.g700remote.action.CONNECTED_NOTIFICATION_START"
        private const val ACTION_START_FROM_BLE_WAKE = "com.mmy.g700remote.action.CONNECTED_NOTIFICATION_BLE_WAKE"
        private const val ACTION_STOP = "com.mmy.g700remote.action.CONNECTED_NOTIFICATION_STOP"
        private const val ACTION_LOCK = "com.mmy.g700remote.action.NOTIFICATION_LOCK"
        private const val ACTION_UNLOCK = "com.mmy.g700remote.action.NOTIFICATION_UNLOCK"
        private const val ACTION_HAZARDS_ON = "com.mmy.g700remote.action.NOTIFICATION_HAZARDS_ON"
        private const val ACTION_HAZARDS_OFF = "com.mmy.g700remote.action.NOTIFICATION_HAZARDS_OFF"

        fun startIntent(context: Context, source: String): Intent =
            Intent(context, ConnectedCarNotificationService::class.java)
                .setAction(ACTION_START_FROM_BLE_WAKE)
                .putExtra("source", source)

        fun setRunning(context: Context, shouldRun: Boolean) {
            val intent = Intent(context, ConnectedCarNotificationService::class.java)
                .setAction(if (shouldRun) ACTION_START else ACTION_STOP)
            if (shouldRun) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.stopService(intent)
            }
        }
    }
}
