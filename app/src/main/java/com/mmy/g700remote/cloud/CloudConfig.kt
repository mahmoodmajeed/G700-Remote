package com.mmy.g700remote.cloud

/**
 * Single source of truth for the DisplayMirror cloud contract.
 *
 * Two-layer auth model (confirmed from decompiled official Car Key companion app):
 *  Layer 1 — relay transport: POST /api/claim-pair with QR `pair` token → returns `clientToken`.
 *             Phone WebSocket upgrade to /ws/phone sends X-Car-Id + X-Auth-Token=clientToken.
 *  Layer 2 — in-channel handshake: `{"cmd":"hello","protocolVersion":5,"pairingCode":"<digits>"}`.
 *
 * All of the following were CONFIRMED live against the running backend (DisplayMirror 3.x):
 *  - [DEFAULT_API_BASE], [DEFAULT_RELAY_BASE], [FLEET_KEY]
 *  - [PATH_LOGIN] (PocketBase users auth-with-password)
 *  - [PATH_CLAIM_PAIR] — redeems QR pair token → returns clientToken + carId + relayUrl
 *  - [PATH_PULL_SETTINGS] / [PATH_PUSH_SETTINGS] (car-scoped settings sync)
 *  - [RELAY_PHONE_PATH] with [HEADER_CAR_ID] + [HEADER_AUTH_TOKEN]=clientToken → 101
 */
object CloudConfig {
    const val DEFAULT_API_BASE = "https://car-api.wowbooking.one"
    const val DEFAULT_RELAY_BASE = "wss://car.wowbooking.one"
    const val FLEET_KEY = "7VH_r1OpXzkO_jSQkJdaxdymEgidZoshiWNagNMOihk"

    // Confirmed live against the running backend.
    const val PATH_LOGIN = "/api/collections/users/auth-with-password"
    const val PATH_PULL_SETTINGS = "/api/pull-settings"
    const val PATH_PUSH_SETTINGS = "/api/push-settings"
    const val PATH_CARS = "/api/collections/cars/records"

    /**
     * Redeems the QR `pair` token to bind the car and mint a relay phone credential.
     * POST {pair, device, deviceId} + Bearer JWT → {carId, clientToken, relayUrl, pairingCode?}.
     * The returned `clientToken` is stored as [BoundCar.cloudClientToken] and used as
     * X-Auth-Token on [RELAY_PHONE_PATH]. Confirmed from official Car Key companion app source.
     */
    const val PATH_CLAIM_PAIR = "/api/claim-pair"

    /**
     * Phone-leg WebSocket endpoint. Upgrade headers: X-Car-Id=carId, X-Auth-Token=clientToken
     * (from /api/claim-pair response). Confirmed from official Car Key companion app.
     */
    const val RELAY_PHONE_PATH = "/ws/phone"
    const val HEADER_AUTH = "Authorization"
    const val HEADER_CAR_ID = "X-Car-Id"
    const val HEADER_AUTH_TOKEN = "X-Auth-Token"

    const val QR_SCHEMA_VERSION = 1

    /**
     * Relay frame envelope. The car leg uses `{"t":"open|msg|close","sid":<session>,"d":<line>}`.
     * We mirror that on the phone leg (INFERRED). If the live phone leg turns out to be a raw
     * newline-JSON passthrough, set [RELAY_USE_ENVELOPE] = false during calibration.
     */
    const val RELAY_USE_ENVELOPE = true
    const val ENVELOPE_TYPE = "t"
    const val ENVELOPE_SID = "sid"
    const val ENVELOPE_DATA = "d"
    const val ENVELOPE_OPEN = "open"
    const val ENVELOPE_MSG = "msg"
    const val ENVELOPE_CLOSE = "close"

    fun bearer(token: String): String = "Bearer $token"
}
