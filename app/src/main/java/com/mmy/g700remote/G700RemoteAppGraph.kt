package com.mmy.g700remote

import android.content.Context
import com.mmy.g700remote.ble.DisplayMirrorBleClient
import com.mmy.g700remote.cloud.CloudClient
import com.mmy.g700remote.cloud.CloudRelayClient
import com.mmy.g700remote.data.CompositeDisplayMirrorTransport
import com.mmy.g700remote.data.RemoteRepository
import com.mmy.g700remote.data.SecureSettingsStore
import com.mmy.g700remote.network.DisplayMirrorLanClient
import com.mmy.g700remote.update.AppUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object G700RemoteAppGraph {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var settingsStore: SecureSettingsStore? = null

    @Volatile
    private var remoteRepository: RemoteRepository? = null

    @Volatile
    private var appUpdateManager: AppUpdateManager? = null

    @Volatile
    private var cloudClientInstance: CloudClient? = null

    fun cloudClient(): CloudClient =
        cloudClientInstance ?: synchronized(this) {
            cloudClientInstance ?: CloudClient().also { cloudClientInstance = it }
        }

    fun settings(context: Context): SecureSettingsStore =
        settingsStore ?: synchronized(this) {
            settingsStore ?: SecureSettingsStore(context.applicationContext).also { settingsStore = it }
        }

    fun repository(context: Context): RemoteRepository =
        remoteRepository ?: synchronized(this) {
            remoteRepository ?: createRepository(context.applicationContext).also { remoteRepository = it }
        }

    fun updateManager(context: Context): AppUpdateManager =
        appUpdateManager ?: synchronized(this) {
            appUpdateManager ?: AppUpdateManager(context.applicationContext).also { appUpdateManager = it }
        }

    private fun createRepository(context: Context): RemoteRepository {
        val settings = settings(context)
        return RemoteRepository(
            context = context,
            transport = CompositeDisplayMirrorTransport(
                ble = DisplayMirrorBleClient(
                    context,
                    appScope,
                    pairingCodeProvider = { settings.getPairingCode() },
                ),
                lan = DisplayMirrorLanClient(
                    context,
                    appScope,
                    pairingCodeProvider = { settings.getPairingCode() },
                ),
                cloud = CloudRelayClient(
                    scope = appScope,
                    boundCarProvider = { settings.getBoundCar() },
                    pairingCodeProvider = {
                        settings.getBoundCar()?.pairingCode?.ifBlank { null } ?: settings.getPairingCode()
                    },
                ),
                settings = settings,
                scope = appScope,
            ),
            settings = settings,
            scope = appScope,
        )
    }
}
