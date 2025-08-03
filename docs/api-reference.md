# API Reference

This comprehensive API reference covers all public classes, interfaces, and functions provided by the Kotlin Obfuscated RPC library. The API is organized by module for easy navigation.

## Core Module (`com.obfuscated.rpc.core`)

The core module provides the fundamental types and interfaces for RPC communication.

### Annotations

#### @RpcService

Marks an interface as an RPC service that can be exposed via the RPC system.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RpcService
```

**Usage:**
```kotlin
@RpcService
interface MyService {
    // Service methods
}
```

#### @RpcMethod

Marks a method within an RPC service as remotely callable.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RpcMethod
```

**Usage:**
```kotlin
@RpcService
interface MyService {
    @RpcMethod
    suspend fun myMethod(param: String): String
}
```

### Message Types

#### RpcMessage

Base sealed class for all RPC messages.

```kotlin
@Serializable
sealed class RpcMessage {
    abstract val messageId: String
    abstract val timestamp: Long
}
```

**Properties:**
- `messageId: String` - Unique identifier for the message
- `timestamp: Long` - Unix timestamp when the message was created

#### RpcRequest

Represents an RPC method call request.

```kotlin
@Serializable
data class RpcRequest(
    override val messageId: String,
    override val timestamp: Long,
    val serviceName: String,
    val methodId: String,
    val parameters: List<@Contextual JsonElement> = emptyList(),
    val streaming: Boolean = false
) : RpcMessage()
```

**Properties:**
- `serviceName: String` - Name of the target service
- `methodId: String` - Identifier of the method to call (may be obfuscated)
- `parameters: List<JsonElement>` - Serialized method parameters
- `streaming: Boolean` - Whether this is a streaming method call

#### RpcResponse

Represents a successful RPC method response.

```kotlin
@Serializable
data class RpcResponse(
    override val messageId: String,
    override val timestamp: Long,
    val result: @Contextual JsonElement? = null,
    val streaming: Boolean = false
) : RpcMessage()
```

**Properties:**
- `result: JsonElement?` - Serialized method return value
- `streaming: Boolean` - Whether this is part of a streaming response

#### RpcError

Represents an error response from an RPC call.

```kotlin
@Serializable
data class RpcError(
    override val messageId: String,
    override val timestamp: Long,
    val errorCode: String,
    val errorMessage: String,
    val errorDetails: @Contextual JsonElement? = null
) : RpcMessage()
```

**Properties:**
- `errorCode: String` - Structured error code (see RpcErrorCodes)
- `errorMessage: String` - Human-readable error message
- `errorDetails: JsonElement?` - Additional error information

### Stream Messages

#### StreamStart

Initiates a streaming RPC operation.

```kotlin
@Serializable
data class StreamStart(
    override val messageId: String,
    override val timestamp: Long,
    val streamId: String,
    val serviceName: String,
    val methodId: String,
    val parameters: List<@Contextual JsonElement> = emptyList()
) : StreamMessage()
```

#### StreamData

Contains a single item from a streaming operation.

```kotlin
@Serializable
data class StreamData(
    override val messageId: String,
    override val timestamp: Long,
    val streamId: String,
    val data: @Contextual JsonElement
) : StreamMessage()
```

#### StreamEnd

Indicates the completion of a streaming operation.

```kotlin
@Serializable
data class StreamEnd(
    override val messageId: String,
    override val timestamp: Long,
    val streamId: String
) : StreamMessage()
```

#### StreamError

Represents an error in a streaming operation.

```kotlin
@Serializable
data class StreamError(
    override val messageId: String,
    override val timestamp: Long,
    val streamId: String,
    val errorCode: String,
    val errorMessage: String,
    val errorDetails: @Contextual JsonElement? = null
) : StreamMessage()
```

### Error Codes

#### RpcErrorCodes

Standard error codes used throughout the RPC system.

```kotlin
object RpcErrorCodes {
    const val INVALID_REQUEST = "INVALID_REQUEST"
    const val METHOD_NOT_FOUND = "METHOD_NOT_FOUND"
    const val SERVICE_NOT_FOUND = "SERVICE_NOT_FOUND"
    const val INVALID_PARAMETERS = "INVALID_PARAMETERS"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
    const val SERIALIZATION_ERROR = "SERIALIZATION_ERROR"
    const val STREAM_ERROR = "STREAM_ERROR"
    const val TIMEOUT_ERROR = "TIMEOUT_ERROR"
    const val AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR"
    const val AUTHORIZATION_ERROR = "AUTHORIZATION_ERROR"
}
```

