package com.mmy.g700remote.protocol

import org.json.JSONObject

sealed class RemoteResponse {
    abstract val type: String
    abstract val raw: String

    data class HelloResult(
        val success: Boolean,
        val protocolVersion: Int?,
        override val raw: String,
    ) : RemoteResponse() {
        override val type: String = "helloResult"
    }

    data class LockState(
        val state: Int?,
        val cabinTemp: Double?,
        val outdoorTemp: Double?,
        val coolantTemp: Double?,
        val batterySoc: Int?,
        val fuelPercent: Int?,
        val acOn: Boolean?,
        val chargingState: Int?,
        val chargeRemainTime: Int?,
        val packVoltage: Double?,
        val packCurrent: Double?,
        val packPower: Double?,
        val chargeMode: String?,
        val parkingChargeTargetSoc: Int?,
        val parkingChargeEtaMin: Int?,
        val dischargeEtaMin: Int?,
        val safetySocFloor: Int?,
        val raceChargeActive: Boolean?,
        val raceChargeTarget: Int?,
        val raceChargeEtaMin: Int?,
        override val raw: String,
    ) : RemoteResponse() {
        override val type: String = "lockState"
    }

    data class ClimateState(
        val acOn: Boolean?,
        val tempLeft: Double?,
        val tempRight: Double?,
        val fanSpeed: Int?,
        val circulation: Int?,
        val fastCool: Boolean?,
        val fastHeat: Boolean?,
        val autoDefrost: Boolean?,
        val rearDefrost: Boolean?,
        val cabinTemp: Double?,
        val outdoorTemp: Double?,
        val coolantTemp: Double?,
        override val raw: String,
    ) : RemoteResponse() {
        override val type: String = "climateState"
    }

    data class ParkingChargeState(
        val target: Int?,
        val switchState: Int?,
        val mode: Int?,
        override val raw: String,
    ) : RemoteResponse() {
        override val type: String = "parkingChargeState"
    }

    data class Error(
        val error: String?,
        val message: String?,
        override val raw: String,
    ) : RemoteResponse() {
        override val type: String = "error"
    }

    data class Unknown(
        override val type: String,
        override val raw: String,
    ) : RemoteResponse()

    companion object {
        fun fromJson(raw: String): RemoteResponse {
            val json = JSONObject(raw)
            return when (val type = json.optString("type", "")) {
                "helloResult" -> HelloResult(
                    success = json.optBoolean("success", false),
                    protocolVersion = json.optIntOrNull("protocolVersion"),
                    raw = raw,
                )

                "lockState" -> LockState(
                    state = json.optIntOrNull("state"),
                    cabinTemp = json.optDoubleOrNull("cabinTemp"),
                    outdoorTemp = json.optDoubleOrNull("outdoorTemp"),
                    coolantTemp = json.optDoubleOrNull("coolantTemp"),
                    batterySoc = json.optIntOrNull("batterySOC"),
                    fuelPercent = json.optIntOrNull("fuelPercent"),
                    acOn = json.optBooleanOrNull("acOn"),
                    chargingState = json.optIntOrNull("chargingState"),
                    chargeRemainTime = json.optIntOrNull("chargeRemainTime"),
                    packVoltage = json.optDoubleOrNull("packVoltage"),
                    packCurrent = json.optDoubleOrNull("packCurrent"),
                    packPower = json.optDoubleOrNull("packPower"),
                    chargeMode = json.optStringOrNull("chargeMode"),
                    parkingChargeTargetSoc = json.optIntOrNull("parkingChargeTargetSOC"),
                    parkingChargeEtaMin = json.optIntOrNull("parkingChargeEtaMin"),
                    dischargeEtaMin = json.optIntOrNull("dischargeEtaMin"),
                    safetySocFloor = json.optIntOrNull("safetySocFloor"),
                    raceChargeActive = json.optBooleanOrNull("raceChargeActive"),
                    raceChargeTarget = json.optIntOrNull("raceChargeTarget"),
                    raceChargeEtaMin = json.optIntOrNull("raceChargeEtaMin"),
                    raw = raw,
                )

                "climateState" -> ClimateState(
                    acOn = json.optBooleanOrNull("acOn"),
                    tempLeft = json.optDoubleOrNull("tempLeft"),
                    tempRight = json.optDoubleOrNull("tempRight"),
                    fanSpeed = json.optIntOrNull("fanSpeed"),
                    circulation = json.optIntOrNull("circulation"),
                    fastCool = json.optBooleanOrNull("fastCool"),
                    fastHeat = json.optBooleanOrNull("fastHeat"),
                    autoDefrost = json.optBooleanOrNull("autoDefrost"),
                    rearDefrost = json.optBooleanOrNull("rearDefrost"),
                    cabinTemp = json.optDoubleOrNull("cabinTemp"),
                    outdoorTemp = json.optDoubleOrNull("outdoorTemp"),
                    coolantTemp = json.optDoubleOrNull("coolantTemp"),
                    raw = raw,
                )

                "parkingChargeState" -> ParkingChargeState(
                    target = json.optIntOrNull("target"),
                    switchState = json.optIntOrNull("switchState"),
                    mode = json.optIntOrNull("mode"),
                    raw = raw,
                )

                "error" -> Error(
                    error = json.optStringOrNull("error"),
                    message = json.optStringOrNull("message"),
                    raw = raw,
                )

                else -> Unknown(type = type.ifBlank { "unknown" }, raw = raw)
            }
        }
    }
}

private fun JSONObject.optIntOrNull(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null

private fun JSONObject.optDoubleOrNull(name: String): Double? =
    if (has(name) && !isNull(name)) optDouble(name) else null

private fun JSONObject.optBooleanOrNull(name: String): Boolean? =
    if (has(name) && !isNull(name)) optBoolean(name) else null

private fun JSONObject.optStringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) optString(name) else null
