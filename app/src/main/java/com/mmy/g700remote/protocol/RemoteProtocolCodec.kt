package com.mmy.g700remote.protocol

import java.nio.charset.StandardCharsets

object RemoteProtocolCodec {
    const val PROTOCOL_VERSION = 4

    fun encodeCommand(command: RemoteCommand): String = command.toJson().toString()

    fun encodeFrame(command: RemoteCommand): ByteArray =
        (encodeCommand(command) + "\n").toByteArray(StandardCharsets.UTF_8)

    fun decodeResponse(frame: String): RemoteResponse =
        RemoteResponse.fromJson(frame.trim())

    fun responseMatches(command: RemoteCommand, response: RemoteResponse): Boolean =
        response.type in command.expectedResponseTypes || response is RemoteResponse.Error
}

class NewlineFrameAssembler {
    private val buffer = ArrayList<Byte>()

    fun accept(bytes: ByteArray): List<String> {
        val frames = mutableListOf<String>()
        bytes.forEach { byte ->
            if (byte == '\n'.code.toByte()) {
                val frame = String(buffer.toByteArray(), StandardCharsets.UTF_8).trim()
                buffer.clear()
                if (frame.isNotEmpty()) {
                    frames += frame
                }
            } else {
                buffer += byte
            }
        }
        return frames
    }

    fun clear() {
        buffer.clear()
    }
}
