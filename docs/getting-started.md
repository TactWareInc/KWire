# Getting Started with Kotlin Obfuscated RPC

This comprehensive guide will walk you through setting up and using the Kotlin Obfuscated RPC library from scratch. By the end of this guide, you'll have a working RPC system with obfuscation and streaming capabilities.

## Prerequisites

Before starting, ensure you have the following installed:

- **Kotlin 1.9.21 or later**
- **Gradle 8.5 or later**
- **Java 17 or later** (for JVM targets)
- **IntelliJ IDEA or Android Studio** (recommended for development)

## Step 1: Project Setup

### Creating a New Project

Create a new Kotlin multiplatform project or add the RPC library to an existing project.

For a new project, create the following structure:
```
my-rpc-project/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── src/
│   ├── commonMain/kotlin/
│   ├── jvmMain/kotlin/
│   └── jsMain/kotlin/
└── server/
    └── src/main/kotlin/
```

### Configure build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.obfuscated.rpc.gradle-plugin") version "1.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    
    jvm {
        withJava()
    }
    
    js(IR) {
        browser()
        nodejs()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core RPC dependencies
                implementation("com.obfuscated.rpc:core:1.0.0")
                implementation("com.obfuscated.rpc:runtime:1.0.0")
                implementation("com.obfuscated.rpc:serialization:1.0.0")
                implementation("com.obfuscated.rpc:obfuscation-support:1.0.0")
                
                // Kotlin dependencies
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation("com.obfuscated.rpc:ktor-integration:1.0.0")
                implementation("io.ktor:ktor-server-core:2.3.7")
                implementation("io.ktor:ktor-server-netty:2.3.7")
                implementation("io.ktor:ktor-client-cio:2.3.7")
            }
        }
        
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:2.3.7")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

// Configure the RPC plugin
obfuscatedRpc {
    generateStubs = true
    obfuscationEnabled = true
    outputDirectory = file("build/generated/rpc")
}
```

### Configure settings.gradle.kts

```kotlin
rootProject.name = "my-rpc-project"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

## Step 2: Define Your First Service

Create a simple calculator service to demonstrate basic RPC functionality.

### Create the Service Interface

Create `src/commonMain/kotlin/com/example/CalculatorService.kt`:

```kotlin
package com.example

import com.obfuscated.rpc.core.RpcService
import com.obfuscated.rpc.core.RpcMethod
import com.obfuscated.rpc.serialization.RpcSerializable
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@RpcService
interface CalculatorService {
    @RpcMethod
    suspend fun add(a: Double, b: Double): Double
    
    @RpcMethod
    suspend fun subtract(a: Double, b: Double): Double
    
    @RpcMethod
    suspend fun multiply(a: Double, b: Double): Double
    
    @RpcMethod
    suspend fun divide(a: Double, b: Double): CalculationResult
    
    @RpcMethod
    suspend fun calculateSequence(start: Double, operation: String, step: Double, count: Int): Flow<Double>
    
    @RpcMethod
    suspend fun getCalculationHistory(): List<CalculationRecord>
}

@Serializable
@RpcSerializable
data class CalculationResult(
    val result: Double,
    val success: Boolean,
    val errorMessage: String? = null
)

@Serializable
@RpcSerializable
data class CalculationRecord(
    val operation: String,
    val operands: List<Double>,
    val result: Double,
    val timestamp: Long
)
```

### Implement the Service

Create `src/jvmMain/kotlin/com/example/CalculatorServiceImpl.kt`:

```kotlin
package com.example

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentLinkedQueue

class CalculatorServiceImpl : CalculatorService {
    private val history = ConcurrentLinkedQueue<CalculationRecord>()
    
    override suspend fun add(a: Double, b: Double): Double {
        val result = a + b
        recordCalculation("add", listOf(a, b), result)
        return result
    }
    
    override suspend fun subtract(a: Double, b: Double): Double {
        val result = a - b
        recordCalculation("subtract", listOf(a, b), result)
        return result
    }
    
    override suspend fun multiply(a: Double, b: Double): Double {
        val result = a * b
        recordCalculation("multiply", listOf(a, b), result)
        return result
    }
    
    override suspend fun divide(a: Double, b: Double): CalculationResult {
        return if (b == 0.0) {
            CalculationResult(
                result = 0.0,
                success = false,
                errorMessage = "Division by zero is not allowed"
            )
        } else {
            val result = a / b
            recordCalculation("divide", listOf(a, b), result)
            CalculationResult(
                result = result,
                success = true
            )
        }
    }
    
    override suspend fun calculateSequence(
        start: Double,
        operation: String,
        step: Double,
        count: Int
    ): Flow<Double> = flow {
        var current = start
        emit(current)
        
        repeat(count - 1) {
            delay(100) // Simulate calculation time
            current = when (operation) {
                "add" -> current + step
                "multiply" -> current * step
                "power" -> Math.pow(current, step)
                else -> current + step
            }
            emit(current)
        }
    }
    
    override suspend fun getCalculationHistory(): List<CalculationRecord> {
        return history.toList()
    }
    
    private fun recordCalculation(operation: String, operands: List<Double>, result: Double) {
        val record = CalculationRecord(
            operation = operation,
            operands = operands,
            result = result,
            timestamp = System.currentTimeMillis()
        )
        history.offer(record)
        
        // Keep only the last 100 records
        while (history.size > 100) {
            history.poll()
        }
    }
}
```

