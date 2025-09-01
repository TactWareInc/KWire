# KWire

KWire is a Kotlin Multiplatform RPC toolkit with  Ktor integration, kotlinx.serialization, and optional obfuscation. It provides a small set of core message types, a pluggable transport abstraction, a Gradle plugin for client/server stub generation, and production-ready WebSocket transports for Ktor.

This repository contains the core library, integration modules, an obfuscation-support module, a Gradle plugin, and runnable samples for a UserService over WebSockets.


## Contents
- What is KWire
- Features
- Architecture overview
- Modules
- Quick start
- Running the samples
- Gradle plugin (code generation)
- Ktor transports
- Obfuscation support
- Development (build/test)
- Troubleshooting
- Documentation


## What is KWire
KWire helps you define RPC service interfaces in Kotlin and call them across process/network boundaries. You can:
- Declare a service interface annotated with @RpcService and @RpcMethod
- Generate client/server stubs via a Gradle plugin (or wire manually)
- Communicate over pluggable transports (Ktor WebSockets provided)
- Stream values using Kotlin Flow
- Optionally obfuscate service/method identifiers


## Features
- Kotlin Multiplatform core (common code, Kotlinx Serialization)
- Ktor WebSocket client/server transports
- Type-safe service interfaces via annotations
- Streaming with Flow, request/response messaging
- Optional obfuscation mapping/validation at runtime
- Gradle plugin for stub generation
- Runnable production-style samples (UserService)


## Architecture overview
- core: message types (RpcRequest/RpcResponse/Stream*), errors, and RpcTransport interface plus service annotations.
- transport: implementations of RpcTransport. This repo includes Ktor WebSocket client/server.
- codegen: a Gradle plugin that can scan interfaces and emit client/server shims.
- obfuscation-support: keeps mappings between public names and obfuscated ids for services/methods.
- samples: minimal UserService API and implementations that run over Ktor WebSockets.


## Modules
- core/ — Core abstractions and messages
  - net.tactware.kwire.core.RpcService, RpcMethod, RpcClient, RpcServer annotations
  - net.tactware.kwire.core.messages.* (RpcRequest, RpcResponse, StreamData, StreamEnd, etc.)
  - net.tactware.kwire.core.RpcTransport interface
- ktor-integration-client/ — Ktor WebSocket client transport
  - net.tactware.kwire.ktor.KtorWebSocketClientTransport
  - Convenience factory: ktorWebSocketClientTransport { ... }
- ktor-integration-server/ — Ktor WebSocket server transport
  - net.tactware.kwire.ktor.KtorWebSocketServerTransport
  - Convenience factory: ktorWebSocketServerTransport { ... }
- obfuscation-support/ — Obfuscation mapping and validation utilities
- gradle-plugin/ — Obfuscated RPC gradle plugin and tasks (e.g., generateRpcStubs)
- sample-api/ — Sample shared API (UserService, DTOs)
- sample-server/ — Runnable server example using Ktor WebSockets and manual dispatch
- sample-client/ — Runnable client example using Ktor WebSockets


## Quick start
1) Declare a service interface (in a shared module):

```kotlin
@RpcService("UserService")
interface UserService {
    @RpcMethod("createUser")
    suspend fun createUser(request: CreateUserRequest): User

    @RpcMethod("getAllUsers")
    suspend fun getAllUsers(): List<User>

    @RpcMethod("streamUsers")
    fun streamUsers(): Flow<User>
}
```

2) Choose how to connect:
- Manual dispatch (as the samples do) by handling RpcRequest/StreamStart yourself
- Or enable the Gradle plugin to generate client/server shims

3) Use the provided Ktor WebSocket transports to connect client to server.


## Running the samples
The samples show a production-style WebSocket setup with manual method dispatch (so you can run without codegen).

