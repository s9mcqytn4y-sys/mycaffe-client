package com.azuredevid.cassy.data.network

import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

enum class RealtimeConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

@Serializable
data class WsTicketResponseDto(
    val ticket: String,
    @SerialName("expires_at") val expiresAt: String = "",
    val channels: List<String> = emptyList()
)

@Serializable
data class RealtimeEventEnvelope(
    val seq: Long = -1L,
    val channel: String = "",
    val type: String = "",
    @SerialName("payload_json") val payloadJson: String = ""
)

class CassyRealtimeClient(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val sessionManager: SessionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _connectionState = MutableStateFlow(RealtimeConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<RealtimeEventEnvelope>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEventEnvelope> = _events.asSharedFlow()

    @Volatile
    private var lastSeq: Long = -1L
    private var connectionJob: Job? = null

    suspend fun requestTicket(): Result<WsTicketResponseDto> {
        val token = sessionManager.getAccessToken()
            ?: return Result.failure(IllegalStateException("Sesi staf tidak tersedia untuk tiket realtime."))
        val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
        return runCatching {
            val response = httpClient.post("$baseUrl/api/v1/pos/ws-ticket") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (!response.status.isSuccess()) {
                throw HttpErrorMapper.toDomainException(response, "Gagal meminta tiket WebSocket.")
            }
            response.body<WsTicketResponseDto>()
        }
    }

    fun buildWebSocketUrl(ticket: String): String {
        val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
        val wsBase = when {
            baseUrl.startsWith("https://", ignoreCase = true) -> "wss://" + baseUrl.removePrefix("https://")
            baseUrl.startsWith("http://", ignoreCase = true) -> "ws://" + baseUrl.removePrefix("http://")
            else -> baseUrl
        }
        val encodedTicket = URLEncoder.encode(ticket, "UTF-8")
        return if (lastSeq >= 0) {
            "$wsBase/ws?ticket=$encodedTicket&last_seq=$lastSeq"
        } else {
            "$wsBase/ws?ticket=$encodedTicket"
        }
    }

    fun start(scope: CoroutineScope) {
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch(ioDispatcher) {
            var attempt = 0
            while (isActive) {
                if (sessionManager.getAccessToken().isNullOrBlank()) {
                    _connectionState.value = RealtimeConnectionState.DISCONNECTED
                    delay(2_000L)
                    continue
                }

                _connectionState.value = if (attempt == 0) {
                    RealtimeConnectionState.CONNECTING
                } else {
                    RealtimeConnectionState.RECONNECTING
                }

                val ticketResult = requestTicket()
                val ticket = ticketResult.getOrNull()?.ticket
                if (ticket.isNullOrBlank()) {
                    attempt = (attempt + 1).coerceAtMost(6)
                    delay(backoffMillis(attempt))
                    continue
                }

                try {
                    val wsUrl = buildWebSocketUrl(ticket)
                    httpClient.webSocket(urlString = wsUrl) {
                        _connectionState.value = RealtimeConnectionState.CONNECTED
                        attempt = 0
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val envelope = runCatching {
                                    json.decodeFromString(RealtimeEventEnvelope.serializer(), text)
                                }.getOrNull()
                                if (envelope != null) {
                                    if (envelope.seq >= 0L) {
                                        lastSeq = envelope.seq
                                    }
                                    _events.emit(envelope)
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // Exponential backoff reconnect below
                }

                _connectionState.value = RealtimeConnectionState.RECONNECTING
                attempt = (attempt + 1).coerceAtMost(6)
                delay(backoffMillis(attempt))
            }
        }
    }

    fun stop() {
        connectionJob?.cancel()
        connectionJob = null
        _connectionState.value = RealtimeConnectionState.DISCONNECTED
    }

    internal fun backoffMillis(attempt: Int): Long {
        val base = 1_000L shl attempt.coerceIn(0, 5)
        return base.coerceAtMost(30_000L)
    }
}
