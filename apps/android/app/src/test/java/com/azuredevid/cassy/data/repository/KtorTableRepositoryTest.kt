package com.azuredevid.cassy.data.repository

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.core.security.DataStoreSessionManager
import com.azuredevid.cassy.data.model.TableInput
import com.azuredevid.cassy.data.network.CassyHttpClientFactory
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoredSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorTableRepositoryTest {

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

    private class FakeServerSettings(private val url: String = "https://api.cassy.test") : ServerSettingsRepository {
        override fun getBaseUrl(): String = url
        override fun setBaseUrl(url: String) = Unit
    }

    @Test
    fun listTables_parsesBoardEntriesWithSessionAndUnpaidTotals() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/pos/tables", request.url.encodedPath)
            assertEquals(HttpMethod.Get, request.method)
            respond(
                content = """
                    {
                      "tables": [
                        {
                          "id": "t-1",
                          "table_number": "01",
                          "label": "Meja Pojok",
                          "section": "Indoor",
                          "capacity": 4,
                          "qr_token": "tok_abc",
                          "qr_rotated_at": "2026-11-14T00:00:00Z",
                          "is_active": true,
                          "status": "occupied",
                          "session": {
                            "id": "s-1",
                            "table_id": "t-1",
                            "status": "open",
                            "opened_at": "2026-11-14T00:00:00Z"
                          },
                          "unpaid_orders": 2,
                          "unpaid_amount": 75000,
                          "timer_state": "ok"
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val sessionManager = DataStoreSessionManager(
            InMemorySessionDataStore(
                StoredSession(
                    accessToken = "jwt",
                    staff = StaffIdentity("u1", "Owner", "o@cassy.id", "owner", "s1", "kopi", "Kopi")
                )
            ),
            backgroundScope
        )
        val client = CassyHttpClientFactory.create(sessionManager, FakeServerSettings(), engine)
        val repo = KtorTableRepository(client, FakeServerSettings())

        val result = repo.listTables()
        assertTrue(result.isSuccess)
        val board = result.getOrThrow()
        assertEquals(1, board.size)
        assertEquals("Meja Pojok", board.single().displayTitle)
        assertEquals("occupied", board.single().status)
        assertEquals(75000L, board.single().unpaidAmount)
    }

    @Test
    fun rotateQr_maps409TableSessionOpenConflict() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/pos/tables/t-1/rotate-qr", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            respond(
                content = """{"code":"TABLE_SESSION_OPEN","error":"Meja masih memiliki sesi aktif."}""",
                status = HttpStatusCode.Conflict,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val sessionManager = DataStoreSessionManager(
            InMemorySessionDataStore(StoredSession(accessToken = "jwt")),
            backgroundScope
        )
        val client = CassyHttpClientFactory.create(sessionManager, FakeServerSettings(), engine)
        val repo = KtorTableRepository(client, FakeServerSettings())

        val result = repo.rotateQr("t-1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("sesi aktif") == true)
    }

    @Test
    fun createTable_postsTableInputAndReturnsRecord() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/pos/tables", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            respond(
                content = """
                    {
                      "id": "t-2",
                      "table_number": "02",
                      "label": "Teras",
                      "section": "Outdoor",
                      "capacity": 2,
                      "qr_token": "tok_xyz",
                      "qr_rotated_at": "2026-11-14T00:00:00Z",
                      "is_active": true
                    }
                """.trimIndent(),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val sessionManager = DataStoreSessionManager(
            InMemorySessionDataStore(StoredSession(accessToken = "jwt")),
            backgroundScope
        )
        val client = CassyHttpClientFactory.create(sessionManager, FakeServerSettings(), engine)
        val repo = KtorTableRepository(client, FakeServerSettings())

        val result = repo.createTable(TableInput(tableNumber = "02", label = "Teras", section = "Outdoor", capacity = 2))
        assertTrue(result.isSuccess)
        assertEquals("02", result.getOrThrow().tableNumber)
        assertEquals("Outdoor", result.getOrThrow().section)
    }
}
