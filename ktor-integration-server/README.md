# KWire Ktor Server Integration

Enhanced Ktor server integration for KWire RPC framework, providing a flexible API for registering multiple RPC services similar to popular RPC libraries like krpc.

## Features

- **Multiple Service Endpoints**: Register different services at different paths
- **Flexible Configuration**: Per-endpoint serialization and WebSocket configuration  
- **Dependency Injection Support**: Easy integration with DI frameworks like Koin
- **Custom Method Dispatchers**: Fine-grained control over method handling
- **WebSocket Transport**: Real-time bidirectional communication
- **CORS Support**: Built-in support for web clients

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("net.tactware.kwire:ktor-integration-server:1.0.2")
}
```

## Quick Start

### Basic Setup

```kotlin
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import net.tactware.kwire.ktor.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        // Install the KWire RPC plugin
        install(KWireRpc) {
            webSocket {
                pingIntervalSeconds = 15
                timeoutSeconds = 60
                maxFrameSize = 1024 * 1024 // 1MB
            }
        }
        
        routing {
            // Register RPC services at different endpoints
            kwireRpc("/device") {
                registerService<DeviceService> { ctx ->
                    DeviceServiceImpl()
                }
            }
            
            kwireRpc("/config") {
                registerService<ConfigurationService> { ctx ->
                    ConfigurationServiceImpl()
                }
            }
        }
    }.start(wait = true)
}
```

### With Dependency Injection (Koin)

```kotlin
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main() {
    // Setup Koin
    val koinModule = module {
        factory<DeviceService> { (ctx: RpcContext) ->
            DeviceServiceImpl()
        }
        factory<ConfigurationService> { (ctx: RpcContext) ->
            ConfigurationServiceImpl()
        }
    }
    
    val koin = startKoin {
        modules(koinModule)
    }.koin
    
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(KWireRpc)
        
        routing {
            kwireRpc("/device") {
                registerService<DeviceService> { ctx ->
                    koin.get { parametersOf(ctx) }
                }
            }
            
            kwireRpc("/config") {
                registerService<ConfigurationService> { ctx ->
                    koin.get { parametersOf(ctx) }
                }
            }
        }
    }.start(wait = true)
}
```

### Advanced Configuration

```kotlin
routing {
    kwireRpc("/api") {
        // Custom JSON serialization for this endpoint
        serialization {
            json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            }
        }
        
        // Custom WebSocket configuration
        webSocket {
            pingIntervalSeconds = 30
            timeoutSeconds = 120
        }
        
        // Enable security
        security(mySecurityManager)
        
        // Enable obfuscation
        obfuscation(myObfuscationManager)
        
        // Register service
        registerService<MyService> { ctx ->
            MyServiceImpl()
        }
    }
}
```

## Service Definition

Define your RPC services using KWire annotations:

```kotlin
import net.tactware.kwire.core.RpcService
import net.tactware.kwire.core.RpcMethod

@RpcService("UserService")
interface UserService {
    @RpcMethod("createUser")
    suspend fun createUser(request: CreateUserRequest): User
    
    @RpcMethod("getUser")
    suspend fun getUser(id: String): User?
    
    @RpcMethod("listUsers")
    suspend fun listUsers(): List<User>
}
```

## Custom Method Dispatcher

For advanced scenarios, you can implement custom method dispatching:

```kotlin
class CustomDispatcher(
    private val service: MyService
) : RpcMethodDispatcher {
    
    override suspend fun dispatch(
        request: RpcRequest,
        context: RpcContext
    ): RpcMessage {
        // Custom method handling with validation, logging, etc.
        return when (request.methodId) {
            "myMethod" -> {
                // Custom logic here
                val result = service.myMethod()
                RpcResponse(
                    messageId = request.messageId,
                    timestamp = System.currentTimeMillis(),
                    result = Json.parseToJsonElement(result)
                )
            }
            else -> {
                RpcError(
                    messageId = request.messageId,
                    timestamp = System.currentTimeMillis(),
                    errorCode = "METHOD_NOT_FOUND",
                    errorMessage = "Unknown method: ${request.methodId}"
                )
            }
        }
    }
    
    override suspend fun dispatchStream(
        streamStart: StreamStart,
        session: WebSocketSession,
        context: RpcContext
    ) {
        // Handle streaming methods
    }
}

// Use the custom dispatcher
routing {
    kwireRpc("/custom") {
        registerServiceWithDispatcher(
            serviceName = "MyService",
            dispatcher = CustomDispatcher(MyServiceImpl())
        )
    }
}
```

## RPC Context

Each service factory receives an `RpcContext` containing:

- `connectionId`: Unique identifier for the connection
- `session`: The WebSocket session
- `path`: The endpoint path
- `securityManager`: Optional security manager
- `obfuscationManager`: Optional obfuscation manager

This context can be used for:
- Connection-specific state management
- Authentication and authorization
- Custom logging and monitoring
- Request/response manipulation

## WebSocket Endpoints

The RPC services are exposed as WebSocket endpoints:

- Device Service: `ws://localhost:8080/device`
- Config Service: `ws://localhost:8080/config`
- Communication Service: `ws://localhost:8080/comms`

## Comparison with krpc

This implementation provides a similar API to krpc:

**krpc style:**
```kotlin
rpc("/device") {
    rpcConfig {
        serialization { json() }
    }
    registerService<DeviceStatus> { ctx ->
        koin.get { parametersOf(ctx) }
    }
}
```

**KWire style:**
```kotlin
kwireRpc("/device") {
    serialization {
        json { /* config */ }
    }
    registerService<DeviceStatus> { ctx ->
        koin.get { parametersOf(ctx) }
    }
}
```

## Features Comparison

| Feature | krpc | KWire |
|---------|------|-------|
| Multiple endpoints | ✅ | ✅ |
| Per-endpoint config | ✅ | ✅ |
| DI integration | ✅ | ✅ |
| WebSocket transport | ❌ | ✅ |
| Streaming support | ✅ | ✅ |
| Custom dispatchers | ❌ | ✅ |
| Security/Obfuscation | ❌ | ✅ |

## Migration Guide

To migrate from krpc to KWire:

1. Replace `rpc` with `kwireRpc`
2. Update configuration syntax (minimal changes)
3. Service registration remains the same
4. Update client code to use WebSocket transport

## Best Practices

1. **Service Organization**: Group related methods in the same service
2. **Error Handling**: Implement proper error handling in service methods
3. **Connection Management**: Use RpcContext for connection-specific state
4. **Security**: Enable security manager for production deployments
5. **Monitoring**: Log important events using the connection ID

## Examples

See `KtorServerExample.kt` for complete examples including:
- Basic server setup
- Multiple service registration
- Dependency injection integration
- Custom method dispatchers
- CORS configuration
- Health check endpoints

## Support

For issues and questions, please visit the [KWire GitHub repository](https://github.com/TactWare/KWire).


