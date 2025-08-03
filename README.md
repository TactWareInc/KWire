# Kotlin Obfuscated RPC Library

A comprehensive Kotlin RPC library built with Ktor that provides built-in obfuscation support, Flow handling, and powerful code generation capabilities.

## Features

- **Obfuscation-First Design**: Built-in support for code obfuscation without breaking RPC functionality
- **Flow-Native**: Comprehensive support for Kotlin Flow streaming with proper serialization
- **Gradle Integration**: Powerful plugin for automatic code generation and configuration
- **Ktor Foundation**: Leverages Ktor's robust networking and plugin architecture
- **Multiplatform**: Supports JVM, JS, WebAssembly, and Native targets

## Modules

- **core**: Core RPC abstractions and protocols
- **runtime**: Runtime components and method mapping
- **serialization**: Flow and data serialization handling
- **ktor-integration**: Ktor server and client integration
- **gradle-plugin**: Code generation and build integration
- **obfuscation-support**: Obfuscation utilities and mapping
- **samples**: Example applications and demos

## Quick Start

### 1. Add the plugin to your build.gradle.kts:

```kotlin
plugins {
    id("com.obfuscated.rpc.plugin") version "1.0.0-SNAPSHOT"
}
```

### 2. Define your RPC service:

```kotlin
@ObfuscatedRpc
interface UserService {
    suspend fun getUser(id: String): User
    fun getUserUpdates(id: String): Flow<UserUpdate>
}
```

### 3. Implement the service:

```kotlin
class UserServiceImpl : UserService {
    override suspend fun getUser(id: String): User {
        // Implementation
    }
    
    override fun getUserUpdates(id: String): Flow<UserUpdate> = flow {
        // Implementation
    }
}
```

### 4. Set up the server:

```kotlin
fun main() {
    embeddedServer(Netty, 8080) {
        configureObfuscatedRpc()
        routing {
            obfuscatedRpc("/api") {
                registerService<UserService> { UserServiceImpl() }
            }
        }
    }.start(wait = true)
}
```

### 5. Create a client:

```kotlin
val client = HttpClient(CIO) {
    install(ObfuscatedRpcClient)
}

val rpcClient = client.obfuscatedRpc {
    url("ws://localhost:8080/api")
}

val userService = rpcClient.createService<UserService>()
```

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```

## License

MIT License - see LICENSE file for details.