### Transport Interface

#### RpcTransport

Abstract interface for RPC transport implementations.

```kotlin
interface RpcTransport {
    suspend fun send(message: RpcMessage): Unit
    suspend fun sendStream(message: StreamStart): Flow<RpcMessage>
    fun receive(): Flow<RpcMessage>
    suspend fun connect(): Unit
    suspend fun disconnect(): Unit
    val isConnected: Boolean
}
```

**Methods:**
- `send(message)` - Send a single RPC message
- `sendStream(message)` - Initiate a streaming operation
- `receive()` - Receive incoming messages as a Flow
- `connect()` - Establish connection to remote endpoint
- `disconnect()` - Close connection
- `isConnected` - Current connection status

### Exceptions

#### RpcException

Base exception class for RPC-related errors.

```kotlin
class RpcException(
    message: String,
    val errorCode: String = RpcErrorCodes.INTERNAL_ERROR,
    val errorDetails: JsonElement? = null,
    cause: Throwable? = null
) : Exception(message, cause)
```

## Runtime Module (`com.obfuscated.rpc.runtime`)

The runtime module provides client and server implementations for RPC communication.

### RpcClient

Client-side RPC runtime for making remote method calls.

```kotlin
class RpcClient(
    private val transport: RpcTransport,
    private val serializer: RpcSerializer = RpcSerializer(),
    private val config: RpcClientConfig = RpcClientConfig()
)
```

**Constructor Parameters:**
- `transport: RpcTransport` - Transport implementation for network communication
- `serializer: RpcSerializer` - Serialization handler
- `config: RpcClientConfig` - Client configuration options

**Methods:**

##### call

Make a synchronous RPC call.

```kotlin
suspend inline fun <reified T> call(
    serviceName: String,
    methodName: String,
    parameters: List<Any> = emptyList()
): T
```

**Parameters:**
- `serviceName: String` - Name of the target service
- `methodName: String` - Name of the method to call
- `parameters: List<Any>` - Method parameters

**Returns:** Deserialized method result of type T

**Throws:** `RpcException` on RPC errors, other exceptions on transport errors

##### stream

Make a streaming RPC call that returns a Flow.

```kotlin
suspend inline fun <reified T> stream(
    serviceName: String,
    methodName: String,
    parameters: List<Any> = emptyList()
): Flow<T>
```

**Parameters:**
- `serviceName: String` - Name of the target service
- `methodName: String` - Name of the streaming method to call
- `parameters: List<Any>` - Method parameters

**Returns:** Flow of deserialized items of type T

### RpcServer

Server-side RPC runtime for handling incoming requests.

```kotlin
class RpcServer(
    private val transport: RpcTransport,
    private val serializer: RpcSerializer = RpcSerializer(),
    private val config: RpcServerConfig = RpcServerConfig()
)
```

**Methods:**

##### registerService

Register a service implementation with the server.

```kotlin
fun <T : Any> registerService(
    serviceClass: KClass<T>,
    implementation: T
)
```

**Parameters:**
- `serviceClass: KClass<T>` - Service interface class
- `implementation: T` - Service implementation instance

##### start

Start the RPC server.

```kotlin
suspend fun start()
```

##### stop

Stop the RPC server.

```kotlin
suspend fun stop()
```

### Configuration Classes

#### RpcClientConfig

Configuration options for RPC clients.

```kotlin
data class RpcClientConfig(
    val timeout: Long = 30000,
    val retryAttempts: Int = 3,
    val retryDelay: Long = 1000,
    val enableCompression: Boolean = false,
    val maxMessageSize: Int = 1024 * 1024 // 1MB
)
```

#### RpcServerConfig

Configuration options for RPC servers.

```kotlin
data class RpcServerConfig(
    val maxConcurrentRequests: Int = 100,
    val requestTimeout: Long = 60000,
    val enableCompression: Boolean = false,
    val maxMessageSize: Int = 1024 * 1024 // 1MB
)
```

