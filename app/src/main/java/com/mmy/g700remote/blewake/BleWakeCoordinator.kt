package com.mmy.g700remote.blewake

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.mmy.g700remote.ConnectedCarNotificationService
import com.mmy.g700remote.G700RemoteAppGraph
import com.mmy.g700remote.MainActivity
import com.mmy.g700remote.R
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.data.SecureSettingsStore

object BleWakeCoordinator {
    private const val TAG = "G700BleWakeCoordinator"
    private const val CHANNEL_ID = "g700_ble_wake"
    private const val ACTIVATE_NOTIFICATION_ID = 7003
    private const val WAKE_WORK_NAME = "g700_ble_wake_reconnect"

    fun startActiveModeFromBleWake(context: Context, source: String, companionExempt: Boolean = false) {
        val appContext = context.applicationContext
        val settings = SecureSettingsStore(appContext)
        if (!settings.isBleWakeEnabled() || settings.getPairedDevice() == null) return
        if (!hasBluetoothPermissions(appContext) || !isBluetoothEnabled(appContext)) {
            showTapToActivateNotification(appContext, "Bluetooth is off or permission is missing")
            return
        }

        val repository = G700RemoteAppGraph.repository(appContext)
        if (repository.uiState.value.connectionState is RemoteConnectionState.Ready) {
            startForegroundControls(appContext, fromBackground = true, companionExempt = true, source = source)
            return
        }

        repository.connectSaved()
        val started = startForegroundControls(
            appContext,
            fromBackground = true,
            companionExempt = companionExempt || settings.getCompanionAssociationId() != null,
            source = source,
        )
        if (!started) {
            enqueueReconnectWork(appContext)
            showTapToActivateNotification(appContext, "G700 is nearby. Tap to activate controls.")
        }
    }

    internal fun startForegroundControls(
        context: Context,
        fromBackground: Boolean,
        companionExempt: Boolean,
        source: String,
    ): Boolean {
        val intent = ConnectedCarNotificationService.startIntent(context, source)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && fromBackground && !companionExempt) {
                throw ForegroundServiceStartNotAllowedException("No companion-device exemption")
            }
            ContextCompat.startForegroundService(context, intent)
            true
        }.getOrElse {
            Log.w(TAG, "Foreground controls not started from $source", it)
            false
        }
    }

    fun showTapToActivateNotification(context: Context, reason: String) {
        createChannel(context)
        val intent = PendingIntent.getActivity(
            context,
            7003,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_jetour_logomark)
            .setContentTitle("G700 nearby")
            .setContentText(reason)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(ACTIVATE_NOTIFICATION_ID, notification)
        }.onFailure {
            Log.w(TAG, "Could not show BLE wake notification", it)
        }
    }

    private fun enqueueReconnectWork(context: Context) {
        val request = OneTimeWorkRequestBuilder<BleWakeWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WAKE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "G700 proximity wake",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Prompts when your paired G700 DisplayMirror BLE device is nearby."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun hasBluetoothPermissions(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun isBluetoothEnabled(context: Context): Boolean =
        context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
}
