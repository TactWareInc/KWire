package net.tactware.kwire.ktor

import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * Data class to hold RPC endpoint configuration
 */
data class RpcEndpointConfig<T : Any>(
    val path: String,
    val serviceClass: KClass<T>,
    val factory: suspend (RpcConnectionContext) -> T,
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
)