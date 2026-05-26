package com.mmy.g700remote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.mmy.g700remote.ui.G700RemoteApp

class MainActivity : ComponentActivity() {
    private val permissionsGranted = mutableStateOf(false)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionsGranted.value = hasRequiredPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsGranted.value = hasRequiredPermissions()
        setContent {
            G700RemoteApp(
                activity = this,
                permissionsGranted = permissionsGranted.value,
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

    private fun hasRequiredPermissions(): Boolean =
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
}
