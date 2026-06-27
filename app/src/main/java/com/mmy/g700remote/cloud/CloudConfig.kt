package com.mmy.g700remote.cloud

/**
 * Single source of truth for the DisplayMirror cloud contract.
 *
 * The backend is the DisplayMirror developer's PocketBase instance. The following are
 * CONFIRMED from the headunit APK (DisplayMirror 3.x):
 *  - [DEFAULT_API_BASE], [DEFAULT_RELAY_BASE], [FLEET_KEY]
 *  - [PATH_LOGIN] (PocketBase users auth-with-password)
 *  - [PATH_PULL_SETTINGS] / [PATH_PUSH_SETTINGS] (the car calls these same endpoints)
 *
 * The following are INFERRED from the car side (the phone-leg counterparts are not in the
 * headunit APK) and must be confirmed in one live calibration pass with a real account+car:
 *  - [PATH_CLAIM_CAR] (phone redeems the QR `pair` token to bind the car to the account)
 *  - [RELAY_PHONE_PATH] and the relay auth headers ([HEADER_AUTH], [HEADER_CAR_ID])
 *
 * Everything funnels through here so calibration is a one-file change. The rest of the app
 * (BLE/LAN control + all new feature UIs) works regardless of cloud calibration.
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
     * Relay phone leg. The endpoint exists ([RELAY_PHONE_PATH] returns 401, not 404) but its
     * WebSocket auth is enforced by an opaque edge worker and could not be derived by probing
     * (JWT / pair / various headers all 401; an online car is needed to calibrate). We send the
     * car leg's header names as the best guess. Cloud relay is therefore best-effort; local
     * BLE/Wi-Fi control (with the QR pairing code) is the reliable path and never blocks on this.
     * The car leg is /ws/car with X-Car-Id + X-Auth-Token.
     */
    const val RELAY_PHONE_PATH = "/ws/phone"
    const val HEADER_AUTH = "Authorization"
    const val HEADER_CAR_ID = "X-Car-Id"
    const val HEADER_AUTH_TOKEN = "X-Auth-Token"
    const val HEADER_FLEET_KEY = "X-Fleet-Key"

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
