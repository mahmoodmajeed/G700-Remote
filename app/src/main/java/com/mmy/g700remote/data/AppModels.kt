package com.mmy.g700remote.data

import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.ScannedDevice
import com.mmy.g700remote.ble.TransportKind
import com.mmy.g700remote.cloud.BoundCar

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

data class VehicleStatusSnapshot(
    val telemetry: VehicleTelemetry,
    val lastRefreshMillis: Long,
    val carLocation: CarLocation? = null,
)

data class CarLocation(
    val lat: Double,
    val lon: Double,
    val address: String? = null,
    val source: CarLocationSource = CarLocationSource.DisplayMirror,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

enum class CarLocationSource {
    DisplayMirror,
    PhoneBle,
}

enum class CarLocationPreference {
    DisplayMirror,
    PhoneWhenBle,
}

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

enum class AppIconTheme {
    GtBlack,
    GtHorizon,
    GtDune,
    DisplayMirror,
}

enum class LockCommandProgress {
    Locking,
    Unlocking,
}

data class NavigationHistoryEntry(
    val id: Long,
    val title: String,
    val detail: String,
    val originalText: String,
    val sentAtMillis: Long,
    val previewText: String? = null,
    val originalLink: String? = null,
    val navLat: Double? = null,
    val navLon: Double? = null,
    val navLabel: String? = null,
    val navQuery: String? = null,
)

/** A single camera image (snapshot or live frame), base64 JPEG plus dimensions. */
data class CameraFrame(
    val dataBase64: String,
    val width: Int? = null,
    val height: Int? = null,
    val seq: Int? = null,
    val capturedAtMillis: Long = System.currentTimeMillis(),
)

data class SentinelAlertUi(
    val event: Int?,
    val eventName: String?,
    val time: String?,
    val thumbBase64: String?,
    val receivedAtMillis: Long = System.currentTimeMillis(),
)

data class AudioUi(
    val eqMode: Int? = null,
    val balance: Int? = null,
    val balanceMin: Int? = null,
    val balanceMax: Int? = null,
    val fade: Int? = null,
    val fadeMin: Int? = null,
    val fadeMax: Int? = null,
    val surround: Boolean? = null,
    val loudness: Boolean? = null,
)

data class CabinCoolingUi(
    val enabled: Boolean? = null,
    val autonomous: Boolean? = null,
    val state: String? = null,
    val reason: String? = null,
    val targetTemp: Double? = null,
    val socFloor: Int? = null,
    val scheduleEnabled: Boolean? = null,
    val scheduleTime: String? = null,
    val scheduleLeadMinutes: Int? = null,
)

/** Aggregated camera UI state. */
data class CameraUiState(
    val cameraIds: List<String> = emptyList(),
    val selectedCameraId: String? = null,
    val snapshot: CameraFrame? = null,
    val liveFrame: CameraFrame? = null,
    val liveViewActive: Boolean = false,
    val loadingSnapshot: Boolean = false,
    val sentinelArmed: Boolean = false,
    val sentinelAlerts: List<SentinelAlertUi> = emptyList(),
    val lastCameraError: String? = null,
    /** Last known snapshot for every camera ID, keyed by camera ID. Survives disconnection. */
    val cachedSnapshots: Map<String, CameraFrame> = emptyMap(),
)

data class NavigationShareResult(
    val title: String,
    val detail: String,
    val sent: Boolean,
    val saved: Boolean,
    val errorMessage: String? = null,
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
    val appTheme: AppTheme = AppTheme.HimalayaSlate,
    val appColorMode: AppColorMode = AppColorMode.Dark,
    val appIconTheme: AppIconTheme = AppIconTheme.GtBlack,
    val bleWakeEnabled: Boolean = true,
    val companionAssociationId: Int? = null,
    val regionalFeaturesEnabled: Boolean = false,
    val localAuthEnabled: Boolean = true,
    val lockStateMapping: LockStateMapping = LockStateMapping.State1Locked,
    val loggingEnabled: Boolean = false,
    val protocolLog: List<ProtocolLogEntry> = emptyList(),
    val carLocation: CarLocation? = null,
    val carLocationPreference: CarLocationPreference = CarLocationPreference.DisplayMirror,
    val navigationHistory: List<NavigationHistoryEntry> = emptyList(),
    val demoMode: Boolean = false,
    val connectedNotificationEnabled: Boolean = true,
    val lastStatusRefreshMillis: Long? = null,
    val pendingLockCommand: LockCommandProgress? = null,
    val lastSeenReleaseNotesVersion: String? = null,
    val lastNavigationStatus: String? = null,
    val lastError: String? = null,
    // Cloud account + binding
    val cloudEnabled: Boolean = true,
    val accountEmail: String? = null,
    val boundCar: BoundCar? = null,
    val cloudBusy: Boolean = false,
    val cloudNotice: String? = null,
    // New v3 feature state
    val camera: CameraUiState = CameraUiState(),
    val audio: AudioUi? = null,
    val cabinCooling: CabinCoolingUi? = null,
    val lastSceneStatus: String? = null,
) {
    val isSignedIn: Boolean get() = accountEmail != null
}
