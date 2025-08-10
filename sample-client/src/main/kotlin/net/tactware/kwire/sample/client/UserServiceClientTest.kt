package net.tactware.kwire.sample.client

import net.tactware.kwire.core.messages.RpcRequest
import net.tactware.kwire.core.messages.RpcResponse
import net.tactware.kwire.core.messages.StreamData
import net.tactware.kwire.core.messages.StreamEnd
import net.tactware.kwire.core.messages.StreamStart
import net.tactware.kwire.ktor.ktorWebSocketClientTransport
import net.tactware.kwire.sample.api.CreateUserRequest
import net.tactware.kwire.sample.api.User
import net.tactware.kwire.sample.api.UserService
import net.tactware.kwire.sample.api.UserStats
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class UserServiceClientTest(
    private val address : String,
    private val callTimeout : Duration = 5.seconds,
    private val scope : CoroutineScope,
    ) : UserService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _streamUsers : MutableSharedFlow<List<User>> = MutableSharedFlow()
    private var streamUsersId by atomic<String?>(null)

    init {
        scope.launch(Dispatchers.Default) {
            _streamUsers.subscriptionCount.collect {
                if (it > 0) {
                    if (streamUsersId == null) {
                        val safeStreamId = generateStreamId().also { streamUsersId = it }

                        val streamRequest = StreamStart(
                            messageId = generateMessageId(),
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            streamId = safeStreamId,
                            serviceName = "UserService",
                            methodId = "streamUsers",
                            parameters = emptyList()
                        )

                        transport.send(streamRequest)

                        transport.receive()
                            .filterIsInstance<StreamData>()
                            .collect { response ->
                                if (response.streamId == safeStreamId) {
                                    val users = json.decodeFromString<List<User>>(response.data.toString())
                                    _streamUsers.emit(users)
                                }
                            }
                    }

                } else {
                    val oldStreamId = streamUsersId
                    streamUsersId = null
                    val stopRequest = StreamEnd(
                        messageId = generateMessageId(),
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        streamId = oldStreamId!!,
                    )
                    transport.send(stopRequest)
                }
            }
        }
    }

    private val transport = ktorWebSocketClientTransport(scope = scope) {
        serverUrl("$address/UserService")  
        pingInterval(15)                 
        requestTimeout(30_000)              
        reconnectDelay(5_000)
        maxReconnectAttempts(3)
    }

    override suspend fun createUser(request: CreateUserRequest): User {
        val createRpcRequest = RpcRequest(
            messageId = generateMessageId(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            serviceName = "UserService",
            methodId = "createUser",
            parameters = listOf(json.parseToJsonElement(json.encodeToString(request)))
        )

        transport.send(createRpcRequest)

        val createResponse = withTimeout(callTimeout) {
            transport.receive()
                .filterIsInstance<RpcResponse>()
                .first { it.messageId == createRpcRequest.messageId }
        }
        val newUser = json.decodeFromString<User>(createResponse.result.toString())
        return newUser
    }

    override suspend fun getUserById(id: String): User? {
        val userRequest = RpcRequest(
            messageId = generateMessageId(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            serviceName = "UserService",
            methodId = "getUserById",
            parameters = listOf(json.parseToJsonElement(id))
        )
        transport.send(userRequest)
        
        val userResponse = withTimeout(callTimeout) {
            transport.receive()
                .filterIsInstance<RpcResponse>()
                .first { it.messageId == userRequest.messageId }
        }
        val user = json.decodeFromString<User?>(userResponse.result.toString())
        return user
    }

    override suspend fun getAllUsers(): List<User> {
        val usersRequest = RpcRequest(
            messageId = generateMessageId(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            serviceName = "UserService",
            methodId = "getAllUsers",
            parameters = emptyList()
        )

        transport.send(usersRequest)

        val usersResponse = withTimeout(callTimeout) {
            transport.receive()
                .filterIsInstance<RpcResponse>()
                .first { it.messageId == usersRequest.messageId }
        }

        val users = json.decodeFromString<List<User>>(usersResponse.result.toString())

        return users
    }

    override suspend fun getUserStats(): UserStats {
        val statsRequest = RpcRequest(
            messageId = generateMessageId(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            serviceName = "UserService",
            methodId = "getUserStats",
            parameters = emptyList()
        )

        transport.send(statsRequest)

        // Wait for response with timeout
        val statsResponse = withTimeout(callTimeout) {
            transport.receive()
                .filterIsInstance<RpcResponse>()
                .first { it.messageId == statsRequest.messageId }
        }
        val stats = json.decodeFromString<UserStats>(statsResponse.result.toString())

        return stats
    }

    override fun streamUsers(): Flow<List<User>> {
       return _streamUsers.asSharedFlow()
    }

    private fun generateMessageId(): String {
        return "msg_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }

    private fun generateStreamId(): String {
        return "stream_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}