package com.azuredevid.cassy.data.repository

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.core.security.DataStoreSessionManager
import com.azuredevid.cassy.domain.error.DomainError
import com.azuredevid.cassy.domain.error.DomainException
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

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

    private fun settings() = object : ServerSettingsRepository {
        override fun getBaseUrl(): String = "https://demo.abbdullaziz.online"
        override fun setBaseUrl(url: String) {}
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    @Test
    fun loginAndBootstrap_persistSessionAndPermissions() = runTest(UnconfinedTestDispatcher()) {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v1/auth/staff/login" -> respond(
                    content = """
                        {
                          "access_token": "jwt-staff-1",
                          "token_type": "Bearer",
                          "access_token_expires_at": "2026-11-13T21:00:00Z",
                          "session_expires_at": "2026-11-14T21:00:00Z",
                          "pin_change_required": false,
                          "staff": {
                            "user_id": "u-1",
                            "full_name": "Siti Kasir",
                            "email": "siti@kopi.id",
                            "role": "owner",
                            "store": {
                              "id": "st-1",
                              "slug": "kopi-senja",
                              "name": "Kopi Senja"
                            }
                          }
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                "/api/v1/mobile/bootstrap" -> {
                    assertEquals("Bearer jwt-staff-1", request.headers[HttpHeaders.Authorization])
                    respond(
                        content = """
                            {
                              "store_id": "st-1",
                              "store_slug": "kopi-senja",
                              "store_name": "Kopi Senja",
                              "permissions": ["store.settings.manage", "order.pos.create"]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else -> respond("{}", HttpStatusCode.NotFound)
            }
        }

        val repo = KtorAuthRepository(client(engine), settings(), sessionManager, Dispatchers.Unconfined)
        val loginRes = repo.login(storeSlug = "kopi-senja", email = "siti@kopi.id", pin = "482913")
        assertTrue(loginRes.isSuccess)
        assertEquals("jwt-staff-1", sessionManager.getAccessToken())

        val bootstrapRes = repo.fetchMobileBootstrap()
        assertTrue(bootstrapRes.isSuccess)
        val stored = sessionManager.currentSession()
        assertNotNull(stored)
        assertEquals(listOf("store.settings.manage", "order.pos.create"), stored?.permissions)

        repo.logout()
        assertNull(sessionManager.getAccessToken())
    }

    @Test
    fun loginWithWrongPin_returnsTypedUnauthorizedDomainError() = runTest(UnconfinedTestDispatcher()) {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        val engine = MockEngine {
            respond(
                content = """{"code":"INVALID_CREDENTIALS","error":"Kode toko, email, atau PIN salah."}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val repo = KtorAuthRepository(client(engine), settings(), sessionManager, Dispatchers.Unconfined)
        val res = repo.login(storeSlug = "kopi-senja", email = "siti@kopi.id", pin = "000000")
        assertTrue(res.isFailure)
        val ex = res.exceptionOrNull() as? DomainException
        assertTrue(ex?.error is DomainError.Unauthorized)
        assertNull(sessionManager.getAccessToken())
    }
}
