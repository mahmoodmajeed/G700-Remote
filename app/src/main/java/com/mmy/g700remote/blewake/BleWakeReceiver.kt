package com.mmy.g700remote.blewake

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BleWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BleWakeManager.ACTION_BLE_WAKE_SCAN) return
        val pendingResult = goAsync()
        runCatching {
            val errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, NO_SCAN_ERROR)
            if (errorCode != NO_SCAN_ERROR) {
                Log.w(TAG, "BLE wake scan error: $errorCode")
                return@runCatching
            }
            val results = scanResults(intent)
            if (results.none { BleWakeManager.matchesTarget(context, it) }) return@runCatching
            if (!BleWakeManager.markWakeIfNotDebounced(context)) return@runCatching
            BleWakeCoordinator.startActiveModeFromBleWake(context, "pending_intent_scan")
        }.onFailure {
            Log.w(TAG, "BLE wake receiver failed", it)
        }
        pendingResult.finish()
    }

    private fun scanResults(intent: Intent): List<ScanResult> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(
                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
                ScanResult::class.java,
            ).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<ScanResult>(
                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
            ).orEmpty()
        }

    companion object {
        private const val TAG = "G700BleWakeReceiver"
        private const val NO_SCAN_ERROR = -1
    }
}
