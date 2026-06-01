package com.mmy.g700remote

import android.app.Application
import android.app.Activity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.blewake.BleCompanionManager
import com.mmy.g700remote.blewake.BleWakeManager
import com.mmy.g700remote.data.LockStateMapping
import com.mmy.g700remote.data.AppLanguage
import com.mmy.g700remote.data.AppColorMode
import com.mmy.g700remote.data.AppIconTheme
import com.mmy.g700remote.data.AppTheme
import com.mmy.g700remote.data.RemoteUiState
import com.mmy.g700remote.data.AppUpdateInfo
import com.mmy.g700remote.data.AppUpdateState
import com.mmy.g700remote.data.NavigationShareResult
import com.mmy.g700remote.protocol.RemoteCommand
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class G700RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val updateManager = G700RemoteAppGraph.updateManager(application.applicationContext)
    private val repository = G700RemoteAppGraph.repository(application.applicationContext)

    val uiState: StateFlow<RemoteUiState> = repository.uiState
    val updateState: StateFlow<AppUpdateState> = updateManager.state

    init {
        LauncherIconManager.applyIcon(application.applicationContext, uiState.value.appIconTheme)
        if (uiState.value.bleWakeEnabled) {
            BleWakeManager.registerBleWakeScan(application.applicationContext)
        }
        viewModelScope.launch {
            updateManager.checkIfDue()
        }
        if (uiState.value.pairedDevice != null) {
            repository.connectSaved()
        }
    }

    fun startScan() = repository.startScan()
    fun stopScan() = repository.stopScan()
    fun pairAndConnect(device: com.mmy.g700remote.ble.ScannedDevice) {
        repository.pairAndConnect(device)
        if (uiState.value.bleWakeEnabled) {
            BleWakeManager.registerBleWakeScan(getApplication<Application>().applicationContext)
        }
    }
    fun connectSaved() = repository.connectSaved()
    fun disconnect() = repository.disconnect()
    fun clearPairing() {
        BleWakeManager.unregisterBleWakeScan(getApplication<Application>().applicationContext)
        repository.clearPairing()
    }
    fun send(command: RemoteCommand) = repository.send(command)
    fun setPairingCode(code: String) = repository.setPairingCode(code)
    fun setBleEnabled(enabled: Boolean) = repository.setBleEnabled(enabled)
    fun setLanEnabled(enabled: Boolean) = repository.setLanEnabled(enabled)
    fun setConnectionPreference(preference: ConnectionPreference) = repository.setConnectionPreference(preference)
    fun setAppLanguage(language: AppLanguage) = repository.setAppLanguage(language)
    fun setAppTheme(theme: AppTheme) = repository.setAppTheme(theme)
    fun setAppColorMode(mode: AppColorMode) = repository.setAppColorMode(mode)
    fun setAppIconTheme(theme: AppIconTheme) {
        repository.setAppIconTheme(theme)
        LauncherIconManager.applyIcon(getApplication<Application>().applicationContext, theme)
    }
    fun setBleWakeEnabled(enabled: Boolean) {
        repository.setBleWakeEnabled(enabled)
        val context = getApplication<Application>().applicationContext
        if (enabled) {
            BleWakeManager.registerBleWakeScan(context)
        } else {
            BleWakeManager.unregisterBleWakeScan(context)
        }
    }
    fun setCompanionAssociationId(id: Int?) {
        repository.setCompanionAssociationId(id)
        if (id != null) {
            BleCompanionManager.startObservingIfAssociated(getApplication<Application>().applicationContext)
        }
    }
    fun setRegionalFeaturesEnabled(enabled: Boolean) = repository.setRegionalFeaturesEnabled(enabled)
    fun setLocalAuthEnabled(enabled: Boolean) = repository.setLocalAuthEnabled(enabled)
    fun setLockStateMapping(mapping: LockStateMapping) = repository.setLockStateMapping(mapping)
    fun setLoggingEnabled(enabled: Boolean) = repository.setLoggingEnabled(enabled)
    fun setConnectedNotificationEnabled(enabled: Boolean) = repository.setConnectedNotificationEnabled(enabled)
    fun markReleaseNotesSeen(version: String) = repository.markReleaseNotesSeen(version)
    fun refreshNow() = repository.refreshNow()
    fun sendSharedNavigation(text: String, onResult: (NavigationShareResult) -> Unit = {}) =
        repository.sendSharedNavigation(text, onResult)
    fun resendNavigationHistory(id: Long, onResult: (NavigationShareResult) -> Unit = {}) =
        repository.resendNavigationHistory(id, onResult)
    fun deleteNavigationHistory(id: Long) = repository.deleteNavigationHistory(id)
    fun clearNavigationHistory() = repository.clearNavigationHistory()
    fun checkForUpdates() = viewModelScope.launch { updateManager.checkNow() }
    fun clearUpdateMessage() = updateManager.clearMessage()
    fun downloadAndInstallUpdate(activity: Activity, info: AppUpdateInfo) = viewModelScope.launch {
        updateManager.downloadAndInstall(activity, info)
    }
    fun onForeground() {
        repository.onForeground()
        if (uiState.value.bleWakeEnabled) {
            BleWakeManager.registerBleWakeScan(getApplication<Application>().applicationContext)
        }
    }
    fun onBackground() = repository.onBackground()
    fun exportLogText(): String = repository.exportLogText()

    override fun onCleared() {
        super.onCleared()
    }
}
