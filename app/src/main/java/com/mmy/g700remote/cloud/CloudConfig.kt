package com.mmy.g700remote.cloud

/**
 * Single source of truth for the DisplayMirror cloud contract.
 *
 * The backend is the DisplayMirror developer's PocketBase instance plus an edge WebSocket relay.
 * All of the following were CONFIRMED live against the running backend (DisplayMirror 3.x):
 *  - [DEFAULT_API_BASE], [DEFAULT_RELAY_BASE], [FLEET_KEY]
 *  - [PATH_LOGIN] (PocketBase users auth-with-password)
 *  - [PATH_PULL_SETTINGS] / [PATH_PUSH_SETTINGS] (car-scoped settings sync)
 *  - [PATH_ADOPT_CAR] — the phone redeems the QR `pair` token (sent as `carToken`) to bind the car
 *  - [RELAY_PHONE_PATH] — the phone joins the relay on /ws/car with [HEADER_CAR_ID] +
 *    [HEADER_AUTH_TOKEN] = the QR pair token (101 verified; no account required)
 *
 * Everything funnels through here so any future change is a one-file edit. The rest of the app
 * (BLE/LAN control + all new feature UIs) works regardless of cloud availability.
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
     * Confirmed: the phone redeems the QR by calling adopt-car with the one-time QR `pair`
     * token in place of the car's secret `carToken`. Returns {claimed, pairingCode, relayUrl}.
     */
    const val PATH_ADOPT_CAR = "/api/adopt-car"

    /**
     * Relay leg — VERIFIED live. The phone connects to the SAME `/ws/car` endpoint as the car,
     * authenticating with `X-Car-Id` + `X-Auth-Token` = the QR **pair** token (confirmed: a 101
     * upgrade; the pair↔carId binding is validated — JWT/garbage/wrong-car all 401, and no account
     * is required). The relay then bridges the pair-authed connection to the real car (authed with
     * its secret carToken) and forwards the phone's open/msg/close envelopes to it.
     */
    const val RELAY_PHONE_PATH = "/ws/car"
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
