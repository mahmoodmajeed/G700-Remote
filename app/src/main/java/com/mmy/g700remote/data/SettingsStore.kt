package com.mmy.g700remote.data

import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.cloud.BoundCar
import com.mmy.g700remote.cloud.CloudAccount

interface SettingsStore {
    fun getPairedDevice(): PairedDevice?
    fun savePairedDevice(device: PairedDevice)
    fun clearPairedDevice()
    fun getCloudAccount(): CloudAccount?
    fun saveCloudAccount(account: CloudAccount)
    fun clearCloudAccount()
    fun getBoundCar(): BoundCar?
    fun saveBoundCar(car: BoundCar)
    fun clearBoundCar()
    fun isCloudEnabled(): Boolean
    fun setCloudEnabled(enabled: Boolean)
    fun getPairingCode(): String
    fun setPairingCode(code: String)
    fun isBleEnabled(): Boolean
    fun setBleEnabled(enabled: Boolean)
    fun isLanEnabled(): Boolean
    fun setLanEnabled(enabled: Boolean)
    fun getConnectionPreference(): ConnectionPreference
    fun setConnectionPreference(preference: ConnectionPreference)
    fun getAppLanguage(): AppLanguage
    fun setAppLanguage(language: AppLanguage)
    fun getAppTheme(): AppTheme
    fun setAppTheme(theme: AppTheme)
    fun getAppColorMode(): AppColorMode
    fun setAppColorMode(mode: AppColorMode)
    fun getAppIconTheme(): AppIconTheme
    fun setAppIconTheme(theme: AppIconTheme)
    fun isBleWakeEnabled(): Boolean
    fun setBleWakeEnabled(enabled: Boolean)
    fun getCompanionAssociationId(): Int?
    fun setCompanionAssociationId(id: Int?)
    fun getNavigationHistory(): List<NavigationHistoryEntry>
    fun saveNavigationHistory(history: List<NavigationHistoryEntry>)
    fun getLastVehicleStatus(): VehicleStatusSnapshot?
    fun saveLastVehicleStatus(snapshot: VehicleStatusSnapshot)
    fun getCarLocationPreference(): CarLocationPreference
    fun setCarLocationPreference(preference: CarLocationPreference)
    fun isConnectedNotificationEnabled(): Boolean
    fun setConnectedNotificationEnabled(enabled: Boolean)
    fun areRegionalFeaturesEnabled(): Boolean
    fun setRegionalFeaturesEnabled(enabled: Boolean)
    fun isLocalAuthEnabled(): Boolean
    fun setLocalAuthEnabled(enabled: Boolean)
    fun getLockStateMapping(): LockStateMapping
    fun setLockStateMapping(mapping: LockStateMapping)
    fun isLoggingEnabled(): Boolean
    fun setLoggingEnabled(enabled: Boolean)
    fun getLastSeenReleaseNotesVersion(): String?
    fun setLastSeenReleaseNotesVersion(version: String)
    /** Returns a stable UUID identifying this device installation; creates one on first call. */
    fun getOrCreateDeviceId(): String
}
