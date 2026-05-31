package com.mmy.g700remote.data

import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.ScannedDevice
import com.mmy.g700remote.ble.TransportKind

data class PairedDevice(
    val name: String?,
    val address: String,
    val transport: TransportKind = TransportKind.Ble,
)

enum class LockStateMapping {
    Unknown,
    State1Locked,
    State2Locked,
}

data class VehicleTelemetry(
    val lockState: Int? = null,
    val cabinTemp: Double? = null,
    val outdoorTemp: Double? = null,
    val coolantTemp: Double? = null,
    val batterySoc: Int? = null,
    val fuelPercent: Int? = null,
    val acOn: Boolean? = null,
    val chargingState: Int? = null,
    val chargeRemainTime: Int? = null,
    val packVoltage: Double? = null,
    val packCurrent: Double? = null,
    val packPower: Double? = null,
    val chargeMode: String? = null,
    val parkingChargeTargetSoc: Int? = null,
    val parkingChargeEtaMin: Int? = null,
    val dischargeEtaMin: Int? = null,
    val safetySocFloor: Int? = null,
    val raceChargeActive: Boolean? = null,
    val raceChargeTarget: Int? = null,
    val raceChargeEtaMin: Int? = null,
    val tempLeft: Double? = null,
    val tempRight: Double? = null,
    val fanSpeed: Int? = null,
    val circulation: Int? = null,
    val fastCool: Boolean? = null,
    val fastHeat: Boolean? = null,
    val autoDefrost: Boolean? = null,
    val rearDefrost: Boolean? = null,
    val parkingChargeSwitchState: Int? = null,
    val parkingChargeMode: Int? = null,
    val hazardsOn: Boolean? = null,
    val drlOn: Boolean? = null,
    val seatHeatLevels: Map<String, Int> = emptyMap(),
    val seatVentLevels: Map<String, Int> = emptyMap(),
)

data class CarLocation(
    val lat: Double,
    val lon: Double,
)

data class AppUpdateInfo(
    val versionName: String,
    val tagName: String,
    val apkUrl: String,
    val releaseUrl: String,
    val body: String?,
)

data class AppUpdateState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int? = null,
    val availableUpdate: AppUpdateInfo? = null,
    val lastCheckedMillis: Long? = null,
    val gateReason: UpdateGateReason = UpdateGateReason.None,
    val message: String? = null,
) {
    val isUseBlocked: Boolean
        get() = gateReason != UpdateGateReason.None
}

data class ProtocolLogEntry(
    val timeMillis: Long,
    val direction: Direction,
    val text: String,
) {
    enum class Direction {
        Tx,
        Rx,
        Info,
    }
}

enum class AppLanguage {
    English,
    Arabic,
}

enum class AppTheme {
    G700Horizon,
    HimalayaSlate,
    NomadStone,
    ModernPastel,
    Minimal,
}

enum class AppColorMode {
    Dark,
    Light,
}

data class NavigationHistoryEntry(
    val id: Long,
    val title: String,
    val detail: String,
    val originalText: String,
    val sentAtMillis: Long,
)

enum class UpdateGateReason {
    None,
    StaleCheck,
    UpdateAvailable,
}

data class RemoteUiState(
    val connectionState: RemoteConnectionState = RemoteConnectionState.Disconnected,
    val pairedDevice: PairedDevice? = null,
    val scanResults: List<ScannedDevice> = emptyList(),
    val isScanning: Boolean = false,
    val telemetry: VehicleTelemetry = VehicleTelemetry(),
    val pairingCode: String = "",
    val bleEnabled: Boolean = true,
    val lanEnabled: Boolean = true,
    val connectionPreference: ConnectionPreference = ConnectionPreference.BleFirst,
    val appLanguage: AppLanguage = AppLanguage.English,
    val appTheme: AppTheme = AppTheme.G700Horizon,
    val appColorMode: AppColorMode = AppColorMode.Dark,
    val regionalFeaturesEnabled: Boolean = false,
    val localAuthEnabled: Boolean = true,
    val lockStateMapping: LockStateMapping = LockStateMapping.State1Locked,
    val loggingEnabled: Boolean = false,
    val protocolLog: List<ProtocolLogEntry> = emptyList(),
    val carLocation: CarLocation? = null,
    val navigationHistory: List<NavigationHistoryEntry> = emptyList(),
    val demoMode: Boolean = false,
    val lastNavigationStatus: String? = null,
    val lastError: String? = null,
)
