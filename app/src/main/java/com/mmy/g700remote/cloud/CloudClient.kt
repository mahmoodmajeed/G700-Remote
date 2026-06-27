package com.mmy.g700remote.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * REST client for the DisplayMirror PocketBase cloud. Pure networking; no Android dependencies
 * so it stays unit-testable. All endpoints/headers come from [CloudConfig].
 */
class CloudClient(
    private val apiBase: String = CloudConfig.DEFAULT_API_BASE,
    private val httpClient: OkHttpClient = defaultClient(),
) {
    /** Authenticate with email + password (PocketBase users collection). Confirmed contract. */
    suspend fun login(email: String, password: String): CloudResult<CloudAccount> =
        post(
            path = CloudConfig.PATH_LOGIN,
            body = JSONObject()
                .put("identity", email.trim())
                .put("password", password),
        ).map { json ->
            val record = json.optJSONObject("record")
            CloudAccount(
                userId = record?.optString("id").orEmpty(),
                email = record?.optString("email")?.ifBlank { email }?.trim() ?: email.trim(),
                token = json.optString("token"),
            )
        }.failIfBlankToken()

    /**
     * Redeem the one-time QR `pair` token to bind the scanned car to this account. Confirmed:
     * adopt-car accepts the QR `pair` in place of the car's secret `carToken`. Returns
     * {claimed, pairingCode, relayUrl}.
     */
    suspend fun adoptCar(account: CloudAccount, payload: QrPairingPayload): CloudResult<AdoptResult> =
        post(
            path = CloudConfig.PATH_ADOPT_CAR,
            body = JSONObject()
                .put("carId", payload.carId)
                .put("carToken", payload.pairToken)
                .put("fleetKey", CloudConfig.FLEET_KEY),
            bearer = account.token,
        ).map { json ->
            AdoptResult(
                claimed = json.optBoolean("claimed", false),
                pairingCode = json.optString("pairingCode").ifBlank { null },
                relayUrl = json.optString("relayUrl").ifBlank { null },
            )
        }

    /** Pull the car-scoped synced settings (`display_prefs`). Confirmed contract. */
    suspend fun pullSettings(account: CloudAccount, carId: String): CloudResult<JSONObject?> =
        post(
            path = CloudConfig.PATH_PULL_SETTINGS,
            body = JSONObject().put("carId", carId),
            bearer = account.token,
        ).map { json -> json.optJSONObject("data") }

    /** Push the car-scoped synced settings. Confirmed contract. */
    suspend fun pushSettings(account: CloudAccount, carId: String, data: JSONObject): CloudResult<Unit> =
        post(
            path = CloudConfig.PATH_PUSH_SETTINGS,
            body = JSONObject().put("carId", carId).put("data", data),
            bearer = account.token,
        ).map { }

    private suspend fun post(
        path: String,
        body: JSONObject,
        bearer: String? = null,
    ): CloudResult<JSONObject> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(apiBase.trimEnd('/') + path)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .apply {
                bearer?.let { header(CloudConfig.HEADER_AUTH, CloudConfig.bearer(it)) }
            }
            .build()
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                if (response.isSuccessful) {
                    CloudResult.Success(json)
                } else {
                    val message = json.optString("message").ifBlank { httpError(response.code) }
                    CloudResult.Failure(message, response.code)
                }
            }
        }.getOrElse { throwable ->
            CloudResult.Failure(
                when (throwable) {
                    is IOException -> "Network error. Check your connection and try again."
                    else -> throwable.message ?: "Cloud request failed"
                },
            )
        }
    }

    private fun CloudResult<CloudAccount>.failIfBlankToken(): CloudResult<CloudAccount> =
        when (this) {
            is CloudResult.Success ->
                if (value.token.isBlank()) {
                    CloudResult.Failure("Sign-in did not return a session token")
                } else {
                    this
                }
            is CloudResult.Failure -> this
        }

    private fun httpError(code: Int): String = when (code) {
        400, 401 -> "Wrong email or password."
        403 -> "This account is not activated yet. Ask the DisplayMirror administrator to approve it, then try again."
        404 -> "Cloud endpoint not found"
        in 500..599 -> "Cloud server error ($code)"
        else -> "Cloud request failed ($code)"
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
