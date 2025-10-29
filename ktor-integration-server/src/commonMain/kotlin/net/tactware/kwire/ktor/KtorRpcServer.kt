package net.tactware.kwire.ktor

import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import net.tactware.kwire.core.RpcErrorCodes
import net.tactware.kwire.core.messages.RpcError
import net.tactware.kwire.core.messages.RpcMessage
import net.tactware.kwire.core.messages.RpcRequest

/**
 * Ktor-specific RPC server implementation.
 */
class KtorRpcServer(
    private val config: KtorRpcConfig
) {
    private val transport = KtorServerRpcTransport(config)
    private val serviceHandlers = mutableMapOf<String, suspend (String, List<JsonElement>) -> JsonElement>()
    
    fun registerService(
        serviceName: String,
        handler: suspend (String, List<JsonElement>) -> JsonElement
    ) {
        serviceHandlers[serviceName] = handler
    }
    
    suspend fun handleRequest(requestJson: String): String {
        return try {
            val request = Json.decodeFromString<RpcRequest>(requestJson)
            
            // Connect if not already connected
            if (!transport.isConnected) {
                transport.connect()
            }
            
            // Send request through transport
            transport.send(request)
            
            // Collect response
            var response: RpcMessage? = null
            transport.receive().collect { message ->
                if (message.messageId == request.messageId) {
                    response = message
                    return@collect
                }
            }
            
            Json.encodeToString(response ?: RpcError(
                messageId = request.messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorCode = RpcErrorCodes.INTERNAL_ERROR,
                errorMessage = "No response generated"
            )
            )
            
        } catch (e: Exception) {
            val errorResponse = RpcError(
                messageId = "unknown",
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorCode = RpcErrorCodes.INTERNAL_ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
            Json.encodeToString(errorResponse)
        }
    }
}