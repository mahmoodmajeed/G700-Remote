package com.mmy.g700remote.blewake

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mmy.g700remote.G700RemoteAppGraph
import com.mmy.g700remote.analytics.G700Analytics

class BleWakeWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = G700RemoteAppGraph.settings(applicationContext)
        if (!settings.isBleWakeEnabled() || settings.getPairedDevice() == null) return Result.success()
        runCatching {
            G700RemoteAppGraph.repository(applicationContext).refreshFromBleWake()
        }.onFailure { G700Analytics.nonFatal(it, "ble_wake_worker") }
        return Result.success()
    }
}
