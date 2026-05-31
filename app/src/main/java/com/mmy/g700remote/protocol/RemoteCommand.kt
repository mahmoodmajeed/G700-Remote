package com.mmy.g700remote.protocol

import org.json.JSONObject

sealed class RemoteCommand {
    abstract val cmd: String
    open val sensitive: Boolean = false
    open val expectedResponseTypes: Set<String> = setOf("lockState", "error")

    data class Hello(val pairingCode: String? = null) : RemoteCommand() {
        override val cmd: String = "hello"
        override val expectedResponseTypes: Set<String> = setOf("helloResult", "error")
    }

    object Status : RemoteCommand() {
        override val cmd: String = "status"
    }

    object Ping : RemoteCommand() {
        override val cmd: String = "ping"
    }

    object Lock : RemoteCommand() {
        override val cmd: String = "lock"
    }

    object Unlock : RemoteCommand() {
        override val cmd: String = "unlock"
        override val sensitive: Boolean = true
    }

    data class Window(val action: WindowAction) : RemoteCommand() {
        override val cmd: String = "window"
        override val sensitive: Boolean = action in setOf(
            WindowAction.OpenAll,
            WindowAction.FrontOpen,
            WindowAction.Vent,
        )
    }

    data class Sunroof(val action: OpenCloseAction) : RemoteCommand() {
        override val cmd: String = "sunroof"
        override val sensitive: Boolean = action == OpenCloseAction.Open
    }

    data class Sunshade(val action: OpenCloseAction) : RemoteCommand() {
        override val cmd: String = "sunshade"
    }

    data class Hazards(val action: OnOffAction) : RemoteCommand() {
        override val cmd: String = "hazards"
    }

    data class Drl(val action: OnOffAction) : RemoteCommand() {
        override val cmd: String = "drl"
    }

    data class Mirror(val action: MirrorAction) : RemoteCommand() {
        override val cmd: String = "mirror"
    }

    object GetLocation : RemoteCommand() {
        override val cmd: String = "get_location"
        override val expectedResponseTypes: Set<String> = setOf("location", "error")
    }

    data class Navigate(
        val lat: Double? = null,
        val lon: Double? = null,
        val label: String? = null,
        val query: String? = null,
    ) : RemoteCommand() {
        init {
            val hasCoords = lat != null && lon != null
            val hasQuery = !query.isNullOrBlank()
            require(hasCoords || hasQuery) { "Navigation requires coordinates or query" }
            if (hasCoords) {
                require(lat in -90.0..90.0) { "Latitude must be -90..90" }
                require(lon in -180.0..180.0) { "Longitude must be -180..180" }
            }
        }

        override val cmd: String = "navigate"
        override val expectedResponseTypes: Set<String> = setOf("navigate", "error")
    }

    data class Soc(val value: Int) : RemoteCommand() {
        init {
            require(value in 25..70) { "SOC target must be 25..70" }
        }

        override val cmd: String = "soc"
    }

    data class ParkingCharge(val action: ParkingChargeAction) : RemoteCommand() {
        override val cmd: String = "parking_charge"
        override val sensitive: Boolean = action != ParkingChargeAction.Status
        override val expectedResponseTypes: Set<String> = setOf("parkingChargeState", "error")
    }

    data class RaceCharge(
        val action: RaceChargeAction,
        val target: Int? = null,
    ) : RemoteCommand() {
        init {
            if (action == RaceChargeAction.Start) {
                require((target ?: 80) in 25..100) { "Race charge target must be 25..100" }
            }
        }

        override val cmd: String = "race_charge"
        override val sensitive: Boolean = action != RaceChargeAction.Status
        override val expectedResponseTypes: Set<String> = setOf("lockState", "error")
    }

    data class Climate(
        val action: ClimateAction,
        val numericValue: Number? = null,
        val position: SeatPosition? = null,
        val level: Int? = null,
    ) : RemoteCommand() {
        init {
            if (action == ClimateAction.SetSeatHeat || action == ClimateAction.SetSeatVent) {
                require(position != null) { "Seat commands require a position" }
                require((level ?: -1) in 0..3) { "Seat level must be 0..3" }
            }
        }

        override val cmd: String = "climate"
        override val expectedResponseTypes: Set<String> = setOf("climateState", "error")
    }

