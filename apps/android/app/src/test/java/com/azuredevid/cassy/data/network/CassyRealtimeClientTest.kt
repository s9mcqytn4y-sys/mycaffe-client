package com.azuredevid.cassy.data.network

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.core.security.DataStoreSessionManager
import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoredSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CassyRealtimeClientTest {

    private class InMemorySessionDataStore(
        initial: StoredSession = StoredSession()
    ) : DataStore<StoredSession> {
        private val state = MutableStateFlow(initial)
        private val mutex = Mutex()
        override val data: Flow<StoredSession> = state
        override suspend fun updateData(transform: suspend (t: StoredSession) -> StoredSession): StoredSession =
            mutex.withLock {
                val updated = transform(state.value)
                state.value = updated
                updated
            }
    }

    private fun settings(url: String = "https://demo.abbdullaziz.online") = object : ServerSettingsRepository {
        override fun getBaseUrl(): String = url
        override fun setBaseUrl(url: String) {}
    }

    @Test
    fun requestTicket_postsToPosWsTicketWithBearerTokenAndBuildsWssUrl() = runTest(UnconfinedTestDispatcher()) {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-pos-1",
            accessTokenExpiresAt = "2026-11-13T21:00:00Z",
            sessionExpiresAt = "2026-11-14T21:00:00Z",
            staff = StaffIdentity("u1", "Kasir", "kasir@kopi.id", "cashier", "s1", "kopi-senja", "Kopi Senja")
        )

        val engine = MockEngine { request ->
            assertEquals("/api/v1/pos/ws-ticket", request.url.encodedPath)
            assertEquals("Bearer jwt-pos-1", request.headers[HttpHeaders.Authorization])
            respond(
                content = """{"ticket":"tk-abc-123","expires_at":"2026-11-13T20:00:30Z","channels":["pos"]}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val realtime = CassyRealtimeClient(httpClient, settings(), sessionManager, Dispatchers.Unconfined)

        val ticketRes = realtime.requestTicket()
        assertTrue(ticketRes.isSuccess)
        assertEquals("tk-abc-123", ticketRes.getOrThrow().ticket)
        assertEquals(
            "wss://demo.abbdullaziz.online/ws?ticket=tk-abc-123",
            realtime.buildWebSocketUrl("tk-abc-123")
        )
    }

    @Test
    fun requestTicket_withoutStaffSession_failsClosed() = runTest(UnconfinedTestDispatcher()) {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        var networkCalls = 0
        val engine = MockEngine { networkCalls++; respond("{}") }
        val httpClient = HttpClient(engine) { install(ContentNegotiation) { json() } }
        val realtime = CassyRealtimeClient(httpClient, settings(), sessionManager, Dispatchers.Unconfined)

        val res = realtime.requestTicket()
        assertTrue(res.isFailure)
        assertEquals(0, networkCalls)
    }
}