- Server entry point:
  - sample-server/src/main/kotlin/net/tactware/kwire/sample/server/ProductionServerMain.kt
  - Starts a Ktor WebSocket server (default ws://0.0.0.0:8082/rpc in code)

- Client entry point:
  - sample-client/src/main/kotlin/net/tactware/kwire/sample/client/ProductionClientMain.kt
  - Connects to ws://localhost:8082/rpc and demonstrates request/response and streaming

Ways to run:
- In IDE: Run ProductionServerMain first, then ProductionClientMain
- Via Gradle (if application main class is configured in your environment):
  - ./gradlew :sample-server:run
  - ./gradlew :sample-client:run

Note: If run tasks fail due to mismatched mainClass, open the sample modules in your IDE and run the ProductionMain files directly. The code does not depend on codegen to run.


## Gradle plugin (code generation)
The plugin net.tactware.kwire.plugin can generate RPC client and server stubs from your annotated interfaces.

In your build.gradle.kts:
```kotlin
plugins {
    id("net.tactware.kwire.plugin")
}

obfuscatedRpc {
    // where to scan service interfaces
    apiSourcePath = "../sample-api/src/main/kotlin"

    // where to output/generated client/server source
    clientSourcePath = "src/main/kotlin"      // optional
    serverSourcePath = "src/main/kotlin"

    // optional flags
    obfuscationEnabled = true
    generateClient = true
    generateServer = true
}
```

The plugin registers a generateRpcStubs task and wires it to compileKotlin. Generated sources are added to the main source set under build/generated/rpc.


## Ktor transports
- Client: net.tactware.kwire.ktor.KtorWebSocketClientTransport
  - Configure via builder: ktorWebSocketClientTransport(scope) { serverUrl("ws://localhost:8082/rpc"); pingInterval(15); requestTimeout(30000) }
- Server: net.tactware.kwire.ktor.KtorWebSocketServerTransport
  - Configure via builder: ktorWebSocketServerTransport { host("0.0.0.0"); port(8082); path("/rpc") }

Each transport implements net.tactware.kwire.core.RpcTransport with:
- suspend fun connect()/disconnect()
- val isConnected: Boolean
- suspend fun send(message: RpcMessage)
- fun receive(): Flow<RpcMessage>

For manual wiring, you can set request/stream handlers on the server transport and send/collect messages on the client transport as shown in the sample Production*Main files.


## Obfuscation support
The obfuscation-support module includes:
- ObfuscationManager and related classes to map obfuscated identifiers to original names
- Strategies/utilities to control how method/service identifiers are resolved

When enabled, requests carry an obfuscated methodId, which is resolved at runtime by the server.


## Development (build/test)
- Build all modules: ./gradlew build
- Run tests: ./gradlew test
- Open the project in IntelliJ IDEA for best Kotlin Multiplatform support.

Kotlin/Gradle versions are controlled from gradle/libs.versions.toml and gradle.properties.


## Troubleshooting
- WebSocket connection refused:
  - Ensure the server is running and listening at the same URL the client uses (default ws://localhost:8082/rpc in the samples).
- No responses arriving:
  - Check server logs; verify manual dispatch handlers are set for the correct service/method ids.
- Gradle run task fails in samples:
  - Run ProductionServerMain and ProductionClientMain directly from your IDE if mainClass isn’t aligned.
- Serialization errors:
  - Ensure DTOs are @Serializable and the same version of kotlinx.serialization is used across modules.


## Documentation
Additional guides and reference live in the docs/ directory:
- docs/README.md — High-level documentation (generic template)
- docs/getting-started.md — Step-by-step guide
- docs/api-reference.md — Conceptual API reference

These documents are generic in places; the module and package names in this README reflect the KWire code in this repository.


## License
Unless otherwise specified in the repository, this project is provided under the license declared in the repository root or headers (add your license text or file reference here).

## Local credentials (local.properties)
To keep secrets out of gradle.properties, the build now reads publishing/signing credentials from a local.properties file at the project root (if present), then falls back to gradle.properties, then environment variables.

Add a local.properties with the following keys (do not commit this file):

```
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password
signing.keyId=XXXXXXXX
signing.key=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signing.password=your-key-passphrase
```

Notes:
- local.properties should be excluded from version control (it is commonly ignored by default; ensure your VCS ignores it).
- These values are used only for the modules published to Maven Central: core, ktor-integration-client, ktor-integration-server.
- Environment variable fallbacks are still supported: OSSRH_USERNAME, OSSRH_PASSWORD, SIGNING_KEY, SIGNING_PASSWORD.
