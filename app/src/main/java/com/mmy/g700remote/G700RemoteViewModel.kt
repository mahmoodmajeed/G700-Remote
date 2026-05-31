package com.mmy.g700remote

import android.app.Application
import android.app.Activity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.ble.DisplayMirrorBleClient
import com.mmy.g700remote.data.LockStateMapping
import com.mmy.g700remote.data.AppLanguage
import com.mmy.g700remote.data.AppColorMode
import com.mmy.g700remote.data.AppTheme
import com.mmy.g700remote.data.CompositeDisplayMirrorTransport
import com.mmy.g700remote.data.RemoteRepository
import com.mmy.g700remote.data.RemoteUiState
import com.mmy.g700remote.data.SecureSettingsStore
import com.mmy.g700remote.data.AppUpdateInfo
import com.mmy.g700remote.data.AppUpdateState
import com.mmy.g700remote.network.DisplayMirrorLanClient
import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.update.AppUpdateManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class G700RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SecureSettingsStore(application.applicationContext)
    private val updateManager = AppUpdateManager(application.applicationContext)
    private val repository = RemoteRepository(
        transport = CompositeDisplayMirrorTransport(
            ble = DisplayMirrorBleClient(
                application.applicationContext,
                viewModelScope,
                pairingCodeProvider = { settings.getPairingCode() },
            ),
            lan = DisplayMirrorLanClient(
                application.applicationContext,
                viewModelScope,
                pairingCodeProvider = { settings.getPairingCode() },
            ),
            settings = settings,
            scope = viewModelScope,
        ),
        settings = settings,
        scope = viewModelScope,
    )

    val uiState: StateFlow<RemoteUiState> = repository.uiState
    val updateState: StateFlow<AppUpdateState> = updateManager.state

    init {
        viewModelScope.launch {
            updateManager.checkIfDue()
        }
        if (uiState.value.pairedDevice != null) {
            repository.connectSaved()
        }
    }

    fun startScan() = repository.startScan()
    fun stopScan() = repository.stopScan()
    fun pairAndConnect(device: com.mmy.g700remote.ble.ScannedDevice) = repository.pairAndConnect(device)
    fun connectSaved() = repository.connectSaved()
    fun disconnect() = repository.disconnect()
    fun clearPairing() = repository.clearPairing()
    fun send(command: RemoteCommand) = repository.send(command)
    fun setPairingCode(code: String) = repository.setPairingCode(code)
    fun setBleEnabled(enabled: Boolean) = repository.setBleEnabled(enabled)
    fun setLanEnabled(enabled: Boolean) = repository.setLanEnabled(enabled)
    fun setConnectionPreference(preference: ConnectionPreference) = repository.setConnectionPreference(preference)
    fun setAppLanguage(language: AppLanguage) = repository.setAppLanguage(language)
    fun setAppTheme(theme: AppTheme) = repository.setAppTheme(theme)
    fun setAppColorMode(mode: AppColorMode) = repository.setAppColorMode(mode)
    fun setRegionalFeaturesEnabled(enabled: Boolean) = repository.setRegionalFeaturesEnabled(enabled)
    fun setLocalAuthEnabled(enabled: Boolean) = repository.setLocalAuthEnabled(enabled)
    fun setLockStateMapping(mapping: LockStateMapping) = repository.setLockStateMapping(mapping)
    fun setLoggingEnabled(enabled: Boolean) = repository.setLoggingEnabled(enabled)
    fun refreshNow() = repository.refreshNow()
    fun sendSharedNavigation(text: String) = repository.sendSharedNavigation(text)
    fun deleteNavigationHistory(id: Long) = repository.deleteNavigationHistory(id)
    fun clearNavigationHistory() = repository.clearNavigationHistory()
    fun checkForUpdates() = viewModelScope.launch { updateManager.checkNow() }
    fun clearUpdateMessage() = updateManager.clearMessage()
    fun downloadAndInstallUpdate(activity: Activity, info: AppUpdateInfo) = viewModelScope.launch {
        updateManager.downloadAndInstall(activity, info)
    }
    fun onForeground() = repository.onForeground()
    fun onBackground() = repository.onBackground()
    fun exportLogText(): String = repository.exportLogText()

    override fun onCleared() {
        viewModelScope.launch {
            repository.disconnect()
        }
        super.onCleared()
    }
}
