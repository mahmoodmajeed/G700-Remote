package com.mmy.g700remote.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.mmy.g700remote.protocol.NewlineFrameAssembler
import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.RemoteProtocolCodec
import com.mmy.g700remote.protocol.RemoteResponse
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class DisplayMirrorBleClient(
    private val context: Context,
    private val scope: CoroutineScope,
    private val pairingCodeProvider: () -> String? = { null },
) : DisplayMirrorTransport {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    override val connectionState: StateFlow<RemoteConnectionState> = _connectionState

    private val _incoming = MutableSharedFlow<RemoteResponse>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<RemoteResponse> = _incoming

    private val assembler = NewlineFrameAssembler()
    private val writeMutex = Mutex()
    private var commandQueue: RemoteCommandQueue? = null
    private var gatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var responseCharacteristic: BluetoothGattCharacteristic? = null
    private var pendingWrite: CompletableDeferred<Unit>? = null
    private var pendingDescriptorWrite: CompletableDeferred<Unit>? = null
    private var negotiatedMtu = DEFAULT_MTU
    private var connectedAddress: String? = null
    private var manualDisconnect = false
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var serviceDiscoveryRetry = 0

    override fun scanForDevices(): Flow<ScannedDevice> = callbackFlow {
        if (!hasBluetoothPermissions()) {
            _connectionState.value = RemoteConnectionState.Error("Bluetooth permissions are not granted")
            close()
            return@callbackFlow
        }
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = RemoteConnectionState.Error("BLE scanner is unavailable")
            close()
            return@callbackFlow
        }

        _connectionState.value = RemoteConnectionState.Scanning
        val callback = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val scanned = ScannedDevice(
                    name = runCatching { device.name }.getOrNull() ?: result.scanRecord?.deviceName,
                    address = device.address,
                    rssi = result.rssi,
                    lastSeenMillis = System.currentTimeMillis(),
                )
                trySend(scanned)
            }

            override fun onScanFailed(errorCode: Int) {
                _connectionState.value = RemoteConnectionState.Error("BLE scan failed: $errorCode")
                close()
            }
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        @SuppressLint("MissingPermission")
        scanner.startScan(listOf(filter), settings, callback)

        awaitClose {
            @SuppressLint("MissingPermission")
            scanner.stopScan(callback)
            if (_connectionState.value is RemoteConnectionState.Scanning) {
                _connectionState.value = RemoteConnectionState.Disconnected
            }
        }
    }

    override suspend fun connect(address: String) {
        if (!hasBluetoothPermissions()) {
            _connectionState.value = RemoteConnectionState.Error("Bluetooth permissions are not granted")
            return
        }
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = RemoteConnectionState.Error("Bluetooth is off or unavailable")
            return
        }
        manualDisconnect = false
        reconnectJob?.cancel()
        disconnectGattOnly()
        // QR pairing does not carry a BLE MAC, so when we don't have a concrete address
        // (or are told to auto-connect) we scan for the DisplayMirror service and use the
        // first/closest advertiser.
        val targetAddress = if (looksLikeBleAddress(address)) {
            address
        } else {
            _connectionState.value = RemoteConnectionState.Scanning
            discoverFirstDeviceAddress() ?: run {
                _connectionState.value = RemoteConnectionState.Error("No DisplayMirror device found over Bluetooth")
                return
            }
        }
        connectedAddress = targetAddress
        _connectionState.value = RemoteConnectionState.Connecting(targetAddress)
        val device = runCatching { bluetoothAdapter.getRemoteDevice(targetAddress) }.getOrNull()
        if (device == null) {
            _connectionState.value = RemoteConnectionState.Error("Invalid BLE address")
            return
        }
        @SuppressLint("MissingPermission")
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDeviceTransportLe)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    override suspend fun disconnect() {
        manualDisconnect = true
        reconnectJob?.cancel()
        disconnectGattOnly()
        _connectionState.value = RemoteConnectionState.Disconnected
    }

    override suspend fun send(command: RemoteCommand, timeoutMs: Long): RemoteResponse {
        if (_connectionState.value !is RemoteConnectionState.Ready && command !is RemoteCommand.Hello) {
            throw IllegalStateException("Not connected and handshaken")
        }
        val queue = commandQueue ?: throw IllegalStateException("Command queue is not ready")
        return queue.send(command, timeoutMs)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = RemoteConnectionState.DiscoveringServices
                serviceDiscoveryRetry = 0
                // A short settle delay before discovery makes the GATT service cache reliable
                // (especially right after a BLE scan), avoiding empty/incomplete discovery results.
                scope.launch {
                    delay(600)
                    discoverServicesSafely(gatt)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                commandQueue = null
                commandCharacteristic = null
                responseCharacteristic = null
                assembler.clear()
                pendingWrite?.completeExceptionally(IllegalStateException("Disconnected"))
                pendingDescriptorWrite?.completeExceptionally(IllegalStateException("Disconnected"))
                this@DisplayMirrorBleClient.gatt?.close()
                this@DisplayMirrorBleClient.gatt = null
                if (!manualDisconnect) {
                    _connectionState.value = RemoteConnectionState.Error("BLE disconnected")
                    scheduleReconnect()
                } else {
                    _connectionState.value = RemoteConnectionState.Disconnected
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failAndDisconnect("Service discovery failed: $status")
                return
            }
            val service = gatt.getService(SERVICE_UUID)
            val command = service?.getCharacteristic(COMMAND_CHAR_UUID)
            val response = service?.getCharacteristic(RESPONSE_CHAR_UUID)
            if (service == null || command == null || response == null) {
                // Android sometimes reports discovery success before the service cache is populated.
                // Retry a couple of times before giving up, rather than dropping the connection.
                if (serviceDiscoveryRetry < MAX_SERVICE_DISCOVERY_RETRIES) {
                    serviceDiscoveryRetry += 1
                    scope.launch {
                        delay(800)
                        discoverServicesSafely(gatt)
                    }
                } else {
                    failAndDisconnect("DisplayMirror BLE service is incomplete")
                }
                return
            }
            serviceDiscoveryRetry = 0
            commandCharacteristic = command
            responseCharacteristic = response
            // Ask for a faster connection interval — helps these flaky head-unit BLE stacks hold
            // the link and move data without stalling.
            runCatching { gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH) }
            if (!gatt.requestMtu(PREFERRED_MTU)) {
                enableNotifications(gatt, service, response)
            } else {
                scope.launch {
                    delay(1_200)
                    if (_connectionState.value is RemoteConnectionState.DiscoveringServices) {
                        enableNotifications(gatt, service, response)
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            negotiatedMtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else DEFAULT_MTU
            // Use the characteristic we already cached in onServicesDiscovered rather than
            // re-looking-up the service (which can transiently return null right after MTU change).
            val response = responseCharacteristic ?: return failAndDisconnect("Response characteristic missing")
            enableNotifications(gatt, response.service, response)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            pendingDescriptorWrite?.let {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    it.complete(Unit)
                } else {
                    it.completeExceptionally(IllegalStateException("Descriptor write failed: $status"))
                }
            }
            pendingDescriptorWrite = null
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            pendingWrite?.let {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    it.complete(Unit)
                } else {
                    it.completeExceptionally(IllegalStateException("Command write failed: $status"))
                }
            }
            pendingWrite = null
        }

        @Deprecated("Deprecated by Android 13 but still called on older devices")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleNotification(characteristic.value ?: ByteArray(0))
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleNotification(value)
        }
    }

    private fun enableNotifications(
        gatt: BluetoothGatt,
        service: BluetoothGattService,
        response: BluetoothGattCharacteristic,
    ) {
        scope.launch {
            runCatching {
                _connectionState.value = RemoteConnectionState.EnablingNotifications
                @SuppressLint("MissingPermission")
                val enabled = gatt.setCharacteristicNotification(response, true)
                if (!enabled) error("Could not enable local notification routing")
                val descriptor = response.getDescriptor(CCCD_UUID)
                    ?: error("Response CCCD descriptor missing")
                val deferred = CompletableDeferred<Unit>()
                pendingDescriptorWrite = deferred
                @SuppressLint("MissingPermission")
                val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                        BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
                if (!started) error("Could not write notification descriptor")
                withTimeout(3_000) { deferred.await() }
                commandQueue = RemoteCommandQueue(::writeFrame, incoming)
                _connectionState.value = RemoteConnectionState.Handshaking
                val hello = commandQueue?.send(RemoteCommand.Hello(pairingCodeProvider()), 5_000)
                if (hello is RemoteResponse.HelloResult && hello.success) {
                    markReadyAndRefresh()
                } else if (hello is RemoteResponse.Error && hello.error == "pairing_code_required") {
                    error("Enter the pairing code shown in DisplayMirror")
                } else if (hello is RemoteResponse.Error && hello.error == "bad_pairing_code") {
                    error("Wrong DisplayMirror pairing code")
                } else if (hello is RemoteResponse.Error && hello.error == "unknown_cmd") {
                    // DisplayMirror can keep the BLE session authenticated across a fast client reconnect.
                    // In that state a second "hello" is routed to the normal command handler and returns
                    // unknown_cmd. A successful status response confirms the session is already usable.
                    val status = commandQueue?.send(RemoteCommand.Status, 5_000)
                    if (status is RemoteResponse.LockState) {
                        markReadyAndRefresh(sendStatus = false)
                    } else {
                        error("Handshake state could not be recovered")
                    }
                } else {
                    error("Handshake rejected by DisplayMirror")
                }
            }.onFailure {
                failAndDisconnect(it.message ?: "Failed to enable notifications")
            }
        }
    }

    private suspend fun markReadyAndRefresh(sendStatus: Boolean = true) {
        reconnectAttempt = 0
        _connectionState.value = RemoteConnectionState.Ready(TransportKind.Ble)
        if (sendStatus) {
            commandQueue?.send(RemoteCommand.Status, 5_000)
        }
    }

    private suspend fun writeFrame(frame: ByteArray) {
        val currentGatt = gatt ?: error("GATT is not connected")
        val characteristic = commandCharacteristic ?: error("Command characteristic missing")
        val chunkSize = (negotiatedMtu - ATT_HEADER_SIZE).coerceAtLeast(20)
        writeMutex.withLock {
            frame.asIterable().chunked(chunkSize).forEach { chunk ->
                writeChunk(currentGatt, characteristic, chunk.toByteArray())
            }
        }
    }

    private suspend fun writeChunk(
        currentGatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        bytes: ByteArray,
    ) {
        val deferred = CompletableDeferred<Unit>()
        pendingWrite = deferred
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        @SuppressLint("MissingPermission")
        val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currentGatt.writeCharacteristic(
                characteristic,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = bytes
            @Suppress("DEPRECATION")
            currentGatt.writeCharacteristic(characteristic)
        }
        if (!started) {
            pendingWrite = null
            error("Bluetooth write was not accepted")
        }
        withTimeout(3_000) { deferred.await() }
    }

    private fun handleNotification(bytes: ByteArray) {
        val frames = assembler.accept(bytes)
        frames.forEach { frame ->
            val response = runCatching { RemoteProtocolCodec.decodeResponse(frame) }.getOrElse {
                RemoteResponse.Error("parse_error", it.message, frame)
            }
            _incoming.tryEmit(response)
        }
    }

    private fun scheduleReconnect() {
        val address = connectedAddress ?: return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = listOf(1_000L, 2_000L, 5_000L, 10_000L)
                .getOrElse(reconnectAttempt.coerceAtLeast(0)) { 10_000L }
            reconnectAttempt += 1
            delay(delayMs)
            if (!manualDisconnect && _connectionState.value !is RemoteConnectionState.Ready) {
                connect(address)
            }
        }
    }

    private fun failAndDisconnect(message: String) {
        _connectionState.value = RemoteConnectionState.Error(message)
        scope.launch { disconnectGattOnly() }
    }

    @SuppressLint("MissingPermission")
    private fun disconnectGattOnly() {
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
        commandCharacteristic = null
        responseCharacteristic = null
        commandQueue = null
        pendingWrite = null
        pendingDescriptorWrite = null
        assembler.clear()
    }

    @SuppressLint("MissingPermission")
    private fun discoverServicesSafely(targetGatt: BluetoothGatt) {
        if (gatt === targetGatt) targetGatt.discoverServices()
    }

    private fun looksLikeBleAddress(address: String): Boolean =
        address != AUTO_ADDRESS && address.count { it == ':' } == 5

    /** Scan for the DisplayMirror BLE service and return the first advertiser's MAC. */
    private suspend fun discoverFirstDeviceAddress(timeoutMs: Long = AUTO_SCAN_TIMEOUT_MS): String? =
        withTimeoutOrNull(timeoutMs) { scanForDevices().firstOrNull()?.address }

    private fun hasBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val AUTO_ADDRESS = "ble-auto"
        private const val AUTO_SCAN_TIMEOUT_MS = 9_000L
        private const val MAX_SERVICE_DISCOVERY_RETRIES = 2
        val SERVICE_UUID: UUID = UUID.fromString("b1c2d3e4-f5a6-7890-abcd-ef1234567890")
        val COMMAND_CHAR_UUID: UUID = UUID.fromString("b1c2d3e4-f5a6-7890-abcd-ef1234567891")
        val RESPONSE_CHAR_UUID: UUID = UUID.fromString("b1c2d3e4-f5a6-7890-abcd-ef1234567892")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val DEFAULT_MTU = 23
        private const val PREFERRED_MTU = 247
        private const val ATT_HEADER_SIZE = 3
        private const val BluetoothDeviceTransportLe = 2
    }
}
