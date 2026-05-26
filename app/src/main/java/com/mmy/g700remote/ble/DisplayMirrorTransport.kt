package com.mmy.g700remote.ble

import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.RemoteResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class ScannedDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val lastSeenMillis: Long,
    val transport: TransportKind = TransportKind.Ble,
)

enum class TransportKind {
    Ble,
    Lan,
}

enum class ConnectionPreference {
    BleFirst,
    LanFirst,
    BleOnly,
    LanOnly,
}

sealed class RemoteConnectionState {
    object Disconnected : RemoteConnectionState()
    object Scanning : RemoteConnectionState()
    data class Connecting(val address: String) : RemoteConnectionState()
    object DiscoveringServices : RemoteConnectionState()
    object EnablingNotifications : RemoteConnectionState()
    object Handshaking : RemoteConnectionState()
    data class Ready(val transport: TransportKind) : RemoteConnectionState()
    data class Error(val message: String) : RemoteConnectionState()
}

interface DisplayMirrorTransport {
    val connectionState: StateFlow<RemoteConnectionState>
    val incoming: SharedFlow<RemoteResponse>

    fun scanForDevices(): Flow<ScannedDevice>
    suspend fun connect(address: String)
    suspend fun disconnect()
    suspend fun send(command: RemoteCommand, timeoutMs: Long = 5_000): RemoteResponse
}