    fun toJson(): JSONObject {
        val json = JSONObject().put("cmd", cmd)
        when (this) {
            is Hello -> {
                json.put("protocolVersion", RemoteProtocolCodec.PROTOCOL_VERSION)
                pairingCode?.trim()?.takeIf { it.isNotEmpty() }?.let { json.put("pairingCode", it) }
            }
            Status, Ping, Lock, Unlock -> Unit
            is Window -> json.put("action", action.wireValue)
            is Sunroof -> json.put("action", action.wireValue)
            is Sunshade -> json.put("action", action.wireValue)
            is Hazards -> json.put("action", action.wireValue)
            is Drl -> json.put("action", action.wireValue)
            is Mirror -> json.put("action", action.wireValue)
            GetLocation -> Unit
            is Navigate -> {
                lat?.let { json.put("lat", it) }
                lon?.let { json.put("lon", it) }
                label?.trim()?.takeIf { it.isNotEmpty() }?.let { json.put("label", it) }
                query?.trim()?.takeIf { it.isNotEmpty() }?.let { json.put("query", it) }
            }
            is Soc -> json.put("value", value)
            is ParkingCharge -> json.put("action", action.wireValue)
            is RaceCharge -> {
                json.put("action", action.wireValue)
                if (action == RaceChargeAction.Start) {
                    json.put("target", target ?: 80)
                }
            }
            is Climate -> {
                json.put("action", action.wireValue)
                numericValue?.let { json.put("value", it) }
                position?.let { json.put("position", it.wireValue) }
                level?.let { json.put("level", it.coerceIn(0, 3)) }
            }
        }
        return json
    }

    fun displayName(): String = when (this) {
        is Hello -> "Handshake"
        Status -> "Status"
        Ping -> "Ping"
        Lock -> "Lock"
        Unlock -> "Unlock"
        is Window -> "Window ${action.label}"
        is Sunroof -> "Sunroof ${action.label}"
        is Sunshade -> "Sunshade ${action.label}"
        is Hazards -> "Hazards ${action.label}"
        is Drl -> "DRL ${action.label}"
        is Mirror -> "Mirrors ${action.label}"
        GetLocation -> "Get car location"
        is Navigate -> "Send destination"
        is Soc -> "Set SOC $value%"
        is ParkingCharge -> "Parking charge ${action.label}"
        is RaceCharge -> "Race charge ${action.label}"
        is Climate -> "Climate ${action.label}"
    }
}

enum class WindowAction(val wireValue: String, val label: String) {
    OpenAll("open_all", "open all"),
    CloseAll("close_all", "close all"),
    FrontOpen("front_open", "front open"),
    FrontClose("front_close", "front close"),
    Vent("vent", "vent"),
}

enum class OpenCloseAction(val wireValue: String, val label: String) {
    Open("open", "open"),
    Close("close", "close"),
}

enum class OnOffAction(val wireValue: String, val label: String) {
    On("on", "on"),
    Off("off", "off"),
}

enum class MirrorAction(val wireValue: String, val label: String) {
    Fold("fold", "fold"),
    Unfold("unfold", "unfold"),
}

enum class ParkingChargeAction(val wireValue: String, val label: String) {
    Status("status", "status"),
    Off("off", "off"),
    Quiet("quiet", "quiet"),
    Fast("fast", "fast"),
}

enum class RaceChargeAction(val wireValue: String, val label: String) {
    Status("status", "status"),
    Start("start", "start"),
    Stop("stop", "stop"),
}

enum class ClimateAction(val wireValue: String, val label: String) {
    Status("status", "status"),
    AcOn("ac_on", "AC on"),
    AcOff("ac_off", "AC off"),
    SetTempLeft("set_temp_left", "left temp"),
    SetTempRight("set_temp_right", "right temp"),
    SetFanSpeed("set_fan_speed", "fan speed"),
    SetCirculation("set_circulation", "circulation"),
    FastCoolOn("fast_cool_on", "fast cool on"),
    FastCoolOff("fast_cool_off", "fast cool off"),
    FastHeatOn("fast_heat_on", "fast heat on"),
    FastHeatOff("fast_heat_off", "fast heat off"),
    AutoDefrostOn("auto_defrost_on", "auto defrost on"),
    AutoDefrostOff("auto_defrost_off", "auto defrost off"),
    RearDefrostOn("rear_defrost_on", "rear defrost on"),
    RearDefrostOff("rear_defrost_off", "rear defrost off"),
    FrontHeatOn("front_heat_on", "front heat on"),
    FrontHeatOff("front_heat_off", "front heat off"),
    Pm25On("pm25_on", "PM2.5 on"),
    Pm25Off("pm25_off", "PM2.5 off"),
    ParkingAcSmart("parking_ac_smart", "parking AC smart"),
    ParkingAcVent("parking_ac_vent", "parking AC vent"),
    ParkingAcOff("parking_ac_off", "parking AC off"),
    SteeringHeatOn("steering_heat_on", "steering heat on"),
    SteeringHeatOff("steering_heat_off", "steering heat off"),
    SetSeatHeat("set_seat_heat", "seat heat"),
    SetSeatVent("set_seat_vent", "seat vent"),
}

enum class SeatPosition(val wireValue: String, val label: String) {
    FrontLeft("fl", "Driver"),
    FrontRight("fr", "Passenger"),
    RearLeft("rl", "Rear Left"),
    RearRight("rr", "Rear Right"),
}
