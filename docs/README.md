# Kotlin Obfuscated RPC Library

A comprehensive Kotlin RPC library built with Ktor that provides advanced obfuscation support, Flow handling, and kotlinx serialization compatibility. This library is designed for developers who need secure, high-performance RPC communication with the ability to obfuscate method names and service interfaces to protect against reverse engineering.

## Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Core Concepts](#core-concepts)
- [Basic Usage](#basic-usage)
- [Obfuscation Support](#obfuscation-support)
- [Flow and Streaming](#flow-and-streaming)
- [Security Features](#security-features)
- [Gradle Plugin](#gradle-plugin)
- [Advanced Configuration](#advanced-configuration)
- [Examples](#examples)
- [API Reference](#api-reference)
- [Performance](#performance)
- [Contributing](#contributing)
- [License](#license)

## Features

### Core RPC Functionality
- **Type-Safe RPC Calls**: Leverage Kotlin's type system for compile-time safety
- **Multiplatform Support**: Works on JVM, JavaScript, and Native platforms
- **Ktor Integration**: Built on top of Ktor for robust HTTP communication
- **kotlinx.serialization**: Full compatibility with kotlinx.serialization for data transfer

### Advanced Obfuscation
- **Method Name Obfuscation**: Configurable strategies for hiding method names
- **Service Name Obfuscation**: Optional obfuscation of service interface names
- **ProGuard Integration**: Seamless integration with ProGuard obfuscation
- **Runtime Mapping**: Dynamic resolution of obfuscated names at runtime
- **Multiple Strategies**: Random, hash-based, and sequential obfuscation options

### Flow and Streaming Support
- **Native Flow Support**: First-class support for Kotlin Flow types
- **Backpressure Handling**: Built-in flow control and backpressure management
- **Stream Serialization**: Automatic serialization of Flow elements
- **Error Recovery**: Robust error handling for streaming operations

### Security and Authentication
- **Multiple Auth Methods**: Token, API key, and custom authentication
- **Session Management**: Secure session handling with configurable timeouts
- **Rate Limiting**: Built-in rate limiting to prevent abuse
- **Authorization**: Role-based access control for RPC methods

### Developer Experience
- **Gradle Plugin**: Automatic code generation for client and server stubs
- **Comprehensive Testing**: Unit tests, integration tests, and benchmarks
- **Rich Documentation**: Detailed guides and API reference
- **Performance Optimized**: Minimal overhead and high throughput



## Quick Start

Get up and running with the Kotlin Obfuscated RPC library in just a few minutes. This quick start guide will walk you through creating a simple RPC service with obfuscation enabled.

### 1. Add Dependencies

Add the library to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.obfuscated.rpc:core:1.0.0")
    implementation("com.obfuscated.rpc:runtime:1.0.0")
    implementation("com.obfuscated.rpc:ktor-integration:1.0.0")
    implementation("com.obfuscated.rpc:obfuscation-support:1.0.0")
}

plugins {
    id("com.obfuscated.rpc.gradle-plugin") version "1.0.0"
}
```

### 2. Define Your Service

Create a service interface with RPC annotations:

```kotlin
@RpcService
interface GreetingService {
    @RpcMethod
    suspend fun greet(name: String): String
    
    @RpcMethod
    suspend fun getGreetings(count: Int): Flow<String>
}
```

### 3. Implement the Service

Provide an implementation of your service:

```kotlin
class GreetingServiceImpl : GreetingService {
    override suspend fun greet(name: String): String {
        return "Hello, $name! Welcome to Obfuscated RPC."
    }
    
    override suspend fun getGreetings(count: Int): Flow<String> = flow {
        repeat(count) { i ->
            emit("Greeting #${i + 1}")
            delay(100)
        }
    }
}
```

### 4. Set Up the Server

Create and configure an RPC server:

```kotlin
fun main() {
    val obfuscationManager = ObfuscationUtils.createDefaultManager()
    val securityManager = SecurityUtils.createDefaultManager()
    
    val server = KtorRpcUtils.createServer(
        enableObfuscation = true,
        enableSecurity = true
    )
    
    server.registerService(GreetingService::class, GreetingServiceImpl())
    
    // Start server (implementation depends on your Ktor setup)
    println("RPC Server started with obfuscation enabled")
}
```

### 5. Create a Client

Connect to your RPC server from a client:

```kotlin
suspend fun main() {
    val client = KtorRpcUtils.createClient(
        baseUrl = "http://localhost:8080",
        enableObfuscation = true,
        enableSecurity = true
    )
    
    // Make RPC calls
    val greeting = client.call<String>("GreetingService", "greet", listOf("World"))
    println(greeting)
    
    // Use streaming
    client.stream<String>("GreetingService", "getGreetings", listOf(3))
        .collect { greeting ->
            println("Received: $greeting")
        }
}
```

That's it! You now have a working RPC system with obfuscation and streaming support. The method names and service names will be automatically obfuscated in the network communication, providing protection against reverse engineering while maintaining full functionality.

## Installation

The Kotlin Obfuscated RPC library is distributed as a set of modules that you can include in your project based on your needs. Each module serves a specific purpose and can be used independently or together for full functionality.

### Module Overview

| Module | Purpose | Required |
|--------|---------|----------|
| `core` | Core RPC protocol and interfaces | Yes |
| `runtime` | Client and server runtime implementations | Yes |
| `serialization` | Flow and kotlinx.serialization support | Recommended |
| `obfuscation-support` | Obfuscation and security features | Optional |
| `ktor-integration` | Ktor transport implementation | Recommended |
| `gradle-plugin` | Code generation plugin | Optional |

### Gradle Configuration

Add the following to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    // Add your repository if publishing locally
}

dependencies {
    // Core modules (required)
    implementation("com.obfuscated.rpc:core:1.0.0")
    implementation("com.obfuscated.rpc:runtime:1.0.0")
    
    // Recommended modules
    implementation("com.obfuscated.rpc:serialization:1.0.0")
    implementation("com.obfuscated.rpc:ktor-integration:1.0.0")
    
    // Optional modules
    implementation("com.obfuscated.rpc:obfuscation-support:1.0.0")
    
    // Ktor dependencies (if using ktor-integration)
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    
    // kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

plugins {
    kotlin("multiplatform") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    
    // Add the RPC plugin for code generation
    id("com.obfuscated.rpc.gradle-plugin") version "1.0.0"
}
```

### Multiplatform Setup

For multiplatform projects, configure your source sets:

```kotlin
kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.obfuscated.rpc:core:1.0.0")
                implementation("com.obfuscated.rpc:runtime:1.0.0")
                implementation("com.obfuscated.rpc:serialization:1.0.0")
                implementation("com.obfuscated.rpc:obfuscation-support:1.0.0")
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation("com.obfuscated.rpc:ktor-integration:1.0.0")
                implementation("io.ktor:ktor-server-netty:2.3.7")
            }
        }
        
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:2.3.7")
            }
        }
    }
}
```

### Version Compatibility

| Library Version | Kotlin Version | Ktor Version | Serialization Version |
|----------------|----------------|--------------|----------------------|
| 1.0.0 | 1.9.21+ | 2.3.7+ | 1.6.2+ |

### Building from Source

If you prefer to build the library from source:

```bash
git clone https://github.com/your-org/kotlin-obfuscated-rpc.git
cd kotlin-obfuscated-rpc
./gradlew build publishToMavenLocal
```

Then use `1.0.0-SNAPSHOT` as the version in your dependencies.



## Core Concepts

Understanding the core concepts of the Kotlin Obfuscated RPC library is essential for effective usage. This section covers the fundamental architecture, design principles, and key components that make up the library.

### RPC Architecture

The library follows a layered architecture designed for flexibility, security, and performance. At its core, it implements a message-based RPC protocol that can work over various transport mechanisms, with Ktor HTTP being the primary implementation.

The architecture consists of several key layers:

**Protocol Layer**: Defines the core message types and communication patterns. This includes request/response messages, streaming messages, and error handling. All messages are serializable using kotlinx.serialization and support contextual serialization for complex types.

**Transport Layer**: Handles the actual network communication between client and server. The transport layer is abstracted to allow for different implementations, though Ktor HTTP is the primary transport provided. This layer manages connection lifecycle, message routing, and network error handling.

**Serialization Layer**: Manages the conversion between Kotlin objects and their serialized representations. This layer provides special handling for Flow types, enabling streaming RPC calls with proper backpressure support. It integrates deeply with kotlinx.serialization to provide type-safe serialization.

**Obfuscation Layer**: Provides method and service name obfuscation capabilities. This layer can transform method calls at runtime, mapping between original method names and obfuscated identifiers. It supports multiple obfuscation strategies and integrates with ProGuard for additional protection.

**Security Layer**: Implements authentication, authorization, and rate limiting. This layer provides session management, role-based access control, and configurable security policies. It can be enabled or disabled based on your security requirements.

### Message Protocol

The RPC protocol is built around a set of core message types that handle different aspects of communication:

**RpcRequest**: Represents a method call from client to server. Contains the service name, method identifier, parameters, and metadata. Supports both regular and streaming method calls.

**RpcResponse**: Contains the result of a successful method call. For streaming methods, this may be followed by additional stream messages. Includes timing information and metadata about the response.

**RpcError**: Represents an error condition during method execution. Includes structured error codes, human-readable messages, and optional detailed error information for debugging.

**Stream Messages**: A family of messages (StreamStart, StreamData, StreamEnd, StreamError) that handle Flow-based streaming operations. These messages maintain stream state and provide proper flow control.

### Service Definition

Services are defined using Kotlin interfaces annotated with `@RpcService`. Methods within these interfaces are marked with `@RpcMethod` to indicate they should be exposed via RPC. This annotation-based approach provides clear separation between regular interface methods and RPC-exposed methods.

The library supports both suspend functions for asynchronous operations and Flow return types for streaming data. All parameters and return types must be serializable using kotlinx.serialization, which provides compile-time safety and runtime efficiency.

Service implementations are regular Kotlin classes that implement the service interfaces. The library handles the marshalling and unmarshalling of parameters and return values automatically, allowing you to focus on business logic rather than serialization concerns.

### Client-Server Model

The library implements a traditional client-server model with some modern enhancements:

**Server Side**: Servers register service implementations and handle incoming RPC requests. The server runtime manages request routing, method dispatch, and response generation. It supports concurrent request handling and provides hooks for middleware integration.

**Client Side**: Clients create proxy objects that implement service interfaces. Method calls on these proxies are automatically converted to RPC requests and sent to the server. The client runtime handles response processing and error handling transparently.

**Bidirectional Communication**: While following a client-server model, the library supports bidirectional streaming through Flow types. This allows for real-time data exchange and reactive programming patterns.

### Obfuscation Model

The obfuscation system is designed to protect your RPC interfaces from reverse engineering while maintaining full functionality:

**Method Mapping**: Original method names are mapped to obfuscated identifiers using configurable strategies. These mappings are generated at build time and can be exported for deployment.

**Runtime Resolution**: The obfuscation manager handles the translation between original and obfuscated names at runtime. This process is transparent to your application code.

**ProGuard Integration**: The library provides utilities for generating ProGuard keep rules and integrating with existing obfuscation workflows. This ensures compatibility with standard Android and JVM obfuscation practices.

### Flow Integration

Kotlin Flow integration is a first-class feature of the library, designed to provide seamless streaming capabilities:

**Automatic Serialization**: Flow elements are automatically serialized and deserialized using the configured serialization strategy. This works with any serializable type, including complex data structures.

**Backpressure Support**: The streaming implementation includes proper backpressure handling to prevent memory issues and ensure smooth data flow between client and server.

**Error Propagation**: Errors in Flow streams are properly propagated across the network boundary, maintaining the error semantics of Kotlin Flow.

**Stream Lifecycle**: The library manages the complete lifecycle of streaming operations, including proper cleanup and resource management.

### Security Model

The security model is designed to be flexible while providing strong default protections:

**Authentication**: Multiple authentication methods are supported, including token-based, API key, and custom authentication schemes. Authentication state is maintained through secure session management.

**Authorization**: Role-based access control allows fine-grained control over which clients can access which methods. Authorization policies can be configured per service or per method.

**Rate Limiting**: Built-in rate limiting prevents abuse and ensures fair resource usage. Rate limits can be configured globally or per client.

**Session Management**: Secure session handling with configurable timeouts and automatic cleanup of expired sessions.

This architecture provides a solid foundation for building secure, high-performance RPC systems while maintaining the flexibility to adapt to different use cases and requirements.


## Basic Usage

This section provides detailed examples and explanations for common usage patterns of the Kotlin Obfuscated RPC library. We'll start with simple examples and gradually introduce more advanced features.

### Defining RPC Services

The foundation of any RPC system is the service definition. Services are defined as Kotlin interfaces using specific annotations that indicate which methods should be exposed via RPC.

```kotlin
import com.obfuscated.rpc.core.RpcService
import com.obfuscated.rpc.core.RpcMethod
import com.obfuscated.rpc.serialization.RpcSerializable
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@RpcService
interface UserService {
    @RpcMethod
    suspend fun createUser(user: CreateUserRequest): User
    
    @RpcMethod
    suspend fun getUserById(userId: String): User?
    
    @RpcMethod
    suspend fun updateUser(userId: String, updates: UserUpdates): Boolean
    
    @RpcMethod
    suspend fun deleteUser(userId: String): Boolean
    
    @RpcMethod
    suspend fun searchUsers(query: String): List<User>
    
    @RpcMethod
    suspend fun getUsersStream(batchSize: Int): Flow<User>
}

@Serializable
@RpcSerializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: Long
)

@Serializable
@RpcSerializable
data class CreateUserRequest(
    val name: String,
    val email: String
)

@Serializable
@RpcSerializable
data class UserUpdates(
    val name: String? = null,
    val email: String? = null
)
```

The `@RpcService` annotation marks an interface as an RPC service. All methods that should be exposed via RPC must be marked with `@RpcMethod`. Methods can be regular suspend functions or return Flow types for streaming operations.

All data classes used as parameters or return types must be annotated with both `@Serializable` (for kotlinx.serialization) and `@RpcSerializable` (for RPC-specific serialization handling).

### Implementing Services

Service implementations are regular Kotlin classes that implement the service interfaces:

```kotlin
class UserServiceImpl : UserService {
    private val users = mutableMapOf<String, User>()
    private val userIdCounter = AtomicInteger(0)
    
    override suspend fun createUser(user: CreateUserRequest): User {
        val userId = "user_${userIdCounter.incrementAndGet()}"
        val newUser = User(
            id = userId,
            name = user.name,
            email = user.email,
            createdAt = System.currentTimeMillis()
        )
        users[userId] = newUser
        return newUser
    }
    
    override suspend fun getUserById(userId: String): User? {
        return users[userId]
    }
    
    override suspend fun updateUser(userId: String, updates: UserUpdates): Boolean {
        val existingUser = users[userId] ?: return false
        
        val updatedUser = existingUser.copy(
            name = updates.name ?: existingUser.name,
            email = updates.email ?: existingUser.email
        )
        
        users[userId] = updatedUser
        return true
    }
    
    override suspend fun deleteUser(userId: String): Boolean {
        return users.remove(userId) != null
    }
    
    override suspend fun searchUsers(query: String): List<User> {
        return users.values.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
            user.email.contains(query, ignoreCase = true)
        }
    }
    
    override suspend fun getUsersStream(batchSize: Int): Flow<User> = flow {
        users.values.chunked(batchSize).forEach { batch ->
            batch.forEach { user ->
                emit(user)
                delay(50) // Simulate processing delay
            }
        }
    }
}
```

Service implementations can use any Kotlin features, including coroutines, collections, and external libraries. The RPC runtime handles all the serialization and network communication automatically.

### Setting Up the Server

Creating an RPC server involves configuring the runtime and registering your service implementations:

```kotlin
import com.obfuscated.rpc.runtime.RpcServer
import com.obfuscated.rpc.ktor.KtorRpcServer
import com.obfuscated.rpc.ktor.KtorRpcConfig
import com.obfuscated.rpc.obfuscation.ObfuscationManager
import com.obfuscated.rpc.obfuscation.ObfuscationConfig
import com.obfuscated.rpc.obfuscation.SecurityManager
import com.obfuscated.rpc.obfuscation.SecurityConfig

suspend fun startRpcServer() {
    // Configure obfuscation
    val obfuscationConfig = ObfuscationConfig(
        enabled = true,
        strategy = ObfuscationStrategy.HASH_BASED,
        methodNameLength = 8
    )
    val obfuscationManager = ObfuscationManager(obfuscationConfig)
    
    // Configure security
    val securityConfig = SecurityConfig(
        authMethod = AuthMethod.TOKEN,
        enableRateLimit = true
    )
    val securityManager = SecurityManager(securityConfig)
    
    // Create server configuration
    val serverConfig = KtorRpcConfig(
        baseUrl = "http://0.0.0.0:8080",
        enableObfuscation = true,
        enableSecurity = true,
        obfuscationManager = obfuscationManager,
        securityManager = securityManager
    )
    
    // Create and configure server
    val server = KtorRpcServer(serverConfig)
    
    // Register services
    val userService = UserServiceImpl()
    server.registerService("UserService", userService)
    
    println("RPC Server started on port 8080")
    
    // Keep the server running
    while (true) {
        delay(1000)
    }
}
```

The server setup involves several configuration steps:

1. **Obfuscation Configuration**: Set up method name obfuscation with your preferred strategy
2. **Security Configuration**: Configure authentication and authorization settings
3. **Server Configuration**: Create the server with transport and feature settings
4. **Service Registration**: Register your service implementations with the server

### Creating RPC Clients

RPC clients are created using the client runtime and can make calls to registered services:

```kotlin
import com.obfuscated.rpc.runtime.RpcClient
import com.obfuscated.rpc.ktor.KtorRpcClient
import com.obfuscated.rpc.ktor.KtorRpcConfig
import kotlinx.serialization.json.Json

suspend fun useRpcClient() {
    // Configure client
    val clientConfig = KtorRpcConfig(
        baseUrl = "http://localhost:8080",
        enableObfuscation = true,
        enableSecurity = true
    )
    
    val client = KtorRpcClient(clientConfig)
    
    try {
        // Create a new user
        val createRequest = CreateUserRequest(
            name = "John Doe",
            email = "john.doe@example.com"
        )
        
        val newUser = client.call<User>("UserService", "createUser", listOf(
            Json.encodeToJsonElement(CreateUserRequest.serializer(), createRequest)
        ))
        
        println("Created user: $newUser")
        
        // Get user by ID
        val retrievedUser = client.call<User?>("UserService", "getUserById", listOf(
            Json.encodeToJsonElement(String.serializer(), newUser.id)
        ))
        
        println("Retrieved user: $retrievedUser")
        
        // Update user
        val updates = UserUpdates(name = "Jane Doe")
        val updateResult = client.call<Boolean>("UserService", "updateUser", listOf(
            Json.encodeToJsonElement(String.serializer(), newUser.id),
            Json.encodeToJsonElement(UserUpdates.serializer(), updates)
        ))
        
        println("Update result: $updateResult")
        
        // Search users
        val searchResults = client.call<List<User>>("UserService", "searchUsers", listOf(
            Json.encodeToJsonElement(String.serializer(), "Jane")
        ))
        
        println("Search results: $searchResults")
        
        // Use streaming
        println("Streaming users:")
        client.stream<User>("UserService", "getUsersStream", listOf(
            Json.encodeToJsonElement(Int.serializer(), 2)
        )).collect { user ->
            println("  Received user: ${user.name}")
        }
        
    } catch (e: Exception) {
        println("RPC call failed: ${e.message}")
    }
}
```

Client usage involves:

1. **Configuration**: Set up the client with server connection details and feature flags
2. **Method Calls**: Use the `call` method for regular RPC calls with proper serialization
3. **Streaming**: Use the `stream` method for Flow-based streaming operations
4. **Error Handling**: Wrap calls in try-catch blocks to handle network and RPC errors

### Type-Safe Client Generation

For better type safety and developer experience, you can use the Gradle plugin to generate type-safe client proxies:

```kotlin
// Generated by the Gradle plugin
class UserServiceClient(private val client: RpcClient) : UserService {
    override suspend fun createUser(user: CreateUserRequest): User {
        return client.call("UserService", "createUser", listOf(user))
    }
    
    override suspend fun getUserById(userId: String): User? {
        return client.call("UserService", "getUserById", listOf(userId))
    }
    
    override suspend fun updateUser(userId: String, updates: UserUpdates): Boolean {
        return client.call("UserService", "updateUser", listOf(userId, updates))
    }
    
    override suspend fun deleteUser(userId: String): Boolean {
        return client.call("UserService", "deleteUser", listOf(userId))
    }
    
    override suspend fun searchUsers(query: String): List<User> {
        return client.call("UserService", "searchUsers", listOf(query))
    }
    
    override suspend fun getUsersStream(batchSize: Int): Flow<User> {
        return client.stream("UserService", "getUsersStream", listOf(batchSize))
    }
}

// Usage with generated client
suspend fun useGeneratedClient() {
    val rpcClient = KtorRpcClient(clientConfig)
    val userService = UserServiceClient(rpcClient)
    
    // Now you can use the service with full type safety
    val user = userService.createUser(CreateUserRequest("Alice", "alice@example.com"))
    val retrievedUser = userService.getUserById(user.id)
    
    userService.getUsersStream(5).collect { streamedUser ->
        println("Streamed user: ${streamedUser.name}")
    }
}
```

The generated clients provide compile-time type safety, IDE autocompletion, and eliminate the need for manual serialization of parameters.

### Error Handling

The library provides structured error handling through the RPC protocol:

```kotlin
suspend fun handleErrors() {
    val client = KtorRpcClient(clientConfig)
    
    try {
        val user = client.call<User>("UserService", "getUserById", listOf("nonexistent"))
    } catch (e: RpcException) {
        when (e.errorCode) {
            RpcErrorCodes.METHOD_NOT_FOUND -> {
                println("Method not found: ${e.message}")
            }
            RpcErrorCodes.INVALID_PARAMETERS -> {
                println("Invalid parameters: ${e.message}")
            }
            RpcErrorCodes.INTERNAL_ERROR -> {
                println("Server error: ${e.message}")
                // Check error details for more information
                e.errorDetails?.let { details ->
                    println("Error details: $details")
                }
            }
            else -> {
                println("Unknown error: ${e.message}")
            }
        }
    } catch (e: Exception) {
        println("Network or other error: ${e.message}")
    }
}
```

The library distinguishes between RPC-level errors (method not found, invalid parameters, etc.) and transport-level errors (network issues, timeouts, etc.), allowing for appropriate error handling strategies.

This basic usage covers the fundamental patterns for using the Kotlin Obfuscated RPC library. The next sections will dive deeper into advanced features like obfuscation configuration, security settings, and performance optimization.

