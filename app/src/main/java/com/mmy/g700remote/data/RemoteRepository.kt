package com.mmy.g700remote.data

import android.content.Context
import android.net.Uri
import com.mmy.g700remote.analytics.G700Analytics
import com.mmy.g700remote.ble.DisplayMirrorTransport
import com.mmy.g700remote.cloud.BoundCar
import com.mmy.g700remote.cloud.CloudClient
import com.mmy.g700remote.cloud.CloudResult
import com.mmy.g700remote.cloud.QrPairingPayload
import com.mmy.g700remote.protocol.SceneKind
import com.mmy.g700remote.protocol.StartStopAction
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.ScannedDevice
import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.ble.TransportKind
import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.NavigationShareParser
import com.mmy.g700remote.protocol.RemoteProtocolCodec
import com.mmy.g700remote.protocol.RemoteResponse
import com.mmy.g700remote.protocol.ClimateAction
import com.mmy.g700remote.protocol.OnOffAction
import com.mmy.g700remote.protocol.ParkingChargeAction
import com.mmy.g700remote.protocol.RaceChargeAction
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class RemoteRepository private constructor(
    private val locationResolver: CarLocationProvider,
    private val transport: DisplayMirrorTransport,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
    private val cloudClient: CloudClient = CloudClient(),
) {
    constructor(
        context: Context,
        transport: DisplayMirrorTransport,
        settings: SettingsStore,
        scope: CoroutineScope,
    ) : this(
        locationResolver = CarLocationResolver(context.applicationContext),
        transport = transport,
        settings = settings,
        scope = scope,
    )

    constructor(
        transport: DisplayMirrorTransport,
        settings: SettingsStore,
        scope: CoroutineScope,
    ) : this(
        locationResolver = NoopCarLocationProvider,
        transport = transport,
        settings = settings,
        scope = scope,
    )
    private val storedStatus = settings.getLastVehicleStatus()
    private val _uiState = MutableStateFlow(
        RemoteUiState(
            pairedDevice = settings.getPairedDevice(),
            telemetry = storedStatus?.telemetry ?: VehicleTelemetry(),
            pairingCode = settings.getPairingCode(),
            bleEnabled = settings.isBleEnabled(),
            lanEnabled = settings.isLanEnabled(),
            connectionPreference = settings.getConnectionPreference(),
            appLanguage = settings.getAppLanguage(),
            appTheme = settings.getAppTheme(),
            appColorMode = settings.getAppColorMode(),
            appIconTheme = settings.getAppIconTheme(),
            bleWakeEnabled = settings.isBleWakeEnabled(),
            companionAssociationId = settings.getCompanionAssociationId(),
            regionalFeaturesEnabled = settings.areRegionalFeaturesEnabled(),
            localAuthEnabled = settings.isLocalAuthEnabled(),
            lockStateMapping = LockStateMapping.State1Locked,
            loggingEnabled = settings.isLoggingEnabled(),
            carLocation = storedStatus?.carLocation,
            carLocationPreference = settings.getCarLocationPreference(),
            navigationHistory = settings.getNavigationHistory(),
            connectedNotificationEnabled = settings.isConnectedNotificationEnabled(),
            lastStatusRefreshMillis = storedStatus?.lastRefreshMillis,
            lastSeenReleaseNotesVersion = settings.getLastSeenReleaseNotesVersion(),
            cloudEnabled = settings.isCloudEnabled(),
            accountEmail = settings.getCloudAccount()?.email,
            boundCar = settings.getBoundCar(),
        ),
    )
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var refreshJob: Job? = null
    private var backgroundRefreshJob: Job? = null
    private var backgroundDisconnectJob: Job? = null
    private var lastLocationRefreshMillis: Long = 0L

    init {
        scope.launch {
            transport.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        connectionState = state,
                        isScanning = state is RemoteConnectionState.Scanning,
                        lastError = (state as? RemoteConnectionState.Error)?.message ?: it.lastError,
                    )
                }
            }
        }
        scope.launch {
            transport.incoming.collect { response ->
                applyResponse(response)
                appendLog(ProtocolLogEntry.Direction.Rx, response.raw)
            }
        }
    }

    fun startScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(scanResults = emptyList(), isScanning = true, lastError = null) }
        scanJob = scope.launch {
            transport.scanForDevices().collect { device ->
                _uiState.update { state ->
                    val merged = (state.scanResults.filterNot {
                        it.address == device.address && it.transport == device.transport
                    } + device)
                        .sortedWith(
                            compareByDescending<ScannedDevice> { it.lastSeenMillis }
                                .thenByDescending { it.rssi },
                        )
                    state.copy(scanResults = merged)
                }
            }
        }
        scope.launch {
            delay(15_000)
            stopScan()
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.update { state ->
            if (state.connectionState is RemoteConnectionState.Scanning) {
                state.copy(isScanning = false, connectionState = RemoteConnectionState.Disconnected)
            } else {
                state.copy(isScanning = false)
            }
        }
    }

    fun pairAndConnect(device: ScannedDevice) {
        val paired = PairedDevice(device.name, device.address, device.transport)
        settings.savePairedDevice(paired)
        _uiState.update { it.copy(pairedDevice = paired) }
        stopScan()
        connectSaved()
    }

    fun connectSaved() {
        val paired = _uiState.value.pairedDevice ?: settings.getPairedDevice()
        if (paired == null) {
            _uiState.update { it.copy(lastError = "No DisplayMirror device is paired") }
            return
        }
        if (_uiState.value.connectionState is RemoteConnectionState.Ready) {
            refreshNow()
            return
        }
        if (_uiState.value.connectionState !is RemoteConnectionState.Disconnected &&
            _uiState.value.connectionState !is RemoteConnectionState.Error
        ) {
            return
        }
        scope.launch {
            appendLog(ProtocolLogEntry.Direction.Info, "Connecting to ${redact(paired.address)}")
            transport.connect(paired.address)
        }
    }

    fun disconnect() {
        scope.launch { transport.disconnect() }
    }

    fun clearPairing() {
        settings.clearPairedDevice()
        _uiState.update { it.copy(pairedDevice = null, scanResults = emptyList()) }
        disconnect()
    }

    // ---- Cloud account + binding ----

    suspend fun login(email: String, password: String): CloudResult<Unit> {
        _uiState.update { it.copy(cloudBusy = true, cloudNotice = null) }
        return when (val result = cloudClient.login(email, password)) {
            is CloudResult.Success -> {
                settings.saveCloudAccount(result.value)
                _uiState.update { it.copy(accountEmail = result.value.email, cloudBusy = false) }
                if (settings.getBoundCar() != null) syncSettingsFromCloud()
                CloudResult.Success(Unit)
            }
            is CloudResult.Failure -> {
                _uiState.update { it.copy(cloudBusy = false, cloudNotice = result.message) }
                result
            }
        }
    }

    suspend fun bindCarFromQr(rawQr: String): CloudResult<Unit> {
        val payload = QrPairingPayload.parse(rawQr)
            ?: return CloudResult.Failure("That QR code is not a DisplayMirror pairing code")
        _uiState.update { it.copy(cloudBusy = true, cloudNotice = null) }
        // An account is optional: with one we also bind to the cloud (remote control + sync) by
        // redeeming the QR pair token via adopt-car; without one the QR still gives us the
        // pairing code for local BLE/Wi-Fi control.
        val account = settings.getCloudAccount()
        var resolvedCode: String? = null
        var resolvedRelay: String? = null
        var adoptNotice: String? = null
        if (account != null && payload.pairToken.isNotBlank()) {
            when (val res = cloudClient.adoptCar(account, payload)) {
                is CloudResult.Success -> {
                    resolvedCode = res.value.pairingCode?.ifBlank { null }
                    resolvedRelay = res.value.relayUrl?.ifBlank { null }
                }
                is CloudResult.Failure -> {
                    adoptNotice = "Paired for local control. Cloud binding failed: ${res.message}"
                }
            }
        }
        val base = payload.toBoundCar()
        val car = base.copy(
            pairingCode = resolvedCode ?: base.pairingCode,
            relayBase = resolvedRelay ?: base.relayBase,
        )
        settings.saveBoundCar(car)
        if (car.pairingCode.isNotBlank()) settings.setPairingCode(car.pairingCode)
        val paired = PairedDevice(
            name = car.name ?: "G700",
            address = car.carId,
            transport = TransportKind.Cloud,
        )
        settings.savePairedDevice(paired)
        _uiState.update {
            it.copy(
                boundCar = car,
                pairingCode = settings.getPairingCode(),
                pairedDevice = paired,
                cloudBusy = false,
                cloudNotice = adoptNotice,
            )
        }
        if (account != null) syncSettingsFromCloud()
        connectSaved()
        return CloudResult.Success(Unit)
    }

    fun signOut() {
        val wasCloudPaired = _uiState.value.pairedDevice?.transport == TransportKind.Cloud
        settings.clearCloudAccount()
        settings.clearBoundCar()
        if (wasCloudPaired) settings.clearPairedDevice()
        _uiState.update {
            it.copy(
                accountEmail = null,
                boundCar = null,
                pairedDevice = if (wasCloudPaired) null else it.pairedDevice,
                camera = CameraUiState(),
            )
        }
        disconnect()
    }

    fun setCloudEnabled(enabled: Boolean) {
        settings.setCloudEnabled(enabled)
        _uiState.update { it.copy(cloudEnabled = enabled) }
    }

    fun clearCloudNotice() {
        _uiState.update { it.copy(cloudNotice = null) }
    }

    private fun syncSettingsFromCloud() {
        val account = settings.getCloudAccount() ?: return
        val car = settings.getBoundCar() ?: return
        scope.launch {
            when (val result = cloudClient.pullSettings(account, car.carId)) {
                is CloudResult.Success -> appendLog(ProtocolLogEntry.Direction.Info, "Cloud preferences synced")
                is CloudResult.Failure -> appendLog(
                    ProtocolLogEntry.Direction.Info,
                    "Cloud settings sync skipped: ${result.message}",
                )
            }
        }
    }

    // ---- Cameras / sentinel / scenes / audio / cabin cooling ----

    fun refreshCameras() = send(RemoteCommand.Cameras)

    fun selectCamera(id: String) {
        _uiState.update { it.copy(camera = it.camera.copy(selectedCameraId = id, snapshot = null, liveFrame = null)) }
    }

    fun captureSnapshot(cameraId: String) {
        _uiState.update { it.copy(camera = it.camera.copy(loadingSnapshot = true, lastCameraError = null)) }
        send(RemoteCommand.Snapshot(camera = cameraId, requestId = "snap-${System.currentTimeMillis()}"))
    }

    fun startLiveView(cameraId: String) = send(RemoteCommand.LiveView(StartStopAction.Start, cameraId))

    fun stopLiveView() {
        send(RemoteCommand.LiveView(StartStopAction.Stop))
        _uiState.update { it.copy(camera = it.camera.copy(liveViewActive = false, liveFrame = null)) }
    }

    fun setSentinelArmed(armed: Boolean) =
        send(RemoteCommand.Sentinel(if (armed) StartStopAction.Start else StartStopAction.Stop))

    fun clearSentinelAlerts() {
        _uiState.update { it.copy(camera = it.camera.copy(sentinelAlerts = emptyList())) }
    }

    fun startScene(scene: SceneKind) = send(RemoteCommand.Scene(scene))

    fun refreshAudio() = send(RemoteCommand.Audio)

    fun setAudio(action: com.mmy.g700remote.protocol.AudioSetAction, value: Any) =
        send(RemoteCommand.AudioSet(action, value))

    fun refreshCabinCooling() =
        send(RemoteCommand.CabinCooling(com.mmy.g700remote.protocol.CabinCoolingAction.Status))

    fun setCabinCoolingEnabled(enabled: Boolean) =
        send(
            RemoteCommand.CabinCooling(
                if (enabled) {
                    com.mmy.g700remote.protocol.CabinCoolingAction.Enable
                } else {
                    com.mmy.g700remote.protocol.CabinCoolingAction.Disable
                },
            ),
        )

    fun triggerCabinCoolingNow() =
        send(RemoteCommand.CabinCooling(com.mmy.g700remote.protocol.CabinCoolingAction.TriggerNow))

    fun setLocalAuthEnabled(enabled: Boolean) {
        settings.setLocalAuthEnabled(enabled)
        _uiState.update { it.copy(localAuthEnabled = enabled) }
    }

    fun setLockStateMapping(mapping: LockStateMapping) {
        settings.setLockStateMapping(LockStateMapping.State1Locked)
        _uiState.update { it.copy(lockStateMapping = LockStateMapping.State1Locked) }
    }

    fun setLoggingEnabled(enabled: Boolean) {
        settings.setLoggingEnabled(enabled)
        _uiState.update { state ->
            state.copy(
                loggingEnabled = enabled,
                protocolLog = if (enabled) state.protocolLog else emptyList(),
            )
        }
    }

    fun setConnectedNotificationEnabled(enabled: Boolean) {
        settings.setConnectedNotificationEnabled(enabled)
        _uiState.update { it.copy(connectedNotificationEnabled = enabled) }
        if (!enabled && _uiState.value.connectionState !is RemoteConnectionState.Ready) {
            backgroundRefreshJob?.cancel()
            backgroundRefreshJob = null
        }
    }

    fun setCarLocationPreference(preference: CarLocationPreference) {
        settings.setCarLocationPreference(preference)
        _uiState.update { it.copy(carLocationPreference = preference) }
    }

    fun markReleaseNotesSeen(version: String) {
        settings.setLastSeenReleaseNotesVersion(version)
        _uiState.update { it.copy(lastSeenReleaseNotesVersion = version) }
    }

    fun setPairingCode(code: String) {
        settings.setPairingCode(code)
        _uiState.update { it.copy(pairingCode = settings.getPairingCode()) }
        if (_uiState.value.connectionState is RemoteConnectionState.Error) {
            connectSaved()
        }
    }

    fun setBleEnabled(enabled: Boolean) {
        settings.setBleEnabled(enabled)
        _uiState.update { it.copy(bleEnabled = enabled) }
    }

    fun setLanEnabled(enabled: Boolean) {
        settings.setLanEnabled(enabled)
        _uiState.update { it.copy(lanEnabled = enabled) }
    }

    fun setConnectionPreference(preference: ConnectionPreference) {
        settings.setConnectionPreference(preference)
        _uiState.update { it.copy(connectionPreference = preference) }
    }

    fun setAppLanguage(language: AppLanguage) {
        settings.setAppLanguage(language)
        _uiState.update { it.copy(appLanguage = language) }
    }

    fun setAppTheme(theme: AppTheme) {
        settings.setAppTheme(theme)
        _uiState.update { it.copy(appTheme = theme) }
    }

    fun setAppColorMode(mode: AppColorMode) {
        settings.setAppColorMode(mode)
        _uiState.update { it.copy(appColorMode = mode) }
    }

    fun setAppIconTheme(theme: AppIconTheme) {
        settings.setAppIconTheme(theme)
        _uiState.update { it.copy(appIconTheme = theme) }
    }

    fun setBleWakeEnabled(enabled: Boolean) {
        settings.setBleWakeEnabled(enabled)
        _uiState.update { it.copy(bleWakeEnabled = enabled) }
    }

    fun setCompanionAssociationId(id: Int?) {
        settings.setCompanionAssociationId(id)
        _uiState.update { it.copy(companionAssociationId = id) }
    }

    fun setRegionalFeaturesEnabled(enabled: Boolean) {
        settings.setRegionalFeaturesEnabled(enabled)
        _uiState.update { it.copy(regionalFeaturesEnabled = enabled) }
    }

    fun onForeground() {
        backgroundDisconnectJob?.cancel()
        backgroundDisconnectJob = null
        backgroundRefreshJob?.cancel()
        backgroundRefreshJob = null
        if (_uiState.value.pairedDevice != null) {
            connectSaved()
        }
        startForegroundRefresh()
    }

    fun onBackground() {
        refreshJob?.cancel()
        refreshJob = null
        backgroundDisconnectJob?.cancel()
        backgroundDisconnectJob = null
        if (_uiState.value.connectedNotificationEnabled && _uiState.value.connectionState is RemoteConnectionState.Ready) {
            startBackgroundRefresh()
        } else {
            backgroundRefreshJob?.cancel()
            backgroundRefreshJob = null
            backgroundDisconnectJob = scope.launch {
                delay(60_000)
                transport.disconnect()
            }
        }
    }

    fun refreshNow() {
        scope.launch {
            refreshAll(forceLocation = true)
        }
    }

    suspend fun refreshFromBleWake(timeoutMillis: Long = 18_000L) {
        if (_uiState.value.pairedDevice == null) return
        connectSaved()
        runCatching {
            withTimeout(timeoutMillis.coerceAtLeast(4_000L)) {
                transport.connectionState.first { it is RemoteConnectionState.Ready }
            }
        }.onFailure { G700Analytics.nonFatal(it, "ble_wake_connect") }
        if (_uiState.value.connectionState is RemoteConnectionState.Ready) {
            refreshAll(forceLocation = true)
        }
    }

    fun sendSharedNavigation(
        text: String,
        onResult: (NavigationShareResult) -> Unit = {},
    ) {
        scope.launch {
            val expandedText = resolveSharedNavigationText(text)
            val command = NavigationShareParser.parse(expandedText) ?: NavigationShareParser.parse(text)
            if (command == null) {
                _uiState.update { it.copy(lastError = "No destination found in shared text") }
                onResult(
                    NavigationShareResult(
                        title = "Shared location",
                        detail = "No destination found in shared text",
                        sent = false,
                        saved = false,
                        errorMessage = "No destination found in shared text",
                    ),
                )
                return@launch
            }
            val entry = addNavigationHistory(text, expandedText, command)
            if (_uiState.value.connectionState !is RemoteConnectionState.Ready) {
                connectSaved()
                runCatching {
                    kotlinx.coroutines.withTimeout(8_000) {
                        transport.connectionState.first { it is RemoteConnectionState.Ready }
                    }
                }
            }
            val ready = _uiState.value.connectionState is RemoteConnectionState.Ready
            val sent = if (ready) sendImmediate(command) else false
            val error = if (sent) null else _uiState.value.lastError ?: "Not connected to DisplayMirror"
            onResult(
                NavigationShareResult(
                    title = entry.title,
                    detail = entry.detail,
                    sent = sent,
                    saved = true,
                    errorMessage = error,
                ),
            )
        }
    }

    fun resendNavigationHistory(
        id: Long,
        onResult: (NavigationShareResult) -> Unit = {},
    ) {
        scope.launch {
            val entry = _uiState.value.navigationHistory.firstOrNull { it.id == id }
            if (entry == null) {
                onResult(
                    NavigationShareResult(
                        title = "Shared location",
                        detail = "Destination is no longer in history",
                        sent = false,
                        saved = false,
                        errorMessage = "Destination is no longer in history",
                    ),
                )
                return@launch
            }
            val command = entry.toNavigateCommand()
            if (command == null) {
                onResult(
                    NavigationShareResult(
                        title = entry.title,
                        detail = entry.detail,
                        sent = false,
                        saved = true,
                        errorMessage = "No destination found in shared text",
                    ),
                )
                return@launch
            }
            if (_uiState.value.connectionState !is RemoteConnectionState.Ready) {
                connectSaved()
                runCatching {
                    kotlinx.coroutines.withTimeout(4_000) {
                        transport.connectionState.first { it is RemoteConnectionState.Ready }
                    }
                }
            }
            val ready = _uiState.value.connectionState is RemoteConnectionState.Ready
            val sent = if (ready) sendImmediate(command, showNavigationStatus = false) else false
            val error = if (sent) null else _uiState.value.lastError ?: "Not connected to DisplayMirror"
            onResult(
                NavigationShareResult(
                    title = entry.title,
                    detail = entry.detail,
                    sent = sent,
                    saved = true,
                    errorMessage = error,
                ),
            )
        }
    }

    fun deleteNavigationHistory(id: Long) {
        val updated = _uiState.value.navigationHistory.filterNot { it.id == id }
        settings.saveNavigationHistory(updated)
        _uiState.update { it.copy(navigationHistory = updated) }
    }

    fun clearNavigationHistory() {
        settings.saveNavigationHistory(emptyList())
        _uiState.update { it.copy(navigationHistory = emptyList()) }
    }

    fun send(command: RemoteCommand) {
        when (command) {
            RemoteCommand.Lock -> _uiState.update { it.copy(pendingLockCommand = LockCommandProgress.Locking) }
            RemoteCommand.Unlock -> _uiState.update { it.copy(pendingLockCommand = LockCommandProgress.Unlocking) }
            else -> Unit
        }
        scope.launch {
            sendImmediate(command)
        }
    }

    private fun startForegroundRefresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            delay(500)
            while (true) {
                if (_uiState.value.connectionState is RemoteConnectionState.Ready) {
                    refreshAll()
                }
                delay(3_000)
            }
        }
    }

    private fun startBackgroundRefresh() {
        if (backgroundRefreshJob?.isActive == true) return
        backgroundRefreshJob = scope.launch {
            while (true) {
                if (_uiState.value.connectionState is RemoteConnectionState.Ready) {
                    refreshAll()
                    delay(120_000)
                } else {
                    break
                }
            }
        }
    }

    private suspend fun refreshAll(forceLocation: Boolean = false) {
        sendForRefresh(RemoteCommand.Status)
        sendForRefresh(RemoteCommand.Climate(ClimateAction.Status))
        sendForRefresh(RemoteCommand.ParkingCharge(ParkingChargeAction.Status))
        sendForRefresh(RemoteCommand.RaceCharge(RaceChargeAction.Status))
        refreshLocationIfDue(forceLocation)
    }

    private suspend fun refreshLocationIfDue(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastLocationRefreshMillis < LOCATION_REFRESH_INTERVAL_MS) return
        lastLocationRefreshMillis = now
        if (
            _uiState.value.carLocationPreference == CarLocationPreference.PhoneWhenBle &&
            _uiState.value.connectionState is RemoteConnectionState.Ready &&
            (_uiState.value.connectionState as RemoteConnectionState.Ready).transport == TransportKind.Ble
        ) {
            val phoneLocation = locationResolver.phoneLocationWhenAllowed()
            if (phoneLocation != null) {
                updateCarLocation(phoneLocation)
                resolveCarLocationAddress(phoneLocation)
                return
            }
        }
        sendForRefresh(RemoteCommand.GetLocation)
    }

    private suspend fun sendImmediate(command: RemoteCommand, showNavigationStatus: Boolean = true): Boolean {
        var success = false
        runCatching {
            appendLog(ProtocolLogEntry.Direction.Tx, RemoteProtocolCodec.encodeCommand(command))
            val response = transport.send(command)
            val lockCommand = command == RemoteCommand.Lock || command == RemoteCommand.Unlock
            if (response is RemoteResponse.Error) {
                applyResponse(response, showNavigationStatus = showNavigationStatus)
            } else if (lockCommand) {
                success = true
                delay(2_000)
                sendForRefresh(RemoteCommand.Status)
            } else {
                applyResponse(response, showNavigationStatus = showNavigationStatus)
                success = true
                applyCommandEcho(command)
                if (command is RemoteCommand.Climate && command.action != ClimateAction.Status) {
                    delay(600)
                    sendForRefresh(RemoteCommand.Climate(ClimateAction.Status))
                }
            }
        }.onFailure { throwable ->
            val message = throwable.message ?: "Command failed"
            appendLog(ProtocolLogEntry.Direction.Info, "Command failed: $message")
            _uiState.update { it.copy(lastError = message) }
        }.also {
            if (command == RemoteCommand.Lock || command == RemoteCommand.Unlock) {
                _uiState.update { it.copy(pendingLockCommand = null) }
            }
        }
        return success
    }

    private suspend fun sendForRefresh(command: RemoteCommand) {
        runCatching {
            appendLog(ProtocolLogEntry.Direction.Tx, RemoteProtocolCodec.encodeCommand(command))
            applyResponse(transport.send(command, timeoutMs = 4_000))
        }.onFailure { throwable ->
            appendLog(ProtocolLogEntry.Direction.Info, "Refresh failed: ${throwable.message ?: "unknown"}")
        }
    }

    fun exportLogText(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return buildString {
            appendLine("G700 Remote protocol log")
            appendLine("Device: ${_uiState.value.pairedDevice?.let { redact(it.address) } ?: "not paired"}")
            appendLine()
            _uiState.value.protocolLog.forEach { entry ->
                append(formatter.format(Date(entry.timeMillis)))
                append(' ')
                append(entry.direction.name)
                append(' ')
                appendLine(redact(entry.text))
            }
        }
    }

    private fun applyResponse(response: RemoteResponse, showNavigationStatus: Boolean = true) {
        when (response) {
            is RemoteResponse.LockState -> updateTelemetry { telemetry ->
                telemetry.copy(
                    lockState = response.state ?: telemetry.lockState,
                    cabinTemp = response.cabinTemp ?: telemetry.cabinTemp,
                    outdoorTemp = response.outdoorTemp ?: telemetry.outdoorTemp,
                    coolantTemp = response.coolantTemp ?: telemetry.coolantTemp,
                    batterySoc = response.batterySoc ?: telemetry.batterySoc,
                    fuelPercent = response.fuelPercent ?: telemetry.fuelPercent,
                    acOn = response.acOn ?: telemetry.acOn,
                    chargingState = response.chargingState ?: telemetry.chargingState,
                    chargeRemainTime = response.chargeRemainTime ?: telemetry.chargeRemainTime,
                    packVoltage = response.packVoltage ?: telemetry.packVoltage,
                    packCurrent = response.packCurrent ?: telemetry.packCurrent,
                    packPower = response.packPower ?: telemetry.packPower,
                    chargeMode = response.chargeMode ?: telemetry.chargeMode,
                    parkingChargeTargetSoc = response.parkingChargeTargetSoc ?: telemetry.parkingChargeTargetSoc,
                    parkingChargeEtaMin = response.parkingChargeEtaMin ?: telemetry.parkingChargeEtaMin,
                    dischargeEtaMin = response.dischargeEtaMin ?: telemetry.dischargeEtaMin,
                    safetySocFloor = response.safetySocFloor ?: telemetry.safetySocFloor,
                    raceChargeActive = response.raceChargeActive ?: telemetry.raceChargeActive,
                    raceChargeTarget = response.raceChargeTarget ?: telemetry.raceChargeTarget,
                    raceChargeEtaMin = response.raceChargeEtaMin ?: telemetry.raceChargeEtaMin,
                )
            }

            is RemoteResponse.ClimateState -> updateTelemetry { telemetry ->
                telemetry.copy(
                    acOn = response.acOn ?: telemetry.acOn,
                    tempLeft = response.tempLeft ?: telemetry.tempLeft,
                    tempRight = response.tempRight ?: telemetry.tempRight,
                    fanSpeed = response.fanSpeed ?: telemetry.fanSpeed,
                    circulation = response.circulation ?: telemetry.circulation,
                    fastCool = response.fastCool ?: telemetry.fastCool,
                    fastHeat = response.fastHeat ?: telemetry.fastHeat,
                    autoDefrost = response.autoDefrost ?: telemetry.autoDefrost,
                    rearDefrost = response.rearDefrost ?: telemetry.rearDefrost,
                    cabinTemp = response.cabinTemp ?: telemetry.cabinTemp,
                    outdoorTemp = response.outdoorTemp ?: telemetry.outdoorTemp,
                    coolantTemp = response.coolantTemp ?: telemetry.coolantTemp,
                )
            }

            is RemoteResponse.ParkingChargeState -> updateTelemetry { telemetry ->
                telemetry.copy(
                    parkingChargeTargetSoc = response.target ?: telemetry.parkingChargeTargetSoc,
                    parkingChargeSwitchState = response.switchState ?: telemetry.parkingChargeSwitchState,
                    parkingChargeMode = response.mode ?: telemetry.parkingChargeMode,
                )
            }

            is RemoteResponse.Location -> {
                val lat = response.lat
                val lon = response.lon
                if (lat != null && lon != null) {
                    val location = CarLocation(
                        lat = lat,
                        lon = lon,
                        source = CarLocationSource.DisplayMirror,
                        updatedAtMillis = System.currentTimeMillis(),
                    )
                    updateCarLocation(location)
                    resolveCarLocationAddress(location)
                }
            }

            is RemoteResponse.NavigateResult -> _uiState.update {
                it.copy(
                    lastNavigationStatus = if (showNavigationStatus) {
                        response.app?.let { app -> "Navigation sent to $app" }
                            ?: response.status
                            ?: "Navigation sent"
                    } else {
                        it.lastNavigationStatus
                    },
                    lastError = null,
                )
            }

            is RemoteResponse.Error -> _uiState.update {
                if (response.error == "unknown_cmd" && response.message?.contains("hello", ignoreCase = true) == true) {
                    it
                } else {
                    it.copy(lastError = response.message ?: response.error ?: "Protocol error")
                }
            }

            is RemoteResponse.CameraList -> _uiState.update { state ->
                val selected = state.camera.selectedCameraId
                    ?.takeIf { it in response.cameras }
                    ?: response.cameras.firstOrNull()
                state.copy(
                    camera = state.camera.copy(
                        cameraIds = response.cameras,
                        selectedCameraId = selected,
                        lastCameraError = null,
                    ),
                )
            }

            is RemoteResponse.SnapshotPending -> _uiState.update {
                it.copy(camera = it.camera.copy(loadingSnapshot = true))
            }

            is RemoteResponse.Snapshot -> _uiState.update { state ->
                if (response.ok && response.dataBase64 != null) {
                    state.copy(
                        camera = state.camera.copy(
                            snapshot = CameraFrame(
                                dataBase64 = response.dataBase64,
                                width = response.width,
                                height = response.height,
                            ),
                            loadingSnapshot = false,
                            lastCameraError = null,
                        ),
                    )
                } else {
                    state.copy(
                        camera = state.camera.copy(
                            loadingSnapshot = false,
                            lastCameraError = response.error ?: "Snapshot failed",
                        ),
                    )
                }
            }

            is RemoteResponse.LiveViewState -> _uiState.update {
                it.copy(camera = it.camera.copy(liveViewActive = response.state == "started"))
            }

            is RemoteResponse.LiveFrame -> {
                val data = response.dataBase64 ?: return
                _uiState.update {
                    it.copy(
                        camera = it.camera.copy(
                            liveFrame = CameraFrame(
                                dataBase64 = data,
                                width = response.width,
                                height = response.height,
                                seq = response.seq,
                            ),
                        ),
                    )
                }
            }

            is RemoteResponse.SentinelState -> _uiState.update {
                it.copy(camera = it.camera.copy(sentinelArmed = response.state == "started"))
            }

            is RemoteResponse.SentinelAlert -> _uiState.update { state ->
                val alert = SentinelAlertUi(
                    event = response.event,
                    eventName = response.eventName,
                    time = response.time,
                    thumbBase64 = response.thumbBase64,
                )
                state.copy(
                    camera = state.camera.copy(
                        sentinelAlerts = (listOf(alert) + state.camera.sentinelAlerts).take(MAX_SENTINEL_ALERTS),
                    ),
                )
            }

            is RemoteResponse.AudioState -> _uiState.update {
                it.copy(
                    audio = AudioUi(
                        eqMode = response.eqMode,
                        balance = response.balance,
                        balanceMin = response.balanceMin,
                        balanceMax = response.balanceMax,
                        fade = response.fade,
                        fadeMin = response.fadeMin,
                        fadeMax = response.fadeMax,
                        surround = response.surround,
                        loudness = response.loudness,
                    ),
                )
            }

            is RemoteResponse.CabinCoolingStateResponse -> _uiState.update {
                it.copy(
                    cabinCooling = CabinCoolingUi(
                        enabled = response.enabled,
                        autonomous = response.autonomous,
                        state = response.state,
                        reason = response.reason,
                        targetTemp = response.targetTemp,
                        socFloor = response.socFloor,
                        scheduleEnabled = response.scheduleEnabled,
                        scheduleTime = response.scheduleTime,
                        scheduleLeadMinutes = response.scheduleLeadMinutes,
                    ),
                )
            }

            is RemoteResponse.SceneResult -> _uiState.update {
                it.copy(
                    lastSceneStatus = if (response.ok == false) {
                        "Scene ${response.scene ?: ""} was not accepted".trim()
                    } else {
                        "Scene ${response.scene ?: ""} started".trim()
                    },
                )
            }

            is RemoteResponse.HelloResult,
            is RemoteResponse.Unknown,
            -> Unit
        }
    }

    private fun appendLog(direction: ProtocolLogEntry.Direction, text: String) {
        if (!_uiState.value.loggingEnabled) return
        _uiState.update { state ->
            state.copy(
                protocolLog = (state.protocolLog + ProtocolLogEntry(System.currentTimeMillis(), direction, redact(text)))
                    .takeLast(250),
            )
        }
    }

    private fun redact(value: String): String =
        value
            .replace(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"), "xx:xx:xx:xx:xx:xx")
            .replace(Regex(""""pairingCode"\s*:\s*"\d{4,8}""""), """"pairingCode":"****"""")
            .replace(Regex(""""(data|thumb)"\s*:\s*"[^"]{32,}""""), """"$1":"<image>"""")

    private fun applyCommandEcho(command: RemoteCommand) {
        when (command) {
            RemoteCommand.Lock -> _uiState.update { state ->
                state.copy(telemetry = state.telemetry.copy(lockState = 1))
            }.also { persistSnapshot() }

            RemoteCommand.Unlock -> _uiState.update { state ->
                state.copy(telemetry = state.telemetry.copy(lockState = 2))
            }.also { persistSnapshot() }

            is RemoteCommand.Hazards -> _uiState.update { state ->
                state.copy(telemetry = state.telemetry.copy(hazardsOn = command.action == OnOffAction.On))
            }.also { persistSnapshot() }

            is RemoteCommand.Drl -> _uiState.update { state ->
                state.copy(telemetry = state.telemetry.copy(drlOn = command.action == OnOffAction.On))
            }.also { persistSnapshot() }

            is RemoteCommand.Climate -> {
                when (command.action) {
                    ClimateAction.AcOn -> _uiState.update { state ->
                        state.copy(telemetry = state.telemetry.copy(acOn = true))
                    }.also { persistSnapshot() }
                    ClimateAction.AcOff -> _uiState.update { state ->
                        state.copy(telemetry = state.telemetry.copy(acOn = false))
                    }.also { persistSnapshot() }
                    ClimateAction.HvacOn -> _uiState.update { state ->
                        state.copy(
                            telemetry = state.telemetry.copy(
                                fanSpeed = (state.telemetry.fanSpeed ?: 0).coerceAtLeast(3),
                            ),
                        )
                    }.also { persistSnapshot() }
                    ClimateAction.HvacOff -> _uiState.update { state ->
                        state.copy(telemetry = state.telemetry.copy(fanSpeed = 0, acOn = false))
                    }.also { persistSnapshot() }
                    ClimateAction.SetTempLeft -> command.numericValue?.toDouble()?.let { value ->
                        _uiState.update { state ->
                            state.copy(telemetry = state.telemetry.copy(tempLeft = value))
                        }
                        persistSnapshot()
                    }
                    ClimateAction.SetTempRight -> command.numericValue?.toDouble()?.let { value ->
                        _uiState.update { state ->
                            state.copy(telemetry = state.telemetry.copy(tempRight = value))
                        }
                        persistSnapshot()
                    }
                    ClimateAction.SetFanSpeed -> command.numericValue?.toInt()?.let { value ->
                        _uiState.update { state ->
                            state.copy(telemetry = state.telemetry.copy(fanSpeed = value.coerceIn(0, 10)))
                        }
                        persistSnapshot()
                    }
                    ClimateAction.FastCoolOn -> echoClimate { it.copy(fastCool = true) }
                    ClimateAction.FastCoolOff -> echoClimate { it.copy(fastCool = false) }
                    ClimateAction.FastHeatOn -> echoClimate { it.copy(fastHeat = true) }
                    ClimateAction.FastHeatOff -> echoClimate { it.copy(fastHeat = false) }
                    ClimateAction.AutoDefrostOn -> echoClimate { it.copy(autoDefrost = true) }
                    ClimateAction.AutoDefrostOff -> echoClimate { it.copy(autoDefrost = false) }
                    ClimateAction.RearDefrostOn -> echoClimate { it.copy(rearDefrost = true) }
                    ClimateAction.RearDefrostOff -> echoClimate { it.copy(rearDefrost = false) }
                    ClimateAction.SetSeatHeat -> {
                        _uiState.update { state ->
                            val position = command.position ?: return@update state
                            val level = command.level ?: return@update state
                            state.copy(
                                telemetry = state.telemetry.copy(
                                    seatHeatLevels = state.telemetry.seatHeatLevels + (position.wireValue to level),
                                ),
                            )
                        }
                        persistSnapshot()
                    }
                    ClimateAction.SetSeatVent -> {
                        _uiState.update { state ->
                            val position = command.position ?: return@update state
                            val level = command.level ?: return@update state
                            state.copy(
                                telemetry = state.telemetry.copy(
                                    seatVentLevels = state.telemetry.seatVentLevels + (position.wireValue to level),
                                ),
                            )
                        }
                        persistSnapshot()
                    }
                    else -> Unit
                }
            }

            else -> Unit
        }
    }

    private fun addNavigationHistory(
        originalText: String,
        expandedText: String,
        command: RemoteCommand.Navigate,
    ): NavigationHistoryEntry {
        val now = System.currentTimeMillis()
        val originalLink = NavigationShareParser.firstShareUri(originalText)
        val originalUrl = NavigationShareParser.firstUrl(originalText)
        val expandedUrl = NavigationShareParser.firstUrl(expandedText)
        val placeName = NavigationShareParser.googlePlaceName(expandedUrl)
            ?: NavigationShareParser.googlePlaceName(originalUrl)
        val coordinates = command.lat?.let { lat ->
            command.lon?.let { lon -> "%.6f, %.6f".format(Locale.US, lat, lon) }
        }
        val title = placeName
            ?.toDisplayPlaceTitle()
            ?: command.label?.toHistoryTitle(originalUrl)?.toDisplayPlaceTitle()
            ?: command.query?.toHistoryTitle(originalUrl)?.take(80)
            ?: coordinates
            ?: "Destination"
        val linkPreview = compactUrl(originalLink ?: originalUrl ?: expandedUrl)
        val detail = when {
            placeName != null && linkPreview != null -> linkPreview
            coordinates != null -> coordinates
            !command.query.isNullOrBlank() -> command.query.toReadableText().take(140)
            originalUrl != null -> compactUrl(originalUrl) ?: originalUrl.take(140)
            else -> originalText.take(140)
        }
        val preview = when {
            placeName != null && coordinates != null -> coordinates
            linkPreview != null && linkPreview != detail -> linkPreview
            else -> null
        }
        val entry = NavigationHistoryEntry(
            id = now,
            title = title,
            detail = detail,
            originalText = originalText,
            sentAtMillis = now,
            previewText = preview,
            originalLink = originalLink ?: expandedUrl,
            navLat = command.lat,
            navLon = command.lon,
            navLabel = command.label,
            navQuery = command.query,
        )
        val updated = (listOf(entry) + _uiState.value.navigationHistory.filterNot {
            it.originalText == originalText || it.detail == detail
        }).take(MAX_NAV_HISTORY)
        settings.saveNavigationHistory(updated)
        _uiState.update { it.copy(navigationHistory = updated) }
        return entry
    }

    private fun NavigationHistoryEntry.toNavigateCommand(): RemoteCommand.Navigate? {
        val storedCommand = when {
            navLat != null && navLon != null -> runCatching {
                RemoteCommand.Navigate(lat = navLat, lon = navLon, label = navLabel)
            }.getOrNull()
            !navQuery.isNullOrBlank() -> runCatching {
                RemoteCommand.Navigate(query = navQuery)
            }.getOrNull()
            else -> null
        }
        if (storedCommand != null) return storedCommand

        val candidates = listOfNotNull(
            previewText,
            detail,
            title.takeUnless { it.equals("Destination", ignoreCase = true) },
            originalLink,
            originalText,
        )
        val parsed = candidates.mapNotNull { NavigationShareParser.parse(it) }
        return parsed.firstOrNull { it.lat != null && it.lon != null }
            ?: parsed.firstOrNull { !it.query.isNullOrBlank() && !it.query.startsWith("http", ignoreCase = true) }
            ?: parsed.firstOrNull()
    }

    private fun String.toHistoryTitle(originalUrl: String?): String? {
        val cleaned = toReadableText().trim()
        if (cleaned.isBlank()) return null
        if (cleaned.startsWith("http", ignoreCase = true)) return null
        if (cleaned.startsWith("data=", ignoreCase = true)) return null
        if (originalUrl != null && cleaned == Uri.parse(originalUrl).lastPathSegment) return null
        if (!cleaned.any { it.isWhitespace() || it == ',' || it == '+' } && cleaned.length > 14) return null
        return cleaned
    }

    private fun String.toReadableText(): String =
        replace('+', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun String.toDisplayPlaceTitle(): String =
        replace(Regex("""^[23456789CFGHJMPQRVWX]{4,8}\+[23456789CFGHJMPQRVWX]{2,3},?\s*"""), "")
            .trim()
            .ifBlank { this }

    private fun compactUrl(url: String?): String? {
        val parsed = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val host = parsed.host?.removePrefix("www.") ?: return null
        val path = parsed.pathSegments
            .lastOrNull { it.isNotBlank() && it != "maps" && it != "place" && !it.startsWith("data=") }
            ?.takeIf { it.length <= 64 }
        return if (path != null) "$host/$path" else host
    }

    private suspend fun resolveSharedNavigationText(text: String): String =
        withContext(Dispatchers.IO) {
            val url = NavigationShareParser.firstUrl(text) ?: return@withContext text
            if (!NavigationShareParser.isGoogleMapsUrl(url)) return@withContext text
            runCatching {
                val resolved = followRedirects(url)
                if (resolved != url) "$text\n$resolved" else text
            }.getOrDefault(text)
        }

    private fun followRedirects(url: String): String {
        var current = url
        repeat(6) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 7_000
                readTimeout = 7_000
                setRequestProperty("User-Agent", "Mozilla/5.0 G700-Remote")
            }
            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: return current
                    current = URL(URL(current), location).toString()
                } else {
                    val finalUrl = connection.url.toString()
                    val htmlResolved = runCatching {
                        connection.inputStream.bufferedReader().use { it.readText().take(200_000) }
                    }.getOrNull()
                        ?.let(::extractGoogleMapsUrl)
                    return htmlResolved ?: finalUrl
                }
            } finally {
                connection.disconnect()
            }
        }
        return current
    }

    private fun extractGoogleMapsUrl(html: String): String? {
        val unescaped = html
            .replace("\\u003d", "=")
            .replace("\\u0026", "&")
            .replace("\\/", "/")
            .replace("&amp;", "&")
        return Regex("""https://(?:www\.)?google\.[^"'<>\\]+/maps/[^"'<>\\]+""", RegexOption.IGNORE_CASE)
            .find(unescaped)
            ?.value
            ?.trimEnd('.', ',', ';', ')', ']', '}')
    }

    private fun echoClimate(update: (VehicleTelemetry) -> VehicleTelemetry) {
        _uiState.update { state -> state.copy(telemetry = update(state.telemetry)) }
        persistSnapshot()
    }

    private fun updateTelemetry(update: (VehicleTelemetry) -> VehicleTelemetry) {
        val now = System.currentTimeMillis()
        _uiState.update { state ->
            state.copy(
                telemetry = update(state.telemetry),
                lastStatusRefreshMillis = now,
            )
        }
        persistSnapshot()
    }

    private fun updateCarLocation(location: CarLocation) {
        _uiState.update { it.copy(carLocation = location) }
        persistSnapshot()
    }

    private fun resolveCarLocationAddress(location: CarLocation) {
        scope.launch {
            val resolved = locationResolver.withResolvedAddress(location)
            if (resolved.address != location.address) {
                _uiState.update { state ->
                    if (state.carLocation?.updatedAtMillis == location.updatedAtMillis) {
                        state.copy(carLocation = resolved)
                    } else {
                        state
                    }
                }
                persistSnapshot()
            }
        }
    }

    private fun persistSnapshot() {
        val state = _uiState.value
        val timestamp = state.lastStatusRefreshMillis ?: return
        settings.saveLastVehicleStatus(
            VehicleStatusSnapshot(
                telemetry = state.telemetry,
                lastRefreshMillis = timestamp,
                carLocation = state.carLocation,
            ),
        )
    }

    private companion object {
        const val MAX_NAV_HISTORY = 50
        const val MAX_SENTINEL_ALERTS = 20
        const val LOCATION_REFRESH_INTERVAL_MS = 60_000L
    }
}
