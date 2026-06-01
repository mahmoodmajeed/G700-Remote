package com.mmy.g700remote.blewake

import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.util.Log

class G700CompanionDeviceService : CompanionDeviceService() {
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        Log.i(TAG, "Companion device appeared")
        BleWakeCoordinator.startActiveModeFromBleWake(
            this,
            source = "companion_presence",
            companionExempt = true,
        )
    }

    @Deprecated("String callbacks are kept for Android 12-14 compatibility")
    override fun onDeviceAppeared(address: String) {
        Log.i(TAG, "Companion device appeared")
        BleWakeCoordinator.startActiveModeFromBleWake(
            this,
            source = "companion_presence_legacy",
            companionExempt = true,
        )
    }

    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        BleWakeManager.registerBleWakeScan(this)
    }

    @Deprecated("String callbacks are kept for Android 12-14 compatibility")
    override fun onDeviceDisappeared(address: String) {
        BleWakeManager.registerBleWakeScan(this)
    }

    companion object {
        private const val TAG = "G700CompanionService"
    }
}
