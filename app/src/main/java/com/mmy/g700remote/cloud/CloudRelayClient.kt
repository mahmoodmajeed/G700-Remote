package com.mmy.g700remote.cloud

import com.mmy.g700remote.ble.DisplayMirrorTransport
import com.mmy.g700remote.ble.RemoteCommandQueue
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.ScannedDevice
import com.mmy.g700remote.ble.TransportKind
import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.RemoteProtocolCodec
import com.mmy.g700remote.protocol.RemoteResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Cloud relay transport: the phone leg of the DisplayMirror WebSocket relay. Carries the same
 * newline-JSON remote protocol used over BLE/LAN, wrapped in the relay envelope (see
 * [CloudConfig]). This is the third transport that enables control from anywhere.
 *
 * The car leg of the relay is confirmed from the headunit APK; the phone leg here is inferred and
 * must be confirmed in one live calibration pass. All relay specifics live in [CloudConfig] so
 * calibration is a one-file change.
 */
class CloudRelayClient(
    private val scope: CoroutineScope,
    private val accountProvider: () -> CloudAccount?,
    private val boundCarProvider: () -> BoundCar?,
    private val pairingCodeProvider: () -> String?,
    private val httpClient: OkHttpClient = defaultClient(),
) : DisplayMirrorTransport {

    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    override val connectionState: StateFlow<RemoteConnectionState> = _connectionState

    private val _incoming = MutableSharedFlow<RemoteResponse>(extraBufferCapacity = 128)
    override val incoming: SharedFlow<RemoteResponse> = _incoming

    private var webSocket: WebSocket? = null
    private var commandQueue: RemoteCommandQueue? = null
    private var manualDisconnect = false
    private val sessionId: String = "phone-" + Integer.toHexString(System.identityHashCode(this))

    /** Cloud relay is not discoverable; the bound car is known from the QR binding. */
    override fun scanForDevices(): Flow<ScannedDevice> = emptyFlow()

    override suspend fun connect(address: String) {
        manualDisconnect = false
        closeSocketOnly()

        val account = accountProvider()
        val car = boundCarProvider()
        if (account == null) {
            _connectionState.value = RemoteConnectionState.Error("Sign in to use cloud control")
            return
        }
        if (car == null) {
            _connectionState.value = RemoteConnectionState.Error("Scan your car's QR code to use cloud control")
            return
        }

        val url = car.relayBase.trimEnd('/') + CloudConfig.RELAY_PHONE_PATH
        _connectionState.value = RemoteConnectionState.Connecting("cloud:${car.carId}")

        val opened = CompletableDeferred<Boolean>()
        // Relay phone-leg auth is enforced by an opaque edge worker (see CloudConfig). We send the
        // account JWT under several header names plus the car id; this is best-effort and may need
        // calibration against an online car. Local transports remain the reliable path.
        val request = Request.Builder()
            .url(toWebSocketUrl(url))
            .header(CloudConfig.HEADER_AUTH, CloudConfig.bearer(account.token))
            .header(CloudConfig.HEADER_AUTH_TOKEN, account.token)
            .header(CloudConfig.HEADER_CAR_ID, car.carId)
            .header(CloudConfig.HEADER_FLEET_KEY, CloudConfig.FLEET_KEY)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                if (CloudConfig.RELAY_USE_ENVELOPE) {
                    ws.send(
                        JSONObject()
                            .put(CloudConfig.ENVELOPE_TYPE, CloudConfig.ENVELOPE_OPEN)
                            .put(CloudConfig.ENVELOPE_SID, sessionId)
                            .toString(),
                    )
                }
                opened.complete(true)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                onRelayText(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(NORMAL_CLOSE, null)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                if (!opened.isCompleted) opened.complete(false)
                if (!manualDisconnect) {
                    _connectionState.value = RemoteConnectionState.Error(
                        t.message?.let { "Cloud relay error: $it" } ?: "Cloud relay disconnected",
                    )
                }
            }
        }

        webSocket = httpClient.newWebSocket(request, listener)
        commandQueue = RemoteCommandQueue(::writeFrame, incoming)

        val didOpen = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { opened.await() } ?: false
        if (!didOpen) {
            closeSocketOnly()
            _connectionState.value = RemoteConnectionState.Error("Cloud relay did not connect")
            return
        }

        _connectionState.value = RemoteConnectionState.Handshaking
        runCatching {
            val hello = commandQueue?.send(RemoteCommand.Hello(pairingCodeProvider()), HELLO_TIMEOUT_MS)
            when {
                hello is RemoteResponse.HelloResult && hello.success -> markReadyAndRefresh()
                hello is RemoteResponse.Error && hello.error == "pairing_code_required" ->
                    error("Enter the pairing code shown in DisplayMirror")
                hello is RemoteResponse.Error && hello.error == "bad_pairing_code" ->
                    error("Wrong DisplayMirror pairing code")
                hello is RemoteResponse.Error && hello.error == "locked_out" ->
                    error("Too many wrong codes. Try again later.")
                else -> error("Cloud handshake rejected by DisplayMirror")
            }
        }.onFailure {
            _connectionState.value = RemoteConnectionState.Error(it.message ?: "Cloud handshake failed")
            closeSocketOnly()
        }
    }

    override suspend fun disconnect() {
        manualDisconnect = true
        closeSocketOnly()
        _connectionState.value = RemoteConnectionState.Disconnected
    }

    override suspend fun send(command: RemoteCommand, timeoutMs: Long): RemoteResponse {
        if (_connectionState.value !is RemoteConnectionState.Ready && command !is RemoteCommand.Hello) {
            throw IllegalStateException("Cloud relay not connected")
        }
        val queue = commandQueue ?: throw IllegalStateException("Cloud relay command queue is not ready")
        return queue.send(command, timeoutMs)
    }

    private suspend fun markReadyAndRefresh() {
        _connectionState.value = RemoteConnectionState.Ready(TransportKind.Cloud)
        commandQueue?.send(RemoteCommand.Status, HELLO_TIMEOUT_MS)
    }

    /** Unwrap a relay frame and emit the inner protocol response(s). */
    private fun onRelayText(text: String) {
        val lines = extractProtocolLines(text)
        for (line in lines) {
            if (line.isBlank()) continue
            val response = runCatching { RemoteProtocolCodec.decodeResponse(line) }.getOrElse {
                RemoteResponse.Error("parse_error", it.message, retryAfterSec = null, raw = line)
            }
            _incoming.tryEmit(response)
        }
    }

    private fun extractProtocolLines(text: String): List<String> {
        if (!CloudConfig.RELAY_USE_ENVELOPE) return listOf(text.trim())
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return listOf(text.trim())
        if (!json.has(CloudConfig.ENVELOPE_DATA)) {
            // control frame (open/close ack) with no payload
            return emptyList()
        }
        // `d` may be a JSON object (already a response) or a string line.
        json.optJSONObject(CloudConfig.ENVELOPE_DATA)?.let { return listOf(it.toString()) }
        val data = json.optString(CloudConfig.ENVELOPE_DATA, "")
        return data.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    }

    private suspend fun writeFrame(frame: ByteArray) {
        val ws = webSocket ?: error("Cloud relay is not connected")
        val line = String(frame, StandardCharsets.UTF_8).trim()
        val payload = if (CloudConfig.RELAY_USE_ENVELOPE) {
            JSONObject()
                .put(CloudConfig.ENVELOPE_TYPE, CloudConfig.ENVELOPE_MSG)
                .put(CloudConfig.ENVELOPE_SID, sessionId)
                .put(CloudConfig.ENVELOPE_DATA, line)
                .toString()
        } else {
            line
        }
        if (!ws.send(payload)) error("Cloud relay send failed")
    }

    private fun closeSocketOnly() {
        runCatching { webSocket?.close(NORMAL_CLOSE, null) }
        webSocket = null
        commandQueue = null
    }

    private fun toWebSocketUrl(url: String): String = when {
        url.startsWith("wss://") -> "https://" + url.removePrefix("wss://")
        url.startsWith("ws://") -> "http://" + url.removePrefix("ws://")
        else -> url
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 12_000L
        private const val HELLO_TIMEOUT_MS = 8_000L
        private const val NORMAL_CLOSE = 1000

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }
}
