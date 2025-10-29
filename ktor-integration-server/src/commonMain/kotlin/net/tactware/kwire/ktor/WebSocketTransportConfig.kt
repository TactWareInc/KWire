package net.tactware.kwire.ktor

import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json

/**
 * Configuration for WebSocket transport
 */
data class WebSocketTransportConfig(
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    },
    val bufferSize: Int = Channel.UNLIMITED
)
