package com.mmy.g700remote.data

import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.ble.DisplayMirrorBleClient
import com.mmy.g700remote.ble.DisplayMirrorTransport
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.ScannedDevice
import com.mmy.g700remote.ble.TransportKind
import com.mmy.g700remote.network.DisplayMirrorLanClient
import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.RemoteResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class CompositeDisplayMirrorTransport(
    private val ble: DisplayMirrorTransport,
    private val lan: DisplayMirrorLanClient,
    private val cloud: DisplayMirrorTransport,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) : DisplayMirrorTransport {
    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    override val connectionState: StateFlow<RemoteConnectionState> = _connectionState

    private val _incoming = MutableSharedFlow<RemoteResponse>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<RemoteResponse> = _incoming

    private var activeTransport: DisplayMirrorTransport? = null
    private var activeKind: TransportKind? = null
    private var activeAddress: String? = null

    init {
        scope.launch {
            ble.connectionState.collect { state ->
                if (activeKind == TransportKind.Ble) {
                    _connectionState.value = normalizeReady(state, TransportKind.Ble)
                }
            }
        }
        scope.launch {
            lan.connectionState.collect { state ->
                if (activeKind == TransportKind.Lan) {
                    _connectionState.value = normalizeReady(state, TransportKind.Lan)
                }
            }
        }
        scope.launch {
            cloud.connectionState.collect { state ->
                if (activeKind == TransportKind.Cloud) {
                    _connectionState.value = normalizeReady(state, TransportKind.Cloud)
                }
            }
        }
        scope.launch {
            ble.incoming.collect { _incoming.emit(it) }
        }
        scope.launch {
            lan.incoming.collect { _incoming.emit(it) }
        }
        scope.launch {
            cloud.incoming.collect { _incoming.emit(it) }
        }
    }

    private fun isCloudAvailable(): Boolean =
        settings.isCloudEnabled() &&
            settings.getBoundCar()?.pairToken?.isNotBlank() == true

    override fun scanForDevices(): Flow<ScannedDevice> = callbackFlow {
        _connectionState.value = RemoteConnectionState.Scanning
        val jobs = mutableListOf(
            if (settings.isBleEnabled()) {
                launch {
                    ble.scanForDevices().collect { send(it.copy(transport = TransportKind.Ble)) }
                }
            } else {
                null
            },
            if (settings.isLanEnabled()) {
                launch {
                    lan.scanForDevices().collect { send(it.copy(transport = TransportKind.Lan)) }
                }
            } else {
                null
            },
        )
        awaitClose {
            jobs.filterNotNull().forEach { it.cancel() }
            if (_connectionState.value is RemoteConnectionState.Scanning) {
                _connectionState.value = RemoteConnectionState.Disconnected
            }
        }
    }

    override suspend fun connect(address: String) {
        val candidates = candidatesFor(address)
        if (candidates.isEmpty()) {
            _connectionState.value = RemoteConnectionState.Error("Enable BLE, LAN, or both in Settings")
            return
        }

        var lastError = "Connection failed"
        for (candidate in candidates) {
            val result = connectCandidate(candidate, address)
            if (result == null) return
            lastError = result
            delay(250)
        }
        activeTransport = null
        activeKind = null
        _connectionState.value = RemoteConnectionState.Error(lastError)
    }

    override suspend fun disconnect() {
        activeTransport = null
        activeKind = null
        activeAddress = null
        ble.disconnect()
        lan.disconnect()
        cloud.disconnect()
        _connectionState.value = RemoteConnectionState.Disconnected
    }

    override suspend fun send(command: RemoteCommand, timeoutMs: Long): RemoteResponse {
        val current = activeTransport ?: throw IllegalStateException("No active DisplayMirror transport")
        try {
            return current.send(command, timeoutMs)
        } catch (firstFailure: Throwable) {
            // The active transport dropped mid-command (common over BLE). Give its built-in
            // auto-reconnect a short window to recover, then retry the same command once.
            val recovered = withTimeoutOrNull(RECOVER_WAIT_MS) {
                current.connectionState.first {
                    it is RemoteConnectionState.Ready || it is RemoteConnectionState.Error
                }
            }
            if (recovered is RemoteConnectionState.Ready) {
                runCatching { return current.send(command, timeoutMs) }
            }
            // Still down — fall back to another transport (e.g. cloud / LAN) if one is available.
            val address = activeAddress
            val kind = activeKind
            if (address != null && kind != null) {
                val fallback = fallbackFor(kind)
                if (fallback != null) {
                    val fallbackError = connectCandidate(fallback, address)
                    if (fallbackError == null) {
                        return activeTransport?.send(command, timeoutMs)
                            ?: throw IllegalStateException("Fallback transport is not ready")
                    }
                }
            }
            throw firstFailure
        }
    }

    private suspend fun connectCandidate(kind: TransportKind, savedAddress: String): String? {
        activeKind = kind
        val transport = transportFor(kind)
        activeTransport = transport
        val address = when (kind) {
            TransportKind.Ble -> if (looksLikeBleMac(savedAddress)) savedAddress else DisplayMirrorBleClient.AUTO_ADDRESS
            TransportKind.Lan -> if (looksLikeLanEndpoint(savedAddress)) savedAddress else DisplayMirrorLanClient.AUTO_ADDRESS
            TransportKind.Cloud -> savedAddress
        }
        activeAddress = savedAddress
        transport.connect(address)
        val state = withTimeoutOrNull(CONNECT_WAIT_MS) {
            transport.connectionState.first {
                it is RemoteConnectionState.Ready || it is RemoteConnectionState.Error
            }
        }
        return when (state) {
            is RemoteConnectionState.Ready -> {
                _connectionState.value = RemoteConnectionState.Ready(kind)
                null
            }
            is RemoteConnectionState.Error -> {
                transport.disconnect()
                state.message
            }
            else -> {
                transport.disconnect()
                "${kind.label()} connection timed out"
            }
        }
    }

    private fun candidatesFor(address: String): List<TransportKind> {
        val bleEnabled = settings.isBleEnabled()
        val lanEnabled = settings.isLanEnabled()
        val cloudEnabled = isCloudAvailable()
        // BLE and LAN both auto-discover (BLE scans the service UUID, LAN uses mDNS), so they
        // are always valid candidates when enabled — even when the saved address is a cloud
        // carId from QR pairing rather than a concrete MAC / host:port.
        val preferred = when (settings.getConnectionPreference()) {
            ConnectionPreference.BleFirst -> listOf(TransportKind.Ble, TransportKind.Lan, TransportKind.Cloud)
            ConnectionPreference.LanFirst -> listOf(TransportKind.Lan, TransportKind.Ble, TransportKind.Cloud)
            ConnectionPreference.LocalFirst -> listOf(TransportKind.Ble, TransportKind.Lan, TransportKind.Cloud)
            ConnectionPreference.CloudFirst -> listOf(TransportKind.Cloud, TransportKind.Lan, TransportKind.Ble)
            ConnectionPreference.BleOnly -> listOf(TransportKind.Ble)
            ConnectionPreference.LanOnly -> listOf(TransportKind.Lan)
            ConnectionPreference.CloudOnly -> listOf(TransportKind.Cloud)
        }
        return preferred
            .filter { it != TransportKind.Ble || bleEnabled }
            .filter { it != TransportKind.Lan || lanEnabled }
            .filter { it != TransportKind.Cloud || cloudEnabled }
    }

    private fun looksLikeBleMac(address: String): Boolean = address.count { it == ':' } == 5

    private fun fallbackFor(kind: TransportKind): TransportKind? {
        val candidates = candidatesFor(activeAddress ?: return null)
        return candidates.firstOrNull { it != kind }
    }

    private fun transportFor(kind: TransportKind): DisplayMirrorTransport =
        when (kind) {
            TransportKind.Ble -> ble
            TransportKind.Lan -> lan
            TransportKind.Cloud -> cloud
        }

    private fun normalizeReady(state: RemoteConnectionState, kind: TransportKind): RemoteConnectionState =
        if (state is RemoteConnectionState.Ready) RemoteConnectionState.Ready(kind) else state

    private fun looksLikeLanEndpoint(address: String): Boolean =
        address == DisplayMirrorLanClient.AUTO_ADDRESS || address.count { it == ':' } == 1

    private fun TransportKind.label(): String =
        when (this) {
            TransportKind.Ble -> "BLE"
            TransportKind.Lan -> "LAN"
            TransportKind.Cloud -> "Cloud"
        }

    companion object {
        private const val CONNECT_WAIT_MS = 14_000L
        private const val RECOVER_WAIT_MS = 7_000L
    }
}
