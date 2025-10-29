package net.tactware.kwire.ktor

import io.ktor.websocket.WebSocketSession

/**
 * Context provided to service factories
 */
data class RpcConnectionContext(
    val connectionId: String,
    val path: String,
    val session: WebSocketSession,
    val remoteAddress: String? = null
)