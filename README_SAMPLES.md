# Sample Server and Client

This directory contains working examples of the Obfuscated RPC library with a complete server and client implementation.

## Overview

The samples demonstrate:
- **Real RPC communication** over HTTP and WebSockets
- **Obfuscation in action** with method name mapping
- **Flow streaming** for real-time data
- **Complete CRUD operations** with a User service
- **Performance testing** and benchmarking

## Architecture

```
┌─────────────────┐    HTTP/WebSocket    ┌─────────────────┐
│   RPC Client    │ ◄─────────────────► │   RPC Server    │
│                 │                      │                 │
│ - UserService   │                      │ - UserService   │
│   Client        │                      │   Implementation│
│ - Ktor Client   │                      │ - Ktor Server   │
│ - Obfuscation   │                      │ - Obfuscation   │
└─────────────────┘                      └─────────────────┘
```

## Modules

### sample-server
- **UserService**: Complete user management service with CRUD operations
- **Ktor Integration**: HTTP server with WebSocket support for streaming
- **Obfuscation**: Method name obfuscation with configurable strategies
- **Real Transport**: Actual network communication (not mocked)

### sample-client  
- **UserServiceClient**: Type-safe client wrapper for the user service
- **Ktor Client**: HTTP client with WebSocket support
- **Comprehensive Examples**: CRUD, search, streaming, and performance tests

## Running the Samples

### 1. Start the Server
```bash
./gradlew :sample-server:run
```

The server will start on `http://localhost:8080` with endpoints:
- `POST /rpc` - HTTP RPC calls
- `WS /rpc-stream` - WebSocket streaming
- `GET /health` - Health check

### 2. Run the Client
```bash
./gradlew :sample-client:run
```

The client will connect to the server and run through all examples.

## Features Demonstrated

### ✅ Basic RPC Operations
- Create, read, update, delete users
- Get user statistics
- Type-safe serialization with kotlinx.serialization

### ✅ Search Operations
- Search users by name pattern
- Filter by age range
- Filter by active status
- Complex search criteria

### ✅ Streaming Operations
- Stream all users with Flow
- Real-time user updates
- WebSocket-based streaming transport

### ✅ Obfuscation Features
- Method name obfuscation (hash-based strategy)
- Automatic mapping generation
- Runtime method resolution
- Configurable obfuscation strategies

### ✅ Performance Testing
- Benchmark RPC call latency
- Measure throughput
- Performance metrics logging

## Sample Output

### Server Output
```
Starting Obfuscated RPC Server...
Generated obfuscation mappings:
  Service: UserService
    createUser -> a1b2c3d4
    getUserById -> e5f6g7h8
    updateUser -> i9j0k1l2
    ...
Server starting on http://0.0.0.0:8080
```

### Client Output
```
Starting Obfuscated RPC Client...
Connected to RPC server

=== Basic Operations ===
User Stats: UserStats(totalUsers=5, activeUsers=5, averageAge=32.4, timestamp=...)
All Users (5):
  - Alice Johnson (alice@example.com, age 28)
  - Bob Smith (bob@example.com, age 35)
  ...

=== Streaming Operations ===
Streaming all users:
  Streamed user: Alice Johnson
  Streamed user: Bob Smith
  ...

=== Performance Test ===
Performance Results:
  Total time: 245ms
  Average time per call: 24.50ms
  Calls per second: 40.8
```

## Configuration

Both server and client use the same obfuscation configuration:
- **Strategy**: Hash-based obfuscation
- **Method name length**: 8 characters
- **Service name length**: 6 characters
- **Security**: Token-based authentication
- **Rate limiting**: Enabled

## Network Protocol

### HTTP RPC Call
```json
POST /rpc
{
  "messageId": "msg_1234567890_123",
  "timestamp": 1234567890123,
  "serviceName": "UserService",
  "methodId": "a1b2c3d4",
  "parameters": [...],
  "streaming": false
}
```

### WebSocket Streaming
```json
{
  "messageId": "msg_1234567890_456", 
  "timestamp": 1234567890123,
  "serviceName": "UserService",
  "methodId": "e5f6g7h8",
  "streamId": "stream_1234567890_789",
  "parameters": [...]
}
```

## Key Benefits Demonstrated

1. **Production Ready**: Real network communication, not mocked
2. **Type Safety**: Full kotlinx.serialization integration
3. **Obfuscation Control**: You control the obfuscation strategy
4. **Performance**: Efficient binary protocol with streaming
5. **Multiplatform**: Works on JVM with Ktor
6. **Flow Integration**: Native Kotlin Flow support
7. **Easy to Use**: Simple client/server setup

This demonstrates the library working exactly as designed - a production-ready RPC system with controllable obfuscation!