## Step 3: Create the Server

Create `src/jvmMain/kotlin/com/example/CalculatorServer.kt`:

```kotlin
package com.example

import com.obfuscated.rpc.ktor.KtorRpcServer
import com.obfuscated.rpc.ktor.KtorRpcConfig
import com.obfuscated.rpc.obfuscation.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.response.*
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        startCalculatorServer()
    }
}

suspend fun startCalculatorServer() {
    // Configure obfuscation
    val obfuscationConfig = ObfuscationConfig(
        enabled = true,
        strategy = ObfuscationStrategy.HASH_BASED,
        methodNameLength = 8,
        serviceNameLength = 6,
        obfuscateServiceNames = true
    )
    
    val obfuscationManager = ObfuscationManager(obfuscationConfig)
    
    // Generate mappings for our service
    val services = listOf(
        ServiceInfo(
            name = "CalculatorService",
            methods = listOf(
                "add", "subtract", "multiply", "divide",
                "calculateSequence", "getCalculationHistory"
            )
        )
    )
    
    val mappings = obfuscationManager.generateMappings(services)
    println("Generated obfuscation mappings:")
    mappings.services.forEach { (serviceName, methods) ->
        println("  Service: $serviceName")
        methods.forEach { (original, obfuscated) ->
            println("    $original -> $obfuscated")
        }
    }
    
    // Configure security
    val securityConfig = SecurityConfig(
        authMethod = AuthMethod.TOKEN,
        sessionTimeoutMs = 3600000, // 1 hour
        enableRateLimit = true,
        rateLimitConfig = RateLimitConfig(
            requestsPerMinute = 100,
            burstSize = 20
        )
    )
    
    val securityManager = SecurityManager(securityConfig)
    
    // Configure RPC server
    val rpcConfig = KtorRpcConfig(
        enableObfuscation = true,
        enableSecurity = true,
        obfuscationManager = obfuscationManager,
        securityManager = securityManager
    )
    
    val rpcServer = KtorRpcServer(rpcConfig)
    
    // Register services
    val calculatorService = CalculatorServiceImpl()
    rpcServer.registerService("CalculatorService") { methodName, parameters ->
        when (methodName) {
            "add" -> {
                val a = parameters[0].toString().toDouble()
                val b = parameters[1].toString().toDouble()
                calculatorService.add(a, b)
            }
            "subtract" -> {
                val a = parameters[0].toString().toDouble()
                val b = parameters[1].toString().toDouble()
                calculatorService.subtract(a, b)
            }
            // Add other method handlers...
            else -> throw IllegalArgumentException("Unknown method: $methodName")
        }
    }
    
    // Start Ktor server
    val server = embeddedServer(Netty, port = 8080) {
        routing {
            post("/rpc") {
                val requestBody = call.receiveText()
                val response = rpcServer.handleRequest(requestBody)
                call.respondText(response, ContentType.Application.Json)
            }
            
            get("/health") {
                call.respondText("OK")
            }
            
            get("/mappings") {
                val mappingsJson = obfuscationManager.exportMappings()
                call.respondText(mappingsJson, ContentType.Application.Json)
            }
        }
    }
    
    println("Calculator RPC Server starting on port 8080...")
    println("Health check: http://localhost:8080/health")
    println("Obfuscation mappings: http://localhost:8080/mappings")
    
    server.start(wait = true)
}
```

## Step 4: Create a Client

Create `src/jvmMain/kotlin/com/example/CalculatorClient.kt`:

