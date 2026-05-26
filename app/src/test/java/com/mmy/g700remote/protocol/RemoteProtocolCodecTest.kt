package com.mmy.g700remote.protocol

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteProtocolCodecTest {
    @Test
    fun serializesCommandsWithExpectedFields() {
        val hello = JSONObject(RemoteProtocolCodec.encodeCommand(RemoteCommand.Hello("123456")))
        assertEquals("hello", hello.getString("cmd"))
        assertEquals(3, hello.getInt("protocolVersion"))
        assertEquals("123456", hello.getString("pairingCode"))

        val seat = JSONObject(
            RemoteProtocolCodec.encodeCommand(
                RemoteCommand.Climate(
                    action = ClimateAction.SetSeatHeat,
                    position = SeatPosition.RearRight,
                    level = 3,
                ),
            ),
        )
        assertEquals("climate", seat.getString("cmd"))
        assertEquals("set_seat_heat", seat.getString("action"))
        assertEquals("rr", seat.getString("position"))
        assertEquals(3, seat.getInt("level"))

        val soc = JSONObject(RemoteProtocolCodec.encodeCommand(RemoteCommand.Soc(70)))
        assertEquals("soc", soc.getString("cmd"))
        assertEquals(70, soc.getInt("value"))

        val race = JSONObject(RemoteProtocolCodec.encodeCommand(RemoteCommand.RaceCharge(RaceChargeAction.Start, 85)))
        assertEquals("race_charge", race.getString("cmd"))
        assertEquals("start", race.getString("action"))
        assertEquals(85, race.getInt("target"))
    }

    @Test
    fun parsesKnownResponses() {
        val lock = RemoteProtocolCodec.decodeResponse(
            """{"type":"lockState","state":1,"cabinTemp":22.5,"outdoorTemp":35.0,"coolantTemp":78.0,"batterySOC":61,"fuelPercent":44,"acOn":true,"chargingState":0,"chargeRemainTime":0,"packVoltage":611.2,"packCurrent":0.0,"packPower":0.0,"chargeMode":"idle","parkingChargeTargetSOC":50,"raceChargeActive":true,"raceChargeTarget":85,"raceChargeEtaMin":18}""",
        )
        assertTrue(lock is RemoteResponse.LockState)
        lock as RemoteResponse.LockState
        assertEquals(1, lock.state)
        assertEquals(61, lock.batterySoc)
        assertEquals(44, lock.fuelPercent)
        assertEquals(78.0, lock.coolantTemp ?: -1.0, 0.0)
        assertEquals(true, lock.acOn)
        assertEquals(true, lock.raceChargeActive)
        assertEquals(85, lock.raceChargeTarget)
        assertEquals(18, lock.raceChargeEtaMin)

        val climate = RemoteProtocolCodec.decodeResponse(
            """{"type":"climateState","acOn":false,"tempLeft":21.0,"tempRight":22.0,"fanSpeed":4,"circulation":1,"fastCool":false,"fastHeat":true,"autoDefrost":false,"rearDefrost":true,"cabinTemp":23.0,"outdoorTemp":34.0,"coolantTemp":79.0}""",
        )
        assertTrue(climate is RemoteResponse.ClimateState)
        climate as RemoteResponse.ClimateState
        assertEquals(4, climate.fanSpeed)
        assertEquals(79.0, climate.coolantTemp ?: -1.0, 0.0)

        val parking = RemoteProtocolCodec.decodeResponse(
            """{"type":"parkingChargeState","target":55,"switchState":1,"mode":2}""",
        )
        assertTrue(parking is RemoteResponse.ParkingChargeState)
        assertEquals(2, (parking as RemoteResponse.ParkingChargeState).mode)
    }

    @Test
    fun leavesMissingV265FieldsNull() {
        val response = RemoteProtocolCodec.decodeResponse("""{"type":"lockState","state":1}""")
        assertTrue(response is RemoteResponse.LockState)
        response as RemoteResponse.LockState
        assertNull(response.fuelPercent)
        assertNull(response.coolantTemp)
        assertNull(response.raceChargeActive)
        assertNull(response.raceChargeTarget)
        assertNull(response.raceChargeEtaMin)
    }

    @Test
    fun reassemblesNewlineFramesAcrossChunks() {
        val assembler = NewlineFrameAssembler()
        assertEquals(emptyList<String>(), assembler.accept("""{"type":"lock""".toByteArray()))
        val frames = assembler.accept("""State","state":1}
{"type":"helloResult","success":true}
""".toByteArray())
        assertEquals(2, frames.size)
        assertEquals("""{"type":"lockState","state":1}""", frames[0])
        assertEquals("""{"type":"helloResult","success":true}""", frames[1])
    }
}