## Serialization Module (`com.obfuscated.rpc.serialization`)

The serialization module provides Flow serialization and RPC-specific serialization utilities.

### Annotations

#### @RpcSerializable

Marks a class as serializable for RPC communication.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RpcSerializable
```

### RpcSerializer

Main serialization handler for RPC operations.

```kotlin
class RpcSerializer(
    private val json: Json = Json.Default
)
```

**Methods:**

##### serialize

Serialize an object to JsonElement.

```kotlin
fun <T> serialize(value: T, serializer: KSerializer<T>): JsonElement
```

##### deserialize

Deserialize a JsonElement to an object.

```kotlin
fun <T> deserialize(element: JsonElement, serializer: KSerializer<T>): T
```

### Flow Serialization

#### FlowSerializationUtils

Utilities for serializing and deserializing Flow types.

```kotlin
object FlowSerializationUtils {
    fun <T> serializeFlowItem(
        item: T,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): JsonElement
    
    fun <T> deserializeFlowItem(
        element: JsonElement,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): T
    
    fun <T> createFlowFromJsonElements(
        elements: Sequence<JsonElement>,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): Flow<T>
    
    fun <T> flowToJsonElements(
        flow: Flow<T>,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): Flow<JsonElement>
}
```

#### Flow Extensions

Extension functions for Flow serialization.

```kotlin
// Serialize Flow items to JsonElement
fun <T> Flow<T>.serializeItems(
    serializer: KSerializer<T>,
    json: Json = Json.Default
): Flow<JsonElement>

// Deserialize JsonElement Flow to typed Flow
fun <T> Flow<JsonElement>.deserializeItems(
    serializer: KSerializer<T>,
    json: Json = Json.Default
): Flow<T>
```

### Streaming Support

#### StreamingConfig

Configuration for streaming operations.

```kotlin
data class StreamingConfig(
    val bufferSize: Int = 64,
    val maxRetries: Int = 3,
    val retryDelay: Long = 1000,
    val enableBackpressure: Boolean = true,
    val enableErrorRecovery: Boolean = true
)
```

#### StreamingSupport

Utilities for streaming RPC operations.

```kotlin
object StreamingSupport {
    fun <T> wrapFlowForRpc(
        flow: Flow<T>,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): Flow<JsonElement>
    
    fun <T> unwrapFlowFromRpc(
        jsonFlow: Flow<JsonElement>,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): Flow<T>
    
    fun <T> createMetadataFlow(
        flow: Flow<T>,
        streamId: String
    ): Flow<StreamItem<T>>
    
