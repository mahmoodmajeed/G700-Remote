package com.mmy.g700remote.cloud

import org.json.JSONObject

/** Signed-in cloud account (DisplayMirror PocketBase user). */
data class CloudAccount(
    val userId: String,
    val email: String,
    val token: String,
)

/**
 * A car bound to the account, captured from the headunit QR code and confirmed via
 * `/api/claim-pair`. The [cloudClientToken] (returned by claim-pair as `clientToken`) is the
 * credential sent as `X-Auth-Token` on the `/ws/phone` WebSocket leg. The [pairToken] from the
 * QR is the one-time token passed to claim-pair and is kept only for re-binding purposes.
 */
data class BoundCar(
    val carId: String,
    val apiBase: String,
    val relayBase: String,
    val pairingCode: String,
    /** QR one-time `pair` token — passed to /api/claim-pair, NOT used for relay auth. */
    val pairToken: String = "",
    /** Relay phone-leg credential minted by /api/claim-pair → `clientToken`. Used as X-Auth-Token on /ws/phone. */
    val cloudClientToken: String = "",
    val name: String? = null,
    val boundAtMillis: Long = System.currentTimeMillis(),
)

/** Parsed, not-yet-bound QR payload. `pair` is a one-time bind token redeemed via /api/claim-pair. */
data class QrPairingPayload(
    val schemaVersion: Int,
    val relayBase: String,
    val apiBase: String,
    val carId: String,
    val pairToken: String,
    val pairingCode: String,
) {
    fun toBoundCar(name: String? = null): BoundCar = BoundCar(
        carId = carId,
        apiBase = apiBase.ifBlank { CloudConfig.DEFAULT_API_BASE },
        relayBase = relayBase.ifBlank { CloudConfig.DEFAULT_RELAY_BASE },
        pairingCode = pairingCode,
        pairToken = pairToken,
        name = name,
    )

    companion object {
        /**
         * Parses the headunit QR text. Returns null if it is not a recognizable DisplayMirror
         * pairing payload, so the scanner can keep scanning instead of binding garbage.
         */
        fun parse(raw: String): QrPairingPayload? {
            val text = raw.trim()
            if (!text.startsWith("{")) return null
            return runCatching {
                val json = JSONObject(text)
                val car = json.optString("car").trim()
                if (car.isEmpty()) return null
                QrPairingPayload(
                    schemaVersion = json.optInt("v", 0),
                    relayBase = json.optString("relay").trim(),
                    apiBase = json.optString("api").trim(),
                    carId = car,
                    pairToken = json.optString("pair").trim(),
                    pairingCode = json.optString("code").trim(),
                )
            }.getOrNull()?.takeIf { it.pairingCode.isNotEmpty() || it.pairToken.isNotEmpty() }
        }
    }
}

/**
 * Response of /api/claim-pair: the phone's relay credential and confirmed car binding.
 * Fields map directly from the JSON: carId, clientToken, relayUrl, pairingCode.
 */
data class ClaimPairResult(
    val carId: String,
    val clientToken: String,
    val relayUrl: String,
    val pairingCode: String?,
)

/** Result of a cloud call that the UI can render without leaking transport details. */
sealed class CloudResult<out T> {
    data class Success<T>(val value: T) : CloudResult<T>()
    data class Failure(val message: String, val code: Int? = null) : CloudResult<Nothing>()
}

inline fun <T, R> CloudResult<T>.map(transform: (T) -> R): CloudResult<R> = when (this) {
    is CloudResult.Success -> CloudResult.Success(transform(value))
    is CloudResult.Failure -> this
}
