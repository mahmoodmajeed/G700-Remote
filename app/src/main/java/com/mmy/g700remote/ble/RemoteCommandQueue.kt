package com.mmy.g700remote.ble

import com.mmy.g700remote.protocol.RemoteCommand
import com.mmy.g700remote.protocol.RemoteProtocolCodec
import com.mmy.g700remote.protocol.RemoteResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class RemoteCommandQueue(
    private val writeFrame: suspend (ByteArray) -> Unit,
    private val responses: Flow<RemoteResponse>,
) {
    private val mutex = Mutex()

    suspend fun send(command: RemoteCommand, timeoutMs: Long = 5_000): RemoteResponse =
        mutex.withLock {
            coroutineScope {
                val response = async {
                    withTimeout(timeoutMs) {
                        responses.first { RemoteProtocolCodec.responseMatches(command, it) }
                    }
                }
                try {
                    writeFrame(RemoteProtocolCodec.encodeFrame(command))
                    response.await()
                } catch (throwable: Throwable) {
                    response.cancel()
                    throw throwable
                }
            }
        }
}
