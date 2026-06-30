package com.mmy.g700remote.data

import com.mmy.g700remote.ble.FakeDisplayMirrorTransport
import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.ScannedDevice
import com.mmy.g700remote.protocol.RemoteCommand
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteRepositoryTest {
    @Test
    fun reflectsScanPairConnectHandshakeAndCommandState() = runTest {
        val settings = InMemorySettingsStore()
        val repository = RemoteRepository(FakeDisplayMirrorTransport(), settings, this)

        assertTrue(repository.uiState.value.connectionState is RemoteConnectionState.Disconnected)

        repository.startScan()
        advanceUntilIdle()
        assertTrue(repository.uiState.value.scanResults.isNotEmpty())

        val device = repository.uiState.value.scanResults.first()
        repository.pairAndConnect(device)
        advanceUntilIdle()

        assertEquals(device.address, repository.uiState.value.pairedDevice?.address)
        assertTrue(repository.uiState.value.connectionState is RemoteConnectionState.Ready)
        assertNotNull(repository.uiState.value.telemetry.batterySoc)
        assertNotNull(repository.uiState.value.lastStatusRefreshMillis)

        repository.send(RemoteCommand.Unlock)
        advanceUntilIdle()
        assertEquals(2, repository.uiState.value.telemetry.lockState)
        assertEquals(null, repository.uiState.value.pendingLockCommand)
        coroutineContext.cancelChildren()
    }

    @Test
    fun loadsLastVehicleSnapshotWhenOffline() = runTest {
        val snapshot = VehicleStatusSnapshot(
            telemetry = VehicleTelemetry(lockState = 1, batterySoc = 61, fuelPercent = 42),
            lastRefreshMillis = 123_456L,
        )
        val settings = InMemorySettingsStore(initialSnapshot = snapshot)
        val repository = RemoteRepository(FakeDisplayMirrorTransport(), settings, this)

        assertTrue(repository.uiState.value.connectionState is RemoteConnectionState.Disconnected)
        assertEquals(61, repository.uiState.value.telemetry.batterySoc)
        assertEquals(42, repository.uiState.value.telemetry.fuelPercent)
        assertEquals(123_456L, repository.uiState.value.lastStatusRefreshMillis)
        coroutineContext.cancelChildren()
    }

    @Test
    fun resendsNavigationHistoryFromStoredCommand() = runTest {
        val historyEntry = NavigationHistoryEntry(
            id = 42L,
            title = "Bahrain International Circuit",
            detail = "26.0325, 50.5106",
            originalText = "https://maps.app.goo.gl/example",
            sentAtMillis = 1_234L,
            originalLink = "https://maps.app.goo.gl/example",
            navLat = 26.0325,
            navLon = 50.5106,
            navLabel = "Bahrain International Circuit",
        )
        val settings = InMemorySettingsStore(initialHistory = listOf(historyEntry))
        val repository = RemoteRepository(FakeDisplayMirrorTransport(), settings, this)

        repository.startScan()
        advanceUntilIdle()
        repository.pairAndConnect(repository.uiState.value.scanResults.first())
        advanceUntilIdle()

        val entry = repository.uiState.value.navigationHistory.first()
        assertEquals(26.0325, entry.navLat ?: 0.0, 0.0001)
        assertEquals(50.5106, entry.navLon ?: 0.0, 0.0001)

        val resendResultDeferred = CompletableDeferred<NavigationShareResult>()
        repository.resendNavigationHistory(entry.id) {
            resendResultDeferred.complete(it)
        }
        val resendResult = withTimeout(5_000) { resendResultDeferred.await() }

        assertTrue(resendResult.sent)
        assertEquals(1, repository.uiState.value.navigationHistory.size)
        coroutineContext.cancelChildren()
    }

    private class InMemorySettingsStore(
        initialHistory: List<NavigationHistoryEntry> = emptyList(),
        initialSnapshot: VehicleStatusSnapshot? = null,
    ) : SettingsStore {
        private var paired: PairedDevice? = null
        private var auth = true
        private var mapping = LockStateMapping.Unknown
        private var pairingCode = "123456"
        private var bleEnabled = true
        private var lanEnabled = true
        private var preference = ConnectionPreference.BleFirst
        private var language = AppLanguage.English
        private var theme = AppTheme.HimalayaSlate
        private var colorMode = AppColorMode.Dark
        private var iconTheme = AppIconTheme.GtBlack
        private var bleWakeEnabled = false
        private var companionAssociationId: Int? = null
        private var history = initialHistory
        private var snapshot = initialSnapshot
        private var regionalFeatures = false
        private var notificationEnabled = true
        private var locationPreference = CarLocationPreference.DisplayMirror
        private var releaseNotesVersion: String? = null

        override fun getPairedDevice(): PairedDevice? = paired
        override fun savePairedDevice(device: PairedDevice) {
            paired = device
        }

        override fun clearPairedDevice() {
            paired = null
        }

        private var cloudAccount: com.mmy.g700remote.cloud.CloudAccount? = null
        private var boundCar: com.mmy.g700remote.cloud.BoundCar? = null
        private var cloudEnabled = true

        override fun getCloudAccount(): com.mmy.g700remote.cloud.CloudAccount? = cloudAccount
        override fun saveCloudAccount(account: com.mmy.g700remote.cloud.CloudAccount) {
            cloudAccount = account
        }

        override fun clearCloudAccount() {
            cloudAccount = null
        }

        override fun getBoundCar(): com.mmy.g700remote.cloud.BoundCar? = boundCar
        override fun saveBoundCar(car: com.mmy.g700remote.cloud.BoundCar) {
            boundCar = car
        }

        override fun clearBoundCar() {
            boundCar = null
        }

        override fun isCloudEnabled(): Boolean = cloudEnabled
        override fun setCloudEnabled(enabled: Boolean) {
            cloudEnabled = enabled
        }

        override fun getPairingCode(): String = pairingCode
        override fun setPairingCode(code: String) {
            pairingCode = code
        }

        override fun isBleEnabled(): Boolean = bleEnabled
        override fun setBleEnabled(enabled: Boolean) {
            bleEnabled = enabled
        }

        override fun isLanEnabled(): Boolean = lanEnabled
        override fun setLanEnabled(enabled: Boolean) {
            lanEnabled = enabled
        }

        override fun getConnectionPreference(): ConnectionPreference = preference
        override fun setConnectionPreference(preference: ConnectionPreference) {
            this.preference = preference
        }

        override fun getAppLanguage(): AppLanguage = language
        override fun setAppLanguage(language: AppLanguage) {
            this.language = language
        }

        override fun getAppTheme(): AppTheme = theme
        override fun setAppTheme(theme: AppTheme) {
            this.theme = theme
        }

        override fun getAppColorMode(): AppColorMode = colorMode

        override fun setAppColorMode(mode: AppColorMode) {
            colorMode = mode
        }

        override fun getAppIconTheme(): AppIconTheme = iconTheme

        override fun setAppIconTheme(theme: AppIconTheme) {
            iconTheme = theme
        }

        override fun isBleWakeEnabled(): Boolean = bleWakeEnabled

        override fun setBleWakeEnabled(enabled: Boolean) {
            bleWakeEnabled = enabled
        }

        override fun getCompanionAssociationId(): Int? = companionAssociationId

        override fun setCompanionAssociationId(id: Int?) {
            companionAssociationId = id
        }

        override fun getNavigationHistory(): List<NavigationHistoryEntry> = history

        override fun saveNavigationHistory(history: List<NavigationHistoryEntry>) {
            this.history = history
        }

        override fun getLastVehicleStatus(): VehicleStatusSnapshot? = snapshot

        override fun saveLastVehicleStatus(snapshot: VehicleStatusSnapshot) {
            this.snapshot = snapshot
        }

        override fun getCarLocationPreference(): CarLocationPreference = locationPreference

        override fun setCarLocationPreference(preference: CarLocationPreference) {
            locationPreference = preference
        }

        override fun isConnectedNotificationEnabled(): Boolean = notificationEnabled

        override fun setConnectedNotificationEnabled(enabled: Boolean) {
            notificationEnabled = enabled
        }

        override fun areRegionalFeaturesEnabled(): Boolean = regionalFeatures
        override fun setRegionalFeaturesEnabled(enabled: Boolean) {
            regionalFeatures = enabled
        }

        override fun isLocalAuthEnabled(): Boolean = auth
        override fun setLocalAuthEnabled(enabled: Boolean) {
            auth = enabled
        }

        override fun getLockStateMapping(): LockStateMapping = mapping
        override fun setLockStateMapping(mapping: LockStateMapping) {
            this.mapping = mapping
        }

        override fun isLoggingEnabled(): Boolean = false

        override fun setLoggingEnabled(enabled: Boolean) = Unit

        override fun getLastSeenReleaseNotesVersion(): String? = releaseNotesVersion

        override fun setLastSeenReleaseNotesVersion(version: String) {
            releaseNotesVersion = version
        }

        override fun getOrCreateDeviceId(): String = "test-device-id"
    }
}