    fun <T> extractDataFromMetadataFlow(
        metadataFlow: Flow<StreamItem<T>>
    ): Flow<T>
}
```

## Obfuscation Support Module (`com.obfuscated.rpc.obfuscation`)

The obfuscation support module provides method name obfuscation and security features.

### ObfuscationManager

Central manager for RPC obfuscation operations.

```kotlin
class ObfuscationManager(
    private val config: ObfuscationConfig = ObfuscationConfig()
)
```

**Methods:**

##### generateMappings

Generate obfuscation mappings for a list of services.

```kotlin
fun generateMappings(services: List<ServiceInfo>): ObfuscationMappings
```

##### getObfuscatedMethodName

Get the obfuscated method name for a service method.

```kotlin
fun getObfuscatedMethodName(serviceName: String, methodName: String): String
```

##### getOriginalMethodName

Get the original method name from an obfuscated name.

```kotlin
fun getOriginalMethodName(obfuscatedName: String): Pair<String, String>?
```

##### loadMappings

Load method mappings from a JSON string.

```kotlin
fun loadMappings(mappingsJson: String)
```

##### exportMappings

Export current mappings to JSON.

```kotlin
fun exportMappings(): String
```

### Configuration Classes

#### ObfuscationConfig

Configuration for obfuscation behavior.

```kotlin
data class ObfuscationConfig(
    val enabled: Boolean = true,
    val strategy: ObfuscationStrategy = ObfuscationStrategy.HASH_BASED,
    val methodNameLength: Int = 8,
    val serviceNameLength: Int = 6,
    val obfuscateServiceNames: Boolean = true,
    val prefix: String = "m"
)
```

#### ObfuscationStrategy

Available obfuscation strategies.

```kotlin
enum class ObfuscationStrategy {
    RANDOM,      // Generate completely random names
    HASH_BASED,  // Use hash of original name as base
    SEQUENTIAL   // Use sequential numbering
}
```

### SecurityManager

Security manager for RPC operations.

```kotlin
class SecurityManager(
    private val config: SecurityConfig = SecurityConfig()
)
```

**Methods:**

##### authenticate

Authenticate a client and create a session.

```kotlin
fun authenticate(credentials: AuthCredentials): AuthResult
```

##### validateSession

Validate a session token.

```kotlin
fun validateSession(sessionToken: String): SessionValidationResult
```

##### authorize

Check if a client is authorized to call a specific method.

```kotlin
fun authorize(
    sessionToken: String,
    serviceName: String,
    methodName: String
): AuthorizationResult
```

### Security Types

#### AuthCredentials

Authentication credentials for client authentication.

```kotlin
data class AuthCredentials(
    val clientId: String? = null,
    val token: String? = null,
    val apiKey: String? = null,
    val customData: Map<String, String> = emptyMap()
)
```

#### AuthResult

Result of an authentication attempt.

```kotlin
sealed class AuthResult {
    data class Success(val sessionToken: String) : AuthResult()
    data class Failure(val reason: String) : AuthResult()
}
```

## Ktor Integration Module (`com.obfuscated.rpc.ktor`)

The Ktor integration module provides HTTP transport implementation using Ktor.

### KtorRpcTransport

Ktor-based RPC transport implementation.

```kotlin
class KtorRpcTransport(
    private val config: KtorRpcConfig = KtorRpcConfig()
) : RpcTransport
```

### KtorRpcClient

Ktor-specific RPC client implementation.

```kotlin
class KtorRpcClient(
    private val config: KtorRpcConfig
)
```

**Methods:**

##### call

Make an RPC call using Ktor HTTP transport.

```kotlin
suspend fun call(
    serviceName: String,
    methodName: String,
    parameters: List<JsonElement>
): JsonElement
```

##### stream

Make a streaming RPC call using Ktor.

```kotlin
suspend fun stream(
    serviceName: String,
    methodName: String,
    parameters: List<JsonElement>
): Flow<JsonElement>
```

### KtorRpcServer

Ktor-specific RPC server implementation.

```kotlin
class KtorRpcServer(
    private val config: KtorRpcConfig
)
```

**Methods:**

##### registerService

Register a service handler with the server.

```kotlin
fun registerService(
    serviceName: String,
    handler: suspend (String, List<JsonElement>) -> JsonElement
)
```

##### handleRequest

Handle an incoming RPC request.

```kotlin
suspend fun handleRequest(requestJson: String): String
```

### Configuration

#### KtorRpcConfig

Configuration for Ktor RPC transport.

```kotlin
data class KtorRpcConfig(
    val baseUrl: String = "http://localhost:8080",
    val timeout: Long = 30000,
    val enableSecurity: Boolean = true,
    val enableObfuscation: Boolean = true,
    val obfuscationManager: ObfuscationManager? = null,
    val securityManager: SecurityManager? = null,
    val serializer: RpcSerializer = RpcSerializer()
)
```

### Utility Functions

#### KtorRpcUtils

Utility functions for creating Ktor RPC clients and servers.

```kotlin
object KtorRpcUtils {
    fun createClient(
        baseUrl: String = "http://localhost:8080",
        enableObfuscation: Boolean = true,
        enableSecurity: Boolean = true
    ): KtorRpcClient
    
    fun createServer(
        enableObfuscation: Boolean = true,
        enableSecurity: Boolean = true
    ): KtorRpcServer
}
```

## Gradle Plugin

The Gradle plugin provides code generation capabilities for RPC stubs and obfuscation mappings.

### Plugin Configuration

```kotlin
obfuscatedRpc {
    generateStubs = true
    obfuscationEnabled = true
    outputDirectory = file("build/generated/rpc")
    servicePackages = listOf("com.example.services")
}
```

### Generated Code

The plugin generates:
- Type-safe client proxies
- Server stub handlers
- Obfuscation mapping files
- Service registry classes

This API reference provides comprehensive coverage of all public APIs in the Kotlin Obfuscated RPC library. For usage examples and best practices, refer to the other documentation sections.

