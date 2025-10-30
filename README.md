# KWire

[![Maven Central](https://img.shields.io/maven-central/v/net.tactware.kwire/core.svg?label=Maven%20Central)](https://central.sonatype.com/namespace/net.tactware.kwire)

KWire is a Kotlin Multiplatform RPC toolkit with Ktor integration, kotlinx.serialization, and optional obfuscation. It provides a small set of core message types, a pluggable client transport, a native Ktor server plugin for service registration, optional code generation support, and runnable samples.

This repository contains the core library, Ktor integration (client and server plugin), an obfuscation-support module, and runnable samples for a `UserService` over WebSockets.


## Contents
- What is KWire
- Features
- Architecture overview
- Modules
- Quick start
- Running the samples
- Ktor integration (client and server)
- Obfuscation support
- Development (build/test)
- Troubleshooting
- Documentation


## What is KWire
KWire helps you define RPC service interfaces in Kotlin and call them across process/network boundaries. You can:
- Declare a service interface annotated with `@RpcService` and `@RpcMethod`
- Use the native Ktor server plugin to expose services over WebSockets
- Use the provided Ktor WebSocket client transport
- Optionally generate client/server stubs via a Gradle plugin
- Stream values using Kotlin `Flow`
- Optionally obfuscate service/method identifiers


## Features
- Kotlin Multiplatform core (common code, Kotlinx Serialization)
- Ktor WebSocket client transport
- Native Ktor server plugin (register multiple services/endpoints)
- Type-safe service interfaces via annotations
- Streaming with Flow, request/response messaging
- Optional obfuscation mapping/validation at runtime
- Optional Gradle plugin for stub generation
- Runnable production-style samples (`UserService`)


## Architecture overview
- `core`: message types (`RpcRequest`/`RpcResponse`/`Stream*`), errors, and `RpcTransport` plus service annotations
- `ktor-integration-client`: Ktor WebSocket client transport
- `ktor-integration-server`: Native Ktor plugin to expose services over WebSockets
- `ktor-integration-common`: Shared Ktor integration utilities
- `obfuscation-support`: Maps obfuscated ids to service/method names
- `samples`: minimal `UserService` API and runnable client/server examples


## Modules
- `core/` — Core abstractions and messages
  - `net.tactware.kwire.core.RpcService`, `RpcMethod` annotations
  - `net.tactware.kwire.core.messages.*` (`RpcRequest`, `RpcResponse`, `StreamData`, `StreamEnd`, etc.)
  - `net.tactware.kwire.core.RpcTransport` interface
- `ktor-integration-client/` — Ktor WebSocket client transport
  - `net.tactware.kwire.ktor.KtorWebSocketClientTransport`
  - Convenience factory: `ktorWebSocketClientTransport { ... }`
- `ktor-integration-server/` — Ktor server plugin to expose RPC services
  - Install `KWireRpc` and register services with paths
  - Helper: `withGeneratedServer { transport, impl -> ... }`
- `ktor-integration-common/` — Shared Ktor integration utilities
- `obfuscation-support/` — Obfuscation mapping and validation utilities
- `sample-api/` — Sample shared API (`UserService`, DTOs)
- `sample-server/` — Runnable server example using the Ktor plugin
- `sample-client/` — Runnable client example using the Ktor client transport


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

2) Expose the service on the server via the Ktor plugin:

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

3) Connect from the client using the provided Ktor WebSocket transport:

```kotlin
val transport = ktorWebSocketClientTransport(scope) {
    serverUrl("ws://localhost:8082/users")
}
val client = UserServiceClientImpl(transport)
```


## Running the samples
The samples show a production-style WebSocket setup using the native Ktor plugin on the server and the Ktor client transport.

- Server entry point:
  - `sample-server/src/main/kotlin/net/tactware/kwire/sample/server/ServerExample.kt` (object `PluginServerExample`)
  - Starts a Ktor WebSocket server (default `ws://0.0.0.0:8082/users`)

- Client entry point:
  - `sample-client/src/main/kotlin/net/tactware/kwire/sample/client/ClientExample.kt`
  - Connects to `ws://localhost:8082/users` and demonstrates request/response and streaming

Ways to run:
- In IDE: Run the server first (`PluginServerExample`), then `ClientExample`
- Via Gradle (if application main class is configured in your environment):
  - `./gradlew :sample-server:run`
  - `./gradlew :sample-client:run`


## Ktor integration (client and server)
- Client: `net.tactware.kwire.ktor.KtorWebSocketClientTransport`
  - Configure via builder: `ktorWebSocketClientTransport(scope) { serverUrl("ws://localhost:8082/users"); pingInterval(15); requestTimeout(30000) }`

- Server: native Ktor plugin `KWireRpc`
  - Install and register services with paths; optionally use generated servers via `withGeneratedServer { transport, impl -> ... }`
  - See detailed guide in `ktor-integration-server/README.md` and `ktor-integration-server/PLUGIN_README.md`


Each transport implements `net.tactware.kwire.core.RpcTransport` with:
- `suspend fun connect()/disconnect()`
- `val isConnected: Boolean`
- `suspend fun send(message: RpcMessage)`
- `fun receive(): Flow<RpcMessage>`


## Obfuscation support
The `obfuscation-support` module includes:
- `ObfuscationManager` and related classes to map obfuscated identifiers to original names
- Strategies/utilities to control how method/service identifiers are resolved

When enabled, requests carry an obfuscated `methodId`, which is resolved at runtime by the server.


## Development (build/test)
- Build all modules: `./gradlew build`
- Run tests: `./gradlew test`
- Open the project in IntelliJ IDEA for best Kotlin Multiplatform support.

Kotlin/Gradle versions are controlled from `gradle/libs.versions.toml` and `gradle.properties`.


## Troubleshooting
- **WebSocket connection refused**:
  - Ensure the server is running and listening at the same URL the client uses (default `ws://localhost:8082/users` in the samples).
- **No responses arriving**:
  - Check server logs; verify the service is registered at the expected path and that generated server/dispatcher is started.
- **Gradle run task fails in samples**:
  - Run the `PluginServerExample` and `ClientExample` mains directly from your IDE if `mainClass` isn’t aligned.
- **Serialization errors**:
  - Ensure DTOs are `@Serializable` and the same version of kotlinx.serialization is used across modules.


## Documentation
Module-specific guides:
- `ktor-integration-server/README.md` — Ktor server integration overview
- `ktor-integration-server/PLUGIN_README.md` — Ktor plugin usage and configuration


## License
Unless otherwise specified in the repository, this project is provided under the license declared in the repository root or headers (add your license text or file reference here).

## Local credentials (local.properties)
To keep secrets out of `gradle.properties`, the build reads publishing/signing credentials from a `local.properties` file at the project root (if present), then falls back to `gradle.properties`, then environment variables.

Add a `local.properties` with the following keys (do not commit this file):

```
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password
signing.keyId=XXXXXXXX
signing.key=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signing.password=your-key-passphrase
```

Notes:
- `local.properties` should be excluded from version control (it is commonly ignored by default; ensure your VCS ignores it).
- These values are used only for the modules published to Maven Central: `core`, `ktor-integration-client`, `ktor-integration-server`.
- Environment variable fallbacks are still supported: `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD`.
