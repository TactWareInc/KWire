package net.tactware.kwire.ktor

import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * Configuration builder for RPC endpoints
 */
class RpcConfigBuilder<T : Any>(private val path: String) {
    lateinit var factory: suspend (RpcConnectionContext) -> T
    lateinit var serviceClass: KClass<T>
    
    var json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    fun serialization(block: JsonBuilder.() -> Unit) {
        json = Json {
            val builder = JsonBuilder().apply(block)
            ignoreUnknownKeys = builder.ignoreUnknownKeys
            isLenient = builder.isLenient
            encodeDefaults = builder.encodeDefaults
            prettyPrint = builder.prettyPrint
        }
    }
    
    inline fun <reified S : T> registerService(noinline factory: suspend (RpcConnectionContext) -> S) {
        this.factory = factory as suspend (RpcConnectionContext) -> T
        this.serviceClass = S::class as KClass<T>
    }
    
    fun build(): RpcEndpointConfig<T> {
        return RpcEndpointConfig(
            path = path,
            serviceClass = serviceClass,
            factory = factory,
            json = json
        )
    }
}