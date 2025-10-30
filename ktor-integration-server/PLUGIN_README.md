0# KWire RPC Ktor Plugin

A native Ktor plugin for integrating KWire RPC services into your Ktor application.

## Installation

```kotlin
install(KWireRpc) {
    // Configuration here
}
```

## Features

- ✅ **Native Ktor Plugin**: Follows Ktor's plugin architecture
- ✅ **Multiple Services**: Register multiple RPC services at different endpoints
- ✅ **WebSocket Transport**: Built on Ktor's WebSocket support
- ✅ **Type-Safe**: Full Kotlin type safety with generics
- ✅ **Generated Server Support**: Works with gradle plugin generated servers
- ✅ **Flexible Configuration**: Per-service and global configuration options

## Quick Start

```kotlin
fun Application.module() {
    install(KWireRpc) {
        // Register a service
        service<UserService>("/users") {
            implementation { UserServiceImpl() }
            withGeneratedServer { transport, impl ->
                UserServiceServerImpl(transport, impl)
            }
        }
    }
}
```

## Configuration Options

### Global Configuration

```kotlin
install(KWireRpc) {
    // WebSocket configuration
    webSocket {
        pingPeriod = 15.seconds
        timeout = 60.seconds
        maxFrameSize = 1024 * 1024 // 1MB
    }
    
    // JSON serialization
    serialization {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }
}
```

### Service Registration

```kotlin
service<MyService>("/api/service") {
    // Provide implementation
    implementation { MyServiceImpl() }
    
    // Use generated server (recommended)
    withGeneratedServer { transport, impl ->
        MyServiceServerImpl(transport, impl)
    }
    
    // Optional: Authentication requirement
    requireAuth = true
    
    // Optional: Custom configuration
    customConfig = MyCustomConfig()
}
```

## Complete Example

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080) {
        // Install the plugin
        install(KWireRpc) {
            // Global WebSocket config
            webSocket {
                pingPeriod = 30.seconds
                timeout = 120.seconds
            }
            
            // User service (public)
            service<UserService>("/api/users") {
                implementation { UserServiceImpl() }
                withGeneratedServer { transport, impl ->
                    UserServiceServerImpl(
                        transport = transport,
                        implementation = impl,
                        timeProvider = { System.currentTimeMillis() }
                    )
                }
            }
            
            // Admin service (protected)
            service<AdminService>("/api/admin") {
                implementation { AdminServiceImpl() }
                requireAuth = true
                withGeneratedServer { transport, impl ->
                    AdminServiceServerImpl(transport, impl)
                }
            }
        }
        
        // Other Ktor features
        install(CORS) { /* ... */ }
        
        routing {
            get("/health") {
                call.respondText("OK")
            }
        }
    }.start(wait = true)
}
```

## Integration with Dependency Injection

### Koin Example

```kotlin
val koin = startKoin {
    modules(serviceModule)
}.koin

install(KWireRpc) {
    service<UserService>("/users") {
        implementation { koin.get() }
        withGeneratedServer { transport, impl ->
            UserServiceServerImpl(transport, impl)
        }
    }
}
```

### Manual DI Container

```kotlin
class ServiceContainer {
    fun getUserService(): UserService = UserServiceImpl()
    fun getAdminService(): AdminService = AdminServiceImpl()
}

val container = ServiceContainer()

install(KWireRpc) {
    service<UserService>("/users") {
        implementation { container.getUserService() }
        withGeneratedServer { transport, impl ->
            UserServiceServerImpl(transport, impl)
        }
    }
}
```

## Architecture

The plugin handles:
1. **WebSocket Setup**: Automatically configures WebSocket support
2. **Service Registration**: Maps paths to service implementations
3. **Transport Management**: Creates and manages `WebSocketSessionTransport` instances
4. **Server Lifecycle**: Starts and stops generated servers
5. **Connection Management**: Handles connection lifecycle and cleanup

## Benefits Over Manual Setup

### Before (Manual Setup)
```kotlin
routing {
    webSocket("/users") {
        val transport = WebSocketSessionTransport(this)
        val serverImpl = UserServiceServerImpl(
            transport = transport,
            implementation = UserServiceImpl()
        )
        serverImpl.start()
        transport.handleIncomingFrames()
        serverImpl.stop()
    }
}
```

### After (With Plugin)
```kotlin
install(KWireRpc) {
    service<UserService>("/users") {
        implementation { UserServiceImpl() }
        withGeneratedServer { transport, impl ->
            UserServiceServerImpl(transport, impl)
        }
    }
}
```

## Advanced Features

### Custom Method Dispatchers

```kotlin
service<MyService>("/api/service") {
    implementation { MyServiceImpl() }
    
    // Use custom dispatcher instead of generated server
    customDispatcher { request, context ->
        // Custom method routing logic
    }
}
```

### Service Middleware

```kotlin
service<MyService>("/api/service") {
    implementation { MyServiceImpl() }
    
    // Add middleware
    beforeRequest { request ->
        // Logging, validation, etc.
    }
    
    afterResponse { response ->
        // Metrics, cleanup, etc.
    }
}
```

## Comparison with Other Approaches

| Feature | Manual WebSocket | rpc<T> Function | KWireRpc Plugin |
|---------|-----------------|-----------------|-----------------|
| Setup Complexity | High | Medium | Low |
| Ktor Integration | Manual | Partial | Native |
| Configuration | Per-endpoint | Per-endpoint | Global + Per-endpoint |
| Type Safety | Yes | Yes | Yes |
| DI Support | Manual | Manual | Built-in patterns |
| Lifecycle Management | Manual | Partial | Automatic |

## Migration Guide

### From Manual WebSocket Setup

Replace manual WebSocket routes with service registration in the plugin.

### From rpc<T> Function

Replace:
```kotlin
rpc<UserService>("/users") { ctx ->
    // Implementation
}
```

With:
```kotlin
install(KWireRpc) {
    service<UserService>("/users") {
        implementation { /* ... */ }
        withGeneratedServer { /* ... */ }
    }
}
```

## Future Enhancements

- [ ] Automatic method dispatch without generated servers
- [ ] Built-in authentication/authorization
- [ ] Request/response interceptors
- [ ] Metrics and monitoring
- [ ] Service discovery
- [ ] Load balancing for multiple instances
- [ ] Circuit breaker pattern
- [ ] Rate limiting

## Support

For issues and questions, visit the [KWire GitHub repository](https://github.com/TactWare/KWire).


