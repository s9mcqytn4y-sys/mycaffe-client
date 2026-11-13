package com.azuredevid.cassy.data.network

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.core.security.DataStoreSessionManager
import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoredSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class CassyHttpClientFactoryTest {

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

    private fun createSettings(url: String = "https://demo.abbdullaziz.online") = object : ServerSettingsRepository {
        override fun getBaseUrl(): String = url
        override fun setBaseUrl(url: String) {}
    }

    @Test
    fun concurrent401Responses_triggerSingleFlightRefreshAndRetryWithCsrfHeader() = runTest(UnconfinedTestDispatcher()) {
        val sessionManager = DataStoreSessionManager(dataStore = InMemorySessionDataStore(), scope = backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "expired-token",
            accessTokenExpiresAt = "2026-11-13T18:00:00Z",
            sessionExpiresAt = "2026-11-14T18:00:00Z",
            staff = StaffIdentity("u1", "Kasir", "kasir@kopi.id", "cashier", "s1", "kopi-senja", "Kopi Senja")
        )

        val refreshCalls = AtomicInteger(0)
        var r2AuthHeader: String? = "NOT_CALLED"

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v1/auth/staff/refresh" -> {
                    assertEquals("1", request.headers["X-CSRF-Protection"])
                    refreshCalls.incrementAndGet()
                    respond(
                        content = """{"access_token":"fresh-token","access_token_expires_at":"2026-11-13T21:00:00Z","session_expires_at":"2026-11-14T21:00:00Z"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                "/api/v1/pos/shifts/current" -> {
                    val auth = request.headers[HttpHeaders.Authorization]
                    if (auth == "Bearer fresh-token") {
                        respond(
                            content = """{"id":"shift-1"}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        )
                    } else {
                        respond(
                            content = """{"code":"SESSION_INVALID","error":"expired"}""",
                            status = HttpStatusCode.Unauthorized,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        )
                    }
                }
                "/presigned/banner.webp" -> {
                    r2AuthHeader = request.headers[HttpHeaders.Authorization]
                    respond(content = "", status = HttpStatusCode.OK)
                }
                else -> respond(content = "{}", status = HttpStatusCode.NotFound)
            }
        }

        val client = CassyHttpClientFactory.create(
            sessionManager = sessionManager,
            serverSettings = createSettings(),
            engine = mockEngine
        )

        val responses = listOf(
            async { client.get("https://demo.abbdullaziz.online/api/v1/pos/shifts/current") },
            async { client.get("https://demo.abbdullaziz.online/api/v1/pos/shifts/current") },
            async { client.get("https://demo.abbdullaziz.online/api/v1/pos/shifts/current") }
        ).awaitAll()

        responses.forEach { assertEquals(HttpStatusCode.OK, it.status) }
        assertEquals(1, refreshCalls.get())
        assertEquals("fresh-token", sessionManager.getAccessToken())

        // Verify presigned R2 upload never receives staff Bearer token
        val putResp = client.put("https://r2.cloudflarestorage.com/presigned/banner.webp") {
            setBody(byteArrayOf(1, 2, 3))
        }
        assertEquals(HttpStatusCode.OK, putResp.status)
        assertNull(r2AuthHeader)
    }

    @Test
    fun failedRefresh_clearsSessionFailClosed() = runTest(UnconfinedTestDispatcher()) {
        val sessionManager = DataStoreSessionManager(dataStore = InMemorySessionDataStore(), scope = backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "stale-token",
            accessTokenExpiresAt = "2026-11-13T18:00:00Z",
            sessionExpiresAt = "2026-11-14T18:00:00Z",
            staff = StaffIdentity("u1", "Kasir", "kasir@kopi.id", "cashier", "s1", "kopi-senja", "Kopi Senja")
        )

        val mockEngine = MockEngine { request ->
            respond(
                content = """{"code":"SESSION_INVALID","error":"expired"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = CassyHttpClientFactory.create(
            sessionManager = sessionManager,
            serverSettings = createSettings(),
            engine = mockEngine
        )

        val response = client.get("https://demo.abbdullaziz.online/api/v1/pos/shifts/current")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertNull(sessionManager.getAccessToken())
    }
}
