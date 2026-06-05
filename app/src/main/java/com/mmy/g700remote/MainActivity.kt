package com.mmy.g700remote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.mmy.g700remote.ui.G700RemoteApp
import com.mmy.g700remote.update.AppUpdateManager

class MainActivity : ComponentActivity() {
    private val permissionsGranted = mutableStateOf(false)
    private val sharedNavigationText = mutableStateOf<String?>(null)
    private val showUpdates = mutableStateOf(false)
    private val forceUpdateCheckRequest = mutableStateOf(0)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionsGranted.value = hasRequiredPermissions()
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Background update notifications are optional; manual update checks still work if denied.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUpdateManager.scheduleBackgroundChecks(this)
        handleIncomingIntent(intent)
        consumePendingForceUpdateCheck()
        requestNotificationPermissionIfNeeded()
        permissionsGranted.value = hasRequiredPermissions()
        setContent {
            G700RemoteApp(
                activity = this,
                permissionsGranted = permissionsGranted.value,
                sharedNavigationText = sharedNavigationText.value,
                showUpdates = showUpdates.value,
                forceUpdateCheckRequest = forceUpdateCheckRequest.value,
                onSharedNavigationConsumed = { sharedNavigationText.value = null },
                onUpdatesShown = { showUpdates.value = false },
                onForceUpdateCheckConsumed = { forceUpdateCheckRequest.value = 0 },
                onRequestPermissions = { permissionLauncher.launch(requiredPermissions()) },
                onShareLog = { text ->
                    startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_SUBJECT, "G700 Remote protocol log")
                                .putExtra(Intent.EXTRA_TEXT, text),
                            "Share protocol log",
                        ),
                    )
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent.requestsForceUpdateCheck()) {
            AppUpdateManager.consumeForceUpdateCheckPending(this)
            requestForceUpdateCheck()
            return
        }
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                sharedNavigationText.value = text
            }
            Intent.ACTION_VIEW -> {
                val uri: Uri? = intent.data
                sharedNavigationText.value = uri?.toString()
            }
            ACTION_SHOW_UPDATES -> showUpdates.value = true
        }
    }

    private fun consumePendingForceUpdateCheck() {
        if (AppUpdateManager.consumeForceUpdateCheckPending(this)) {
            requestForceUpdateCheck()
        }
    }

    private fun requestForceUpdateCheck() {
        showUpdates.value = true
        forceUpdateCheckRequest.value += 1
    }

    private fun Intent?.requestsForceUpdateCheck(): Boolean {
        if (this == null) return false
        if (action == ACTION_FORCE_UPDATE_CHECK) return true
        return getStringExtra(EXTRA_FORCE_UPDATE)
            ?.trim()
            ?.equals("1", ignoreCase = true) == true
    }

    private fun hasRequiredPermissions(): Boolean =
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    companion object {
        const val ACTION_SHOW_UPDATES = "com.mmy.g700remote.action.SHOW_UPDATES"
        const val ACTION_FORCE_UPDATE_CHECK = "com.mmy.g700remote.action.FORCE_UPDATE_CHECK"
        const val EXTRA_FORCE_UPDATE = "forceupdate"
    }
}
