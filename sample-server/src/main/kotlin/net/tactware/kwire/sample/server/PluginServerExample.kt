package net.tactware.kwire.sample.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tactware.kwire.ktor.KWireRpc
import net.tactware.kwire.ktor.withGeneratedServer
import net.tactware.kwire.sample.api.UserService
import kotlin.time.Duration.Companion.seconds

/**
 * Example server using the KWireRpc Ktor Plugin.
 * 
 * This shows how clean and simple the API becomes when using
 * the plugin pattern that's native to Ktor.
 */
object PluginServerExample {
    @JvmStatic
    fun main(args: Array<String> = emptyArray()) {
        startServer()
    }
}

private fun startServer() {
    embeddedServer(
        Netty,
        port = 8082,
        host = "0.0.0.0",
        module = {
            // Install the KWire RPC plugin
            install(KWireRpc) {
                // Configure WebSocket settings globally
                webSocket {
                    pingPeriod = 15.seconds
                    timeout = 60.seconds
                    maxFrameSize = 1024 * 1024
                }
                
                // Configure JSON serialization
                serialization {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                    prettyPrint = false
                }
                
                // Register the UserService
                service<UserService>("/users") {
                    // Provide the implementation
                    implementation { UserServiceImpl() }
                    
                    // Configure to use the generated server
                    withGeneratedServer { transport, impl ->
                        UserServiceServerImpl(
                            transport = transport,
                            implementation = impl,
                            timeProvider = { System.currentTimeMillis() }
                        )
                    }
                }
            }
            
            // Configure CORS
            install(CORS) {
                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Delete)
                allowMethod(HttpMethod.Patch)
                allowHeader(HttpHeaders.Authorization)
                allowHeader(HttpHeaders.AccessControlAllowOrigin)
                allowHeader(HttpHeaders.Upgrade)
                allowNonSimpleContentTypes = true
                allowCredentials = true
                allowSameOrigin = true
            }
            
            // Add regular HTTP routes
            routing {
                get("/health") {
                    call.respondText(status = HttpStatusCode.OK) { 
                        "KWire RPC Server is Running" 
                    }
                }
                
                get("/") {
                    call.respondText { 
                        """
                        KWire RPC Server
                        ================
                        Available endpoints:
                        - WebSocket: ws://localhost:8082/users
                        - Health: http://localhost:8082/health
                        """.trimIndent()
                    }
                }
            }
        }
    ).start(wait = true)
    
    println("=" * 60)
    println("KWire RPC Server with Plugin Started")
    println("=" * 60)
    println("WebSocket endpoint: ws://0.0.0.0:8082/users")
    println("Health check: http://0.0.0.0:8082/health")
    println("=" * 60)
}

/**
 * Alternative configuration showing more options
 */
fun alternativeConfiguration() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        // Minimal configuration
        install(KWireRpc) {
            service<UserService>("/api/users") {
                implementation { UserServiceImpl() }
                withGeneratedServer { transport, impl ->
                    UserServiceServerImpl(transport, impl, timeProvider = { System.currentTimeMillis() })
                }
            }
        }
    }.start(wait = true)
}

/**
 * Production-style configuration with dependency injection
 */
fun productionConfiguration() {
    // Simulated DI container
    class ServiceContainer {
        fun getUserService(): UserService = UserServiceImpl()
        // fun getAdminService(): AdminService = AdminServiceImpl()
        // fun getMetricsService(): MetricsService = MetricsServiceImpl()
    }
    
    val container = ServiceContainer()
    
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(KWireRpc) {
            // Production WebSocket settings
            webSocket {
                pingPeriod = 30.seconds
                timeout = 120.seconds
                maxFrameSize = 2 * 1024 * 1024 // 2MB
            }
            
            // User service
            service<UserService>("/api/v1/users") {
                implementation { container.getUserService() }
                requireAuth = false // Public endpoint
                withGeneratedServer { transport, impl ->
                    UserServiceServerImpl(
                        transport = transport,
                        implementation = impl,
                        timeProvider = { System.currentTimeMillis() }
                    )
                }
            }
            
            // Admin service (would require auth)
            // service<AdminService>("/api/v1/admin") {
            //     implementation { container.getAdminService() }
            //     requireAuth = true // Protected endpoint
            //     withGeneratedServer { transport, impl ->
            //         AdminServiceServerImpl(transport, impl)
            //     }
            // }
        }
        
        // Add authentication, monitoring, etc.
        // install(Authentication) { ... }
        // install(CallLogging) { ... }
        // install(ContentNegotiation) { ... }
        
    }.start(wait = true)
}

// Extension for string multiplication
private operator fun String.times(n: Int): String = repeat(n)
