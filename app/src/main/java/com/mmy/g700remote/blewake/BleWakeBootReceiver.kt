package com.mmy.g700remote.blewake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mmy.g700remote.data.SecureSettingsStore

class BleWakeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val settings = SecureSettingsStore(context.applicationContext)
        if (!settings.isBleWakeEnabled()) return
        BleWakeManager.registerBleWakeScan(context)
    }
}
