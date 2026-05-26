package com.mmy.g700remote.ble

import com.mmy.g700remote.protocol.ClimateAction
import com.mmy.g700remote.protocol.OnOffAction
import com.mmy.g700remote.protocol.ParkingChargeAction
import com.mmy.g700remote.protocol.RaceChargeAction
import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.RemoteResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

class FakeDisplayMirrorTransport : DisplayMirrorTransport {
    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    override val connectionState: StateFlow<RemoteConnectionState> = _connectionState

    private val _incoming = MutableSharedFlow<RemoteResponse>(extraBufferCapacity = 32)
    override val incoming: SharedFlow<RemoteResponse> = _incoming

    private var lockState = 1
    private var acOn = false
    private var soc = 56
    private var targetSoc = 50
    private var chargeMode = 0
    private var raceChargeActive = false
    private var raceChargeTarget = 80

    override fun scanForDevices(): Flow<ScannedDevice> = flow {
        _connectionState.value = RemoteConnectionState.Scanning
        emit(
            ScannedDevice(
                name = "DisplayMirror",
                address = "00:11:22:33:44:55",
                rssi = -48,
                lastSeenMillis = System.currentTimeMillis(),
                transport = TransportKind.Ble,
            ),
        )
    }

    override suspend fun connect(address: String) {
        _connectionState.value = RemoteConnectionState.Connecting(address)
        delay(150)
        _connectionState.value = RemoteConnectionState.DiscoveringServices
        delay(100)
        _connectionState.value = RemoteConnectionState.EnablingNotifications
        delay(100)
        _connectionState.value = RemoteConnectionState.Handshaking
        delay(100)
        _connectionState.value = RemoteConnectionState.Ready(TransportKind.Ble)
        _incoming.emit(status())
    }

    override suspend fun disconnect() {
        _connectionState.value = RemoteConnectionState.Disconnected
    }

    override suspend fun send(command: RemoteCommand, timeoutMs: Long): RemoteResponse {
        delay(80)
        val response = when (command) {
            is RemoteCommand.Hello -> RemoteResponse.HelloResult(true, 3, """{"type":"helloResult","success":true,"protocolVersion":3}""")
            RemoteCommand.Status, RemoteCommand.Ping -> status()
            RemoteCommand.Lock -> {
                lockState = 1
                status()
            }
            RemoteCommand.Unlock -> {
                lockState = 2
                status()
            }
            is RemoteCommand.Soc -> {
                targetSoc = command.value
                status()
            }
            is RemoteCommand.ParkingCharge -> {
                if (command.action != ParkingChargeAction.Status) {
                    chargeMode = when (command.action) {
                        ParkingChargeAction.Off -> 0
                        ParkingChargeAction.Quiet -> 1
                        ParkingChargeAction.Fast -> 2
                        ParkingChargeAction.Status -> chargeMode
                    }
                }
                RemoteResponse.ParkingChargeState(
                    target = targetSoc,
                    switchState = if (chargeMode == 0) 0 else 1,
                    mode = chargeMode,
                    raw = """{"type":"parkingChargeState","target":$targetSoc,"switchState":${if (chargeMode == 0) 0 else 1},"mode":$chargeMode}""",
                )
            }
            is RemoteCommand.RaceCharge -> {
                when (command.action) {
                    RaceChargeAction.Start -> {
                        raceChargeActive = true
                        raceChargeTarget = command.target ?: raceChargeTarget
                    }
                    RaceChargeAction.Stop -> raceChargeActive = false
                    RaceChargeAction.Status -> Unit
                }
                status()
            }
            is RemoteCommand.Climate -> {
                if (command.action == ClimateAction.AcOn) acOn = true
                if (command.action == ClimateAction.AcOff) acOn = false
                climate()
            }
            is RemoteCommand.Hazards,
            is RemoteCommand.Drl,
            is RemoteCommand.Sunroof,
            is RemoteCommand.Sunshade,
            is RemoteCommand.Window,
            -> status()
        }
        _incoming.emit(response)
        return response
    }

    private fun status(): RemoteResponse.LockState =
        RemoteResponse.LockState(
            state = lockState,
            cabinTemp = 23.0,
            outdoorTemp = 34.0,
            coolantTemp = 76.0,
            batterySoc = soc,
            fuelPercent = 68,
            acOn = acOn,
            chargingState = 0,
            chargeRemainTime = 0,
            packVoltage = 611.0,
            packCurrent = 0.0,
            packPower = 0.0,
            chargeMode = "idle",
            parkingChargeTargetSoc = targetSoc,
            parkingChargeEtaMin = null,
            dischargeEtaMin = null,
            safetySocFloor = null,
            raceChargeActive = raceChargeActive,
            raceChargeTarget = if (raceChargeActive) raceChargeTarget else null,
            raceChargeEtaMin = if (raceChargeActive) 18 else null,
            raw = if (raceChargeActive) {
                """{"type":"lockState","state":$lockState,"cabinTemp":23.0,"outdoorTemp":34.0,"coolantTemp":76.0,"batterySOC":$soc,"fuelPercent":68,"acOn":$acOn,"chargingState":0,"chargeRemainTime":0,"packVoltage":611.0,"packCurrent":0.0,"packPower":0.0,"chargeMode":"idle","parkingChargeTargetSOC":$targetSoc,"raceChargeActive":true,"raceChargeTarget":$raceChargeTarget,"raceChargeEtaMin":18}"""
            } else {
                """{"type":"lockState","state":$lockState,"cabinTemp":23.0,"outdoorTemp":34.0,"coolantTemp":76.0,"batterySOC":$soc,"fuelPercent":68,"acOn":$acOn,"chargingState":0,"chargeRemainTime":0,"packVoltage":611.0,"packCurrent":0.0,"packPower":0.0,"chargeMode":"idle","parkingChargeTargetSOC":$targetSoc,"raceChargeActive":false}"""
            },
        )

    private fun climate(): RemoteResponse.ClimateState =
        RemoteResponse.ClimateState(
            acOn = acOn,
            tempLeft = 22.0,
            tempRight = 22.0,
            fanSpeed = 3,
            circulation = 0,
            fastCool = false,
            fastHeat = false,
            autoDefrost = false,
            rearDefrost = false,
            cabinTemp = 23.0,
            outdoorTemp = 34.0,
            coolantTemp = 76.0,
            raw = """{"type":"climateState","acOn":$acOn,"tempLeft":22.0,"tempRight":22.0,"fanSpeed":3,"circulation":0,"fastCool":false,"fastHeat":false,"autoDefrost":false,"rearDefrost":false,"cabinTemp":23.0,"outdoorTemp":34.0,"coolantTemp":76.0}""",
        )
}
