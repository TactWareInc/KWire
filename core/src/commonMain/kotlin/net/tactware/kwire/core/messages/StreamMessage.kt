package net.tactware.kwire.core.messages

import kotlinx.serialization.Serializable

/**
 * Stream-specific messages for Flow handling.
 */
@Serializable
sealed class StreamMessage : RpcMessage()