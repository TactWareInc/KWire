package net.tactware.kwire.core.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.serialization.json.JsonElement

/**
 * Stream error message.
 */
@Serializable
data class StreamError(
    override val messageId: String,
    override val timestamp: Long,
    val streamId: String,
    val errorCode: String,
    val errorMessage: String,
    val errorDetails: @Contextual JsonElement? = null
) : net.tactware.kwire.core.messages.StreamMessage()