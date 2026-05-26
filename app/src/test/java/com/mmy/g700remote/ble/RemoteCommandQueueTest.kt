package com.mmy.g700remote.ble

import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.RemoteProtocolCodec
import com.mmy.g700remote.protocol.RemoteResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCommandQueueTest {
    @Test
    fun sendsOneCommandAtATimeAndWaitsForMatchingResponse() = runTest {
        val responses = MutableSharedFlow<RemoteResponse>(extraBufferCapacity = 8)
        val writes = mutableListOf<String>()
        val queue = RemoteCommandQueue(
            writeFrame = { frame ->
                writes += String(frame).trim()
                if (writes.size == 1) {
                    launch {
                        responses.emit(
                            RemoteResponse.LockState(
                                state = 1,
                                cabinTemp = null,
                                outdoorTemp = null,
                                coolantTemp = null,
                                batterySoc = null,
                                fuelPercent = null,
                                acOn = null,
                                chargingState = null,
                                chargeRemainTime = null,
                                packVoltage = null,
                                packCurrent = null,
                                packPower = null,
                                chargeMode = null,
                                parkingChargeTargetSoc = null,
                                parkingChargeEtaMin = null,
                                dischargeEtaMin = null,
                                safetySocFloor = null,
                                raceChargeActive = null,
                                raceChargeTarget = null,
                                raceChargeEtaMin = null,
                                raw = """{"type":"lockState","state":1}""",
                            ),
                        )
                    }
                } else {
                    launch {
                        responses.emit(RemoteResponse.ParkingChargeState(55, 1, 2, """{"type":"parkingChargeState","target":55,"switchState":1,"mode":2}"""))
                    }
                }
            },
            responses = responses,
        )

        val first = async { queue.send(RemoteCommand.Lock) }
        val second = async { queue.send(RemoteCommand.ParkingCharge(com.mmy.g700remote.protocol.ParkingChargeAction.Fast)) }

        assertTrue(first.await() is RemoteResponse.LockState)
        assertTrue(second.await() is RemoteResponse.ParkingChargeState)
        assertEquals("lock", org.json.JSONObject(writes[0]).getString("cmd"))
        assertEquals("parking_charge", org.json.JSONObject(writes[1]).getString("cmd"))
        assertEquals(
            "fast",
            org.json.JSONObject(writes[1]).getString("action"),
        )
        assertEquals("""{"cmd":"lock"}""", RemoteProtocolCodec.encodeCommand(RemoteCommand.Lock))
    }
}
