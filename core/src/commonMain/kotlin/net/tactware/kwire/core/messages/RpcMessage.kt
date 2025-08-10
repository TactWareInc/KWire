package net.tactware.kwire.core.messages

import kotlinx.serialization.Serializable

/**
 * Base class for all RPC messages.
 */
@Serializable
sealed class RpcMessage {
    abstract val messageId: String
    abstract val timestamp: Long
}
