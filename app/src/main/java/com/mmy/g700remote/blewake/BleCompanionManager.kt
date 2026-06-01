package com.mmy.g700remote.blewake

import android.app.Activity
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.mmy.g700remote.ble.DisplayMirrorBleClient
import com.mmy.g700remote.data.SecureSettingsStore

object BleCompanionManager {
    private const val TAG = "G700BleCompanion"

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    fun requestAssociation(
        activity: Activity,
        onLaunch: (IntentSender) -> Unit,
        onAssociated: (Int) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!isSupported()) {
            onError("Companion Device setup is not supported on this Android version.")
            return
        }
        val manager = activity.getSystemService(CompanionDeviceManager::class.java)
        val request = AssociationRequest.Builder()
            .addDeviceFilter(
                BluetoothLeDeviceFilter.Builder()
                    .setScanFilter(BleWakeManager.buildTargetScanFilters(SecureSettingsStore(activity).getPairedDevice() ?: run {
                        onError("Pair DisplayMirror before setting up companion wake.")
                        return
                    }).first())
                    .build(),
            )
            .setSingleDevice(true)
            .setDisplayName("G700 DisplayMirror")
            .build()
        manager.associate(
            request,
            activity.mainExecutor,
            object : CompanionDeviceManager.Callback() {
                override fun onAssociationPending(intentSender: IntentSender) {
                    onLaunch(intentSender)
                }

                override fun onDeviceFound(intentSender: IntentSender) {
                    onLaunch(intentSender)
                }

                override fun onAssociationCreated(associationInfo: AssociationInfo) {
                    onAssociated(associationInfo.id)
                }

                override fun onFailure(error: CharSequence?) {
                    onError(error?.toString() ?: "Companion association failed")
                }
            },
        )
    }

    fun extractAssociationId(intent: Intent?): Int? {
        if (intent == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return intent.getParcelableExtra(
            CompanionDeviceManager.EXTRA_ASSOCIATION,
            AssociationInfo::class.java,
        )?.id
    }

    fun startObservingIfAssociated(context: Context) {
        if (!isSupported()) return
        val appContext = context.applicationContext
        val settings = SecureSettingsStore(appContext)
        val manager = appContext.getSystemService(CompanionDeviceManager::class.java)
        val id = settings.getCompanionAssociationId()
        val address = settings.getPairedDevice()?.address
        runCatching {
            when {
                Build.VERSION.SDK_INT >= 35 && id != null -> {
                    val request = android.companion.ObservingDevicePresenceRequest.Builder()
                        .setAssociationId(id)
                        .build()
                    manager.startObservingDevicePresence(request)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && BleWakePolicy.isBleAddress(address) -> {
                    @Suppress("DEPRECATION")
                    manager.startObservingDevicePresence(address!!)
                }
                else -> Unit
            }
        }.onFailure {
            Log.w(TAG, "Could not start companion presence observation", it)
        }
    }

    fun stopObservingIfAssociated(context: Context) {
        if (!isSupported()) return
        val appContext = context.applicationContext
        val settings = SecureSettingsStore(appContext)
        val manager = appContext.getSystemService(CompanionDeviceManager::class.java)
        val id = settings.getCompanionAssociationId()
        val address = settings.getPairedDevice()?.address
        runCatching {
            when {
                Build.VERSION.SDK_INT >= 35 && id != null -> {
                    val request = android.companion.ObservingDevicePresenceRequest.Builder()
                        .setAssociationId(id)
                        .build()
                    manager.stopObservingDevicePresence(request)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && BleWakePolicy.isBleAddress(address) -> {
                    @Suppress("DEPRECATION")
                    manager.stopObservingDevicePresence(address!!)
                }
                else -> Unit
            }
        }.onFailure {
            Log.w(TAG, "Could not stop companion presence observation", it)
        }
    }

    fun displayMirrorPresenceUuid(): ParcelUuid = ParcelUuid(DisplayMirrorBleClient.SERVICE_UUID)
}
