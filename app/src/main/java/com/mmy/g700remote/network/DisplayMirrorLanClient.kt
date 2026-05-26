package com.mmy.g700remote.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.mmy.g700remote.ble.DisplayMirrorTransport
import com.mmy.g700remote.ble.RemoteCommandQueue
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.ble.ScannedDevice
import com.mmy.g700remote.ble.TransportKind
import com.mmy.g700remote.protocol.NewlineFrameAssembler
import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.RemoteProtocolCodec
import com.mmy.g700remote.protocol.RemoteResponse
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class DisplayMirrorLanClient(
    context: Context,
    private val scope: CoroutineScope,
    private val pairingCodeProvider: () -> String? = { null },
) : DisplayMirrorTransport {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(NsdManager::class.java)
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)

    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    override val connectionState: StateFlow<RemoteConnectionState> = _connectionState

    private val _incoming = MutableSharedFlow<RemoteResponse>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<RemoteResponse> = _incoming

    private val assembler = NewlineFrameAssembler()
    private val writeMutex = Mutex()
    private var commandQueue: RemoteCommandQueue? = null
    private var socket: Socket? = null
    private var readerJob: Job? = null
    private var manualDisconnect = false

    override fun scanForDevices(): Flow<ScannedDevice> = callbackFlow {
        if (nsdManager == null) {
            _connectionState.value = RemoteConnectionState.Error("Network service discovery is unavailable")
            close()
            return@callbackFlow
        }

        _connectionState.value = RemoteConnectionState.Scanning
        val multicastLock = wifiManager?.createMulticastLock(MULTICAST_LOCK_TAG)?.apply {
            setReferenceCounted(false)
            runCatching { acquire() }
        }

        var resolving = false
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceType.equals(SERVICE_TYPE, ignoreCase = true)) return
                if (!serviceInfo.serviceName.contains(SERVICE_NAME, ignoreCase = true)) return
                if (resolving) return
                resolving = true
                nsdManager.resolveService(
                    serviceInfo,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            resolving = false
                        }

                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            resolving = false
                            val host = resolved.host?.hostAddress ?: return
                            val port = resolved.port.takeIf { it > 0 } ?: PORT
                            trySend(
                                ScannedDevice(
                                    name = resolved.serviceName,
                                    address = "$host:$port",
                                    rssi = 0,
                                    lastSeenMillis = System.currentTimeMillis(),
                                    transport = TransportKind.Lan,
                                ),
                            )
                        }
                    },
                )
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _connectionState.value = RemoteConnectionState.Error("mDNS discovery failed: $errorCode")
                runCatching { nsdManager.stopServiceDiscovery(this) }
                close()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                runCatching { nsdManager.stopServiceDiscovery(this) }
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
            runCatching { multicastLock?.release() }
            if (_connectionState.value is RemoteConnectionState.Scanning) {
                _connectionState.value = RemoteConnectionState.Disconnected
            }
        }
    }

    override suspend fun connect(address: String) {
        manualDisconnect = false
        disconnectSocketOnly()
        val endpoint = parseEndpoint(address) ?: discoverEndpoint()?.let { parseEndpoint(it.address) }
        if (endpoint == null) {
            _connectionState.value = RemoteConnectionState.Error("DisplayMirror LAN service was not found")
            return
        }

        _connectionState.value = RemoteConnectionState.Connecting("${endpoint.host}:${endpoint.port}")
        runCatching {
            val connectedSocket = withContext(Dispatchers.IO) {
                Socket().apply {
                    tcpNoDelay = true
                    keepAlive = true
                    connect(InetSocketAddress(endpoint.host, endpoint.port), CONNECT_TIMEOUT_MS)
                }
            }
            socket = connectedSocket
            commandQueue = RemoteCommandQueue(::writeFrame, incoming)
            startReader(connectedSocket)
            _connectionState.value = RemoteConnectionState.Handshaking
            val hello = commandQueue?.send(RemoteCommand.Hello(pairingCodeProvider()), 5_000)
            when {
                hello is RemoteResponse.HelloResult && hello.success -> markReadyAndRefresh()
                hello is RemoteResponse.Error && hello.error == "pairing_code_required" ->
                    error("Enter the pairing code shown in DisplayMirror")
                hello is RemoteResponse.Error && hello.error == "bad_pairing_code" ->
                    error("Wrong DisplayMirror pairing code")
                else -> error("Handshake rejected by DisplayMirror over LAN")
            }
        }.onFailure {
            failAndDisconnect(it.message ?: "LAN connection failed")
        }
    }

    override suspend fun disconnect() {
        manualDisconnect = true
        disconnectSocketOnly()
        _connectionState.value = RemoteConnectionState.Disconnected
    }

    override suspend fun send(command: RemoteCommand, timeoutMs: Long): RemoteResponse {
        if (_connectionState.value !is RemoteConnectionState.Ready && command !is RemoteCommand.Hello) {
            throw IllegalStateException("Not connected and handshaken")
        }
        val queue = commandQueue ?: throw IllegalStateException("LAN command queue is not ready")
        return queue.send(command, timeoutMs)
    }

    suspend fun discoverEndpoint(timeoutMs: Long = DISCOVERY_TIMEOUT_MS): ScannedDevice? =
        withTimeoutOrNull(timeoutMs) {
            scanForDevices().firstOrNull()
        }

    private suspend fun markReadyAndRefresh() {
        _connectionState.value = RemoteConnectionState.Ready(TransportKind.Lan)
        commandQueue?.send(RemoteCommand.Status, 5_000)
    }

    private fun startReader(connectedSocket: Socket) {
        readerJob = scope.launch(Dispatchers.IO) {
            runCatching {
                BufferedReader(InputStreamReader(connectedSocket.getInputStream(), Charsets.UTF_8)).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        assembler.accept((line + "\n").toByteArray(Charsets.UTF_8)).forEach { frame ->
                            val response = runCatching { RemoteProtocolCodec.decodeResponse(frame) }.getOrElse {
                                RemoteResponse.Error("parse_error", it.message, frame)
                            }
                            _incoming.emit(response)
                        }
                    }
                }
            }.onFailure {
                if (!manualDisconnect) {
                    _connectionState.value = RemoteConnectionState.Error("LAN disconnected")
                }
            }
        }
    }

    private suspend fun writeFrame(frame: ByteArray) {
        val connectedSocket = socket ?: error("LAN socket is not connected")
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                connectedSocket.getOutputStream().write(frame)
                connectedSocket.getOutputStream().flush()
            }
        }
    }

    private fun failAndDisconnect(message: String) {
        _connectionState.value = RemoteConnectionState.Error(message)
        disconnectSocketOnly()
    }

    private fun disconnectSocketOnly() {
        readerJob?.cancel()
        readerJob = null
        runCatching { socket?.close() }
        socket = null
        commandQueue = null
        assembler.clear()
    }

    private fun parseEndpoint(address: String): Endpoint? {
        if (address == AUTO_ADDRESS || address.count { it == ':' } > 1) return null
        val separator = address.lastIndexOf(':')
        if (separator <= 0 || separator == address.lastIndex) return null
        val host = address.substring(0, separator)
        val port = address.substring(separator + 1).toIntOrNull() ?: return null
        return Endpoint(host, port)
    }

    private data class Endpoint(val host: String, val port: Int)

    companion object {
        const val AUTO_ADDRESS = "mdns://_carkey._tcp./CarKey"
        const val SERVICE_NAME = "CarKey"
        const val SERVICE_TYPE = "_carkey._tcp."
        const val PORT = 9274
        private const val CONNECT_TIMEOUT_MS = 4_000
        private const val DISCOVERY_TIMEOUT_MS = 6_000L
        private const val MULTICAST_LOCK_TAG = "G700RemoteMdns"
    }
}
