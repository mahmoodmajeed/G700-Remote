package com.mmy.g700remote.blewake

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.mmy.g700remote.ble.DisplayMirrorBleClient
import com.mmy.g700remote.data.PairedDevice
import com.mmy.g700remote.data.SecureSettingsStore
import com.mmy.g700remote.ble.TransportKind

object BleWakeManager {
    const val ACTION_BLE_WAKE_SCAN = "com.mmy.g700remote.action.BLE_WAKE_SCAN"
    private const val TAG = "G700BleWake"
    private const val REQUEST_CODE = 7002

    fun registerBleWakeScan(context: Context): Boolean {
        val appContext = context.applicationContext
        val settings = SecureSettingsStore(appContext)
        if (!settings.isBleWakeEnabled()) return false
        val paired = settings.getPairedDevice() ?: return false
        if (paired.transport == TransportKind.Lan) return false
        if (!hasScanPermission(appContext)) return false
        val scanner = scanner(appContext) ?: return false
        if (bluetoothOff(appContext)) return false
        val result = runCatching {
            @SuppressLint("MissingPermission")
            scanner.startScan(
                buildTargetScanFilters(paired),
                scanSettings(),
                buildWakePendingIntent(appContext),
            )
        }.getOrElse {
            Log.w(TAG, "Could not register BLE wake scan", it)
            return false
        }
        val ok = result == 0 || result == ScanCallback.SCAN_FAILED_ALREADY_STARTED
        settings.setBleWakeScanRegistered(ok)
        if (ok) {
            BleCompanionManager.startObservingIfAssociated(appContext)
        }
        return ok
    }

    fun unregisterBleWakeScan(context: Context) {
        val appContext = context.applicationContext
        val scanner = scanner(appContext)
        runCatching {
            @SuppressLint("MissingPermission")
            scanner?.stopScan(buildWakePendingIntent(appContext))
        }.onFailure {
            Log.w(TAG, "Could not unregister BLE wake scan", it)
        }
        SecureSettingsStore(appContext).setBleWakeScanRegistered(false)
        BleCompanionManager.stopObservingIfAssociated(appContext)
    }

    fun isBleWakeScanRegistered(context: Context): Boolean =
        SecureSettingsStore(context.applicationContext).isBleWakeScanRegistered()

    fun buildTargetScanFilters(paired: PairedDevice): List<ScanFilter> {
        val serviceUuid = ParcelUuid(DisplayMirrorBleClient.SERVICE_UUID)
        val builder = ScanFilter.Builder().setServiceUuid(serviceUuid)
        val address = paired.address.takeIf(BleWakePolicy::isBleAddress)
        if (address != null) {
            // DisplayMirror currently advertises a stable BLE address in this integration.
            // If a future peripheral uses a random/private address, service UUID matching still
            // identifies DisplayMirror and receiver-side validation should avoid address-only logic.
            runCatching { builder.setDeviceAddress(address) }
        }
        return listOf(builder.build())
    }

    fun buildWakePendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, BleWakeReceiver::class.java)
                .setAction(ACTION_BLE_WAKE_SCAN)
                .setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag(),
        )

    fun matchesTarget(context: Context, result: ScanResult): Boolean {
        val settings = SecureSettingsStore(context.applicationContext)
        val paired = settings.getPairedDevice() ?: return false
        val serviceUuids = result.scanRecord?.serviceUuids
            ?.map { it.uuid.toString() }
            ?.toSet()
            .orEmpty()
        val address = runCatching { result.device?.address }.getOrNull()
        return BleWakePolicy.matchesTarget(
            advertisedServiceUuids = serviceUuids,
            resultAddress = address,
            targetAddress = paired.address,
            serviceUuid = DisplayMirrorBleClient.SERVICE_UUID.toString(),
        )
    }

    fun markWakeIfNotDebounced(context: Context): Boolean {
        val settings = SecureSettingsStore(context.applicationContext)
        val now = System.currentTimeMillis()
        if (BleWakePolicy.shouldDebounce(settings.getLastBleWakeMillis(), now)) return false
        settings.setLastBleWakeMillis(now)
        return true
    }

    private fun scanSettings(): ScanSettings =
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .build()

    private fun scanner(context: Context): BluetoothLeScanner? =
        context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner

    private fun bluetoothOff(context: Context): Boolean =
        context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled != true

    private fun hasScanPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun mutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
}
