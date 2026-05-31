package com.mmy.g700remote.data

import com.mmy.g700remote.ble.DisplayMirrorTransport
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.ScannedDevice
import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.NavigationShareParser
import com.mmy.g700remote.protocol.RemoteProtocolCodec
import com.mmy.g700remote.protocol.RemoteResponse
import com.mmy.g700remote.protocol.ClimateAction
import com.mmy.g700remote.protocol.OnOffAction
import com.mmy.g700remote.protocol.ParkingChargeAction
import com.mmy.g700remote.protocol.RaceChargeAction
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RemoteRepository(
    private val transport: DisplayMirrorTransport,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(
        RemoteUiState(
            pairedDevice = settings.getPairedDevice(),
            pairingCode = settings.getPairingCode(),
            bleEnabled = settings.isBleEnabled(),
            lanEnabled = settings.isLanEnabled(),
            connectionPreference = settings.getConnectionPreference(),
            appLanguage = settings.getAppLanguage(),
            appTheme = settings.getAppTheme(),
            regionalFeaturesEnabled = settings.areRegionalFeaturesEnabled(),
            localAuthEnabled = settings.isLocalAuthEnabled(),
            lockStateMapping = LockStateMapping.State1Locked,
            loggingEnabled = settings.isLoggingEnabled(),
        ),
    )
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var refreshJob: Job? = null
    private var backgroundDisconnectJob: Job? = null

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

    fun setRegionalFeaturesEnabled(enabled: Boolean) {
        settings.setRegionalFeaturesEnabled(enabled)
        _uiState.update { it.copy(regionalFeaturesEnabled = enabled) }
    }

    fun onForeground() {
        backgroundDisconnectJob?.cancel()
        if (_uiState.value.pairedDevice != null) {
            connectSaved()
        }
        startForegroundRefresh()
    }

    fun onBackground() {
        refreshJob?.cancel()
        refreshJob = null
        backgroundDisconnectJob?.cancel()
        backgroundDisconnectJob = scope.launch {
            delay(60_000)
            transport.disconnect()
        }
    }

    fun refreshNow() {
        scope.launch {
            refreshAll()
        }
    }

    fun sendSharedNavigation(text: String) {
        val command = NavigationShareParser.parse(text)
        if (command == null) {
            _uiState.update { it.copy(lastError = "No destination found in shared text") }
            return
        }
        scope.launch {
            if (_uiState.value.connectionState !is RemoteConnectionState.Ready) {
                connectSaved()
                runCatching {
                    kotlinx.coroutines.withTimeout(8_000) {
                        transport.connectionState.first { it is RemoteConnectionState.Ready }
                    }
                }
            }
            sendImmediate(command)
        }
    }

    fun send(command: RemoteCommand) {
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

    private suspend fun refreshAll() {
        sendForRefresh(RemoteCommand.Status)
        sendForRefresh(RemoteCommand.Climate(ClimateAction.Status))
        sendForRefresh(RemoteCommand.ParkingCharge(ParkingChargeAction.Status))
        sendForRefresh(RemoteCommand.RaceCharge(RaceChargeAction.Status))
    }

    private suspend fun sendImmediate(command: RemoteCommand) {
        runCatching {
            appendLog(ProtocolLogEntry.Direction.Tx, RemoteProtocolCodec.encodeCommand(command))
            val response = transport.send(command)
            applyResponse(response)
            if (response !is RemoteResponse.Error) {
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
        }
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

    private fun applyResponse(response: RemoteResponse) {
        when (response) {
            is RemoteResponse.LockState -> _uiState.update {
                it.copy(
                    telemetry = it.telemetry.copy(
                        lockState = response.state ?: it.telemetry.lockState,
                        cabinTemp = response.cabinTemp ?: it.telemetry.cabinTemp,
                        outdoorTemp = response.outdoorTemp ?: it.telemetry.outdoorTemp,
                        coolantTemp = response.coolantTemp ?: it.telemetry.coolantTemp,
                        batterySoc = response.batterySoc ?: it.telemetry.batterySoc,
                        fuelPercent = response.fuelPercent ?: it.telemetry.fuelPercent,
                        acOn = response.acOn ?: it.telemetry.acOn,
                        chargingState = response.chargingState ?: it.telemetry.chargingState,
                        chargeRemainTime = response.chargeRemainTime ?: it.telemetry.chargeRemainTime,
                        packVoltage = response.packVoltage ?: it.telemetry.packVoltage,
                        packCurrent = response.packCurrent ?: it.telemetry.packCurrent,
                        packPower = response.packPower ?: it.telemetry.packPower,
                        chargeMode = response.chargeMode ?: it.telemetry.chargeMode,
                        parkingChargeTargetSoc = response.parkingChargeTargetSoc ?: it.telemetry.parkingChargeTargetSoc,
                        parkingChargeEtaMin = response.parkingChargeEtaMin ?: it.telemetry.parkingChargeEtaMin,
                        dischargeEtaMin = response.dischargeEtaMin ?: it.telemetry.dischargeEtaMin,
                        safetySocFloor = response.safetySocFloor ?: it.telemetry.safetySocFloor,
                        raceChargeActive = response.raceChargeActive ?: it.telemetry.raceChargeActive,
                        raceChargeTarget = response.raceChargeTarget ?: it.telemetry.raceChargeTarget,
                        raceChargeEtaMin = response.raceChargeEtaMin ?: it.telemetry.raceChargeEtaMin,
                    ),
                )
            }

            is RemoteResponse.ClimateState -> _uiState.update {
                it.copy(
                    telemetry = it.telemetry.copy(
                        acOn = response.acOn ?: it.telemetry.acOn,
                        tempLeft = response.tempLeft ?: it.telemetry.tempLeft,
                        tempRight = response.tempRight ?: it.telemetry.tempRight,
                        fanSpeed = response.fanSpeed ?: it.telemetry.fanSpeed,
                        circulation = response.circulation ?: it.telemetry.circulation,
                        fastCool = response.fastCool ?: it.telemetry.fastCool,
                        fastHeat = response.fastHeat ?: it.telemetry.fastHeat,
                        autoDefrost = response.autoDefrost ?: it.telemetry.autoDefrost,
                        rearDefrost = response.rearDefrost ?: it.telemetry.rearDefrost,
                        cabinTemp = response.cabinTemp ?: it.telemetry.cabinTemp,
                        outdoorTemp = response.outdoorTemp ?: it.telemetry.outdoorTemp,
                        coolantTemp = response.coolantTemp ?: it.telemetry.coolantTemp,
                    ),
                )
            }

            is RemoteResponse.ParkingChargeState -> _uiState.update {
                it.copy(
                    telemetry = it.telemetry.copy(
                        parkingChargeTargetSoc = response.target ?: it.telemetry.parkingChargeTargetSoc,
                        parkingChargeSwitchState = response.switchState ?: it.telemetry.parkingChargeSwitchState,
                        parkingChargeMode = response.mode ?: it.telemetry.parkingChargeMode,
                    ),
                )
            }

            is RemoteResponse.Location -> _uiState.update {
                if (response.lat != null && response.lon != null) {
                    it.copy(carLocation = CarLocation(response.lat, response.lon))
                } else {
                    it
                }
            }

            is RemoteResponse.NavigateResult -> _uiState.update {
                it.copy(
                    lastNavigationStatus = response.app?.let { app -> "Navigation sent to $app" }
                        ?: response.status
                        ?: "Navigation sent",
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

    private fun applyCommandEcho(command: RemoteCommand) {
        when (command) {
            RemoteCommand.Lock -> _uiState.update { state ->
                state.copy(telemetry = state.telemetry.copy(lockState = 1))
            }

            RemoteCommand.Unlock -> _uiState.update { state ->
                state.copy(telemetry = state.telemetry.copy(lockState = 0))
            }

            is RemoteCommand.Hazards -> _uiState.update { state ->
                state.copy(telemetry = state.telemetry.copy(hazardsOn = command.action == OnOffAction.On))
            }

            is RemoteCommand.Drl -> _uiState.update { state ->
                state.copy(telemetry = state.telemetry.copy(drlOn = command.action == OnOffAction.On))
            }

            is RemoteCommand.Climate -> {
                when (command.action) {
                    ClimateAction.AcOn -> _uiState.update { state ->
                        state.copy(telemetry = state.telemetry.copy(acOn = true))
                    }
                    ClimateAction.AcOff -> _uiState.update { state ->
                        state.copy(telemetry = state.telemetry.copy(acOn = false))
                    }
                    ClimateAction.SetTempLeft -> command.numericValue?.toDouble()?.let { value ->
                        _uiState.update { state ->
                            state.copy(telemetry = state.telemetry.copy(tempLeft = value))
                        }
                    }
                    ClimateAction.SetTempRight -> command.numericValue?.toDouble()?.let { value ->
                        _uiState.update { state ->
                            state.copy(telemetry = state.telemetry.copy(tempRight = value))
                        }
                    }
                    ClimateAction.SetFanSpeed -> command.numericValue?.toInt()?.let { value ->
                        _uiState.update { state ->
                            state.copy(telemetry = state.telemetry.copy(fanSpeed = value.coerceIn(0, 10)))
                        }
                    }
                    ClimateAction.FastCoolOn -> echoClimate { it.copy(fastCool = true) }
                    ClimateAction.FastCoolOff -> echoClimate { it.copy(fastCool = false) }
                    ClimateAction.FastHeatOn -> echoClimate { it.copy(fastHeat = true) }
                    ClimateAction.FastHeatOff -> echoClimate { it.copy(fastHeat = false) }
                    ClimateAction.AutoDefrostOn -> echoClimate { it.copy(autoDefrost = true) }
                    ClimateAction.AutoDefrostOff -> echoClimate { it.copy(autoDefrost = false) }
                    ClimateAction.RearDefrostOn -> echoClimate { it.copy(rearDefrost = true) }
                    ClimateAction.RearDefrostOff -> echoClimate { it.copy(rearDefrost = false) }
                    ClimateAction.SetSeatHeat -> _uiState.update { state ->
                        val position = command.position ?: return@update state
                        val level = command.level ?: return@update state
                        state.copy(
                            telemetry = state.telemetry.copy(
                                seatHeatLevels = state.telemetry.seatHeatLevels + (position.wireValue to level),
                            ),
                        )
                    }
                    ClimateAction.SetSeatVent -> _uiState.update { state ->
                        val position = command.position ?: return@update state
                        val level = command.level ?: return@update state
                        state.copy(
                            telemetry = state.telemetry.copy(
                                seatVentLevels = state.telemetry.seatVentLevels + (position.wireValue to level),
                            ),
                        )
                    }
                    else -> Unit
                }
            }

            else -> Unit
        }
    }

    private fun echoClimate(update: (VehicleTelemetry) -> VehicleTelemetry) {
        _uiState.update { state -> state.copy(telemetry = update(state.telemetry)) }
    }
}
