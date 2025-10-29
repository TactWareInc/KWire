package net.tactware.kwire.ktor.plugin

import net.tactware.kwire.core.RpcTransport
import kotlin.reflect.KClass

/**
 * Service configuration
 */
class ServiceConfiguration<T : Any>(
    val path: String,
    val serviceClass: KClass<T>
) {
    internal lateinit var implementationFactory: () -> T
    internal var serverFactory: ((RpcTransport, T) -> Any)? = null
    var useGeneratedServer: Boolean = false
    var requireAuth: Boolean = false
    var customConfig: Any? = null
    
    /**
     * Set the implementation factory
     */
    fun implementation(factory: () -> T) {
        implementationFactory = factory
    }
    
    /**
     * Set the server factory for generated servers
     */
    fun generatedServer(factory: (RpcTransport, T) -> Any) {
        serverFactory = factory
        useGeneratedServer = true
    }
}