```kotlin
package com.example

import com.obfuscated.rpc.ktor.KtorRpcClient
import com.obfuscated.rpc.ktor.KtorRpcConfig
import com.obfuscated.rpc.obfuscation.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToJsonElement

fun main() {
    runBlocking {
        runCalculatorClient()
    }
}

suspend fun runCalculatorClient() {
    // Configure client with same obfuscation settings as server
    val obfuscationConfig = ObfuscationConfig(
        enabled = true,
        strategy = ObfuscationStrategy.HASH_BASED,
        methodNameLength = 8,
        serviceNameLength = 6,
        obfuscateServiceNames = true
    )
    
    val obfuscationManager = ObfuscationManager(obfuscationConfig)
    
    // Load mappings from server (in real app, these would be bundled with client)
    val services = listOf(
        ServiceInfo(
            name = "CalculatorService",
            methods = listOf(
                "add", "subtract", "multiply", "divide",
                "calculateSequence", "getCalculationHistory"
            )
        )
    )
    obfuscationManager.generateMappings(services)
    
    val clientConfig = KtorRpcConfig(
        baseUrl = "http://localhost:8080",
        enableObfuscation = true,
        enableSecurity = true,
        obfuscationManager = obfuscationManager
    )
    
    val client = KtorRpcClient(clientConfig)
    
    try {
        println("=== Calculator RPC Client Demo ===")
        
        // Basic arithmetic operations
        println("\n--- Basic Operations ---")
        
        val sum = client.call<Double>("CalculatorService", "add", listOf(
            Json.encodeToJsonElement(10.5),
            Json.encodeToJsonElement(5.3)
        ))
        println("10.5 + 5.3 = $sum")
        
        val difference = client.call<Double>("CalculatorService", "subtract", listOf(
            Json.encodeToJsonElement(20.0),
            Json.encodeToJsonElement(8.0)
        ))
        println("20.0 - 8.0 = $difference")
        
        val product = client.call<Double>("CalculatorService", "multiply", listOf(
            Json.encodeToJsonElement(4.0),
            Json.encodeToJsonElement(7.0)
        ))
        println("4.0 * 7.0 = $product")
        
        // Division with error handling
        val divisionResult = client.call<CalculationResult>("CalculatorService", "divide", listOf(
            Json.encodeToJsonElement(15.0),
            Json.encodeToJsonElement(3.0)
        ))
        
        if (divisionResult.success) {
            println("15.0 / 3.0 = ${divisionResult.result}")
        } else {
            println("Division failed: ${divisionResult.errorMessage}")
        }
        
        // Test division by zero
        val divisionByZero = client.call<CalculationResult>("CalculatorService", "divide", listOf(
            Json.encodeToJsonElement(10.0),
            Json.encodeToJsonElement(0.0)
        ))
        
        if (!divisionByZero.success) {
            println("Division by zero handled: ${divisionByZero.errorMessage}")
        }
        
        // Streaming operations
        println("\n--- Streaming Operations ---")
        
        println("Calculating sequence: 2, 4, 8, 16, 32...")
        client.stream<Double>("CalculatorService", "calculateSequence", listOf(
            Json.encodeToJsonElement(2.0),
            Json.encodeToJsonElement("multiply"),
            Json.encodeToJsonElement(2.0),
            Json.encodeToJsonElement(5)
        )).collect { value ->
            println("  -> $value")
        }
        
        // Get calculation history
        println("\n--- Calculation History ---")
        val history = client.call<List<CalculationRecord>>("CalculatorService", "getCalculationHistory", emptyList())
        
        history.forEach { record ->
            println("${record.operation}(${record.operands.joinToString(", ")}) = ${record.result} at ${record.timestamp}")
        }
        
        println("\n=== Demo completed successfully ===")
        
    } catch (e: Exception) {
        println("Client error: ${e.message}")
        e.printStackTrace()
    }
}
```

## Step 5: Build and Run

### Build the Project

```bash
./gradlew build
```

This will compile all modules and generate RPC stubs if the Gradle plugin is configured.

### Run the Server

In one terminal:
```bash
./gradlew :server:run
```

Or run the main function in `CalculatorServer.kt` from your IDE.

### Run the Client

In another terminal:
```bash
./gradlew :client:run
```

Or run the main function in `CalculatorClient.kt` from your IDE.

## Step 6: Verify Everything Works

1. **Check Server Health**: Visit `http://localhost:8080/health` in your browser
2. **View Obfuscation Mappings**: Visit `http://localhost:8080/mappings` to see the generated mappings
3. **Run Client**: Execute the client to see RPC calls in action
4. **Monitor Logs**: Check both server and client logs for obfuscated method names

## Next Steps

Now that you have a working RPC system, you can:

1. **Add More Services**: Create additional service interfaces and implementations
2. **Configure Security**: Set up authentication and authorization
3. **Optimize Performance**: Tune serialization and transport settings
4. **Deploy to Production**: Configure for your deployment environment
5. **Add Monitoring**: Integrate logging and metrics collection

## Troubleshooting

### Common Issues

**Build Failures**:
- Ensure all dependencies are correctly specified
- Check Kotlin and Gradle versions
- Verify plugin configuration

**Connection Issues**:
- Check server is running on correct port
- Verify firewall settings
- Ensure client is connecting to correct URL

**Serialization Errors**:
- Verify all data classes have `@Serializable` annotation
- Check parameter types match between client and server
- Ensure kotlinx.serialization is properly configured

**Obfuscation Issues**:
- Verify obfuscation configuration matches between client and server
- Check mapping generation and loading
- Ensure ProGuard rules are correct if using ProGuard

For more detailed troubleshooting, check the library logs and refer to the API documentation.

This getting started guide provides a solid foundation for using the Kotlin Obfuscated RPC library. The next sections of the documentation cover advanced topics like security configuration, performance optimization, and production deployment.

