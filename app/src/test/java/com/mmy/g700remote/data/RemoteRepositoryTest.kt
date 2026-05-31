package com.mmy.g700remote.data

import com.mmy.g700remote.ble.FakeDisplayMirrorTransport
import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.ScannedDevice
import com.mmy.g700remote.protocol.RemoteCommand
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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

        repository.send(RemoteCommand.Unlock)
        advanceUntilIdle()
        assertEquals(2, repository.uiState.value.telemetry.lockState)
        coroutineContext.cancelChildren()
    }

    private class InMemorySettingsStore : SettingsStore {
        private var paired: PairedDevice? = null
        private var auth = true
        private var mapping = LockStateMapping.Unknown
        private var pairingCode = "123456"
        private var bleEnabled = true
        private var lanEnabled = true
        private var preference = ConnectionPreference.BleFirst
        private var language = AppLanguage.English
        private var theme = AppTheme.G700Horizon
        private var colorMode = AppColorMode.Dark
        private var history = emptyList<NavigationHistoryEntry>()
        private var regionalFeatures = false

        override fun getPairedDevice(): PairedDevice? = paired
        override fun savePairedDevice(device: PairedDevice) {
            paired = device
        }

        override fun clearPairedDevice() {
            paired = null
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

        override fun getNavigationHistory(): List<NavigationHistoryEntry> = history

        override fun saveNavigationHistory(history: List<NavigationHistoryEntry>) {
            this.history = history
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
    }
}
