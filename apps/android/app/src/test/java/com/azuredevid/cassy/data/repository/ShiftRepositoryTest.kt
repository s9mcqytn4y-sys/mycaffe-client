package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.domain.model.ShiftStatus
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ShiftRepositoryTest {

    private class FakeServerSettings(private var url: String = "http://10.0.2.2:8080") : ServerSettingsRepository {
        override fun getBaseUrl(): String = url
        override fun setBaseUrl(url: String) { this.url = url }
    }

    private fun createMockClient(responseJson: String, statusCode: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = responseJson,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }
    }

    @Test
    fun getCurrentShift_whenShiftActive_returnsShiftDomainModel() = runTest {
        val jsonPayload = """
            {
                "id": "shift-1234-uuid",
                "store_id": "store-88-uuid",
                "drawer_id": "drawer-01-uuid",
                "opened_by": "staff-05-uuid",
                "opening_float": 500000,
                "opened_at": "2026-11-13T08:00:00Z",
                "status": "open",
                "expected_cash": 650000
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(jsonPayload),
            serverSettings = FakeServerSettings(),
            authTokenProvider = { "mock-token" },
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.getCurrentShift()
        assertTrue(result.isSuccess)
        val shift = result.getOrNull()
        assertNotNull(shift)
        assertEquals("shift-1234-uuid", shift?.id)
        assertEquals(500000L, shift?.openingFloat)
        assertEquals(650000L, shift?.expectedCash)
        assertEquals(ShiftStatus.OPEN, shift?.status)
    }

    @Test
    fun getCurrentShift_whenNoActiveShift_returnsNullWithoutError() = runTest {
        val conflictJson = """
            {
                "code": "SHIFT_CONFLICT",
                "error": "Sesi shift tidak dalam status terbuka atau data tidak valid."
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(conflictJson, HttpStatusCode.Conflict),
            serverSettings = FakeServerSettings(),
            authTokenProvider = { "mock-token" },
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.getCurrentShift()
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun openShift_success_returnsCreatedShift() = runTest {
        val jsonPayload = """
            {
                "id": "new-shift-uuid",
                "store_id": "store-88-uuid",
                "drawer_id": "drawer-01-uuid",
                "opened_by": "staff-05-uuid",
                "opening_float": 200000,
                "opened_at": "2026-11-13T09:00:00Z",
                "status": "open",
                "notes": "Shift Pagi Kasir 1"
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(jsonPayload, HttpStatusCode.Created),
            serverSettings = FakeServerSettings(),
            authTokenProvider = { "mock-token" },
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.openShift(openingFloat = 200000L, notes = "Shift Pagi Kasir 1")
        assertTrue(result.isSuccess)
        val shift = result.getOrNull()
        assertNotNull(shift)
        assertEquals("new-shift-uuid", shift?.id)
        assertEquals(200000L, shift?.openingFloat)
        assertEquals(ShiftStatus.OPEN, shift?.status)
        assertEquals("Shift Pagi Kasir 1", shift?.notes)
    }

    @Test
    fun recordCashMovement_paidOut_returnsMovement() = runTest {
        val jsonPayload = """
            {
                "id": "movement-01-uuid",
                "store_id": "store-88-uuid",
                "shift_id": "shift-1234-uuid",
                "movement_type": "paid_out",
                "amount": 25000,
                "actor_id": "staff-05-uuid",
                "reason": "Beli es batu kristal darurat",
                "created_at": "2026-11-13T10:15:00Z"
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(jsonPayload, HttpStatusCode.Created),
            serverSettings = FakeServerSettings(),
            authTokenProvider = { "mock-token" },
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.recordCashMovement(
            movementType = CashMovementType.PAID_OUT,
            amount = 25000L,
            reason = "Beli es batu kristal darurat"
        )
        assertTrue(result.isSuccess)
        val movement = result.getOrNull()
        assertNotNull(movement)
        assertEquals(CashMovementType.PAID_OUT, movement?.movementType)
        assertEquals(25000L, movement?.amount)
        assertEquals("Beli es batu kristal darurat", movement?.reason)
    }

    @Test
    fun closeShift_success_returnsZReport() = runTest {
        val jsonPayload = """
            {
                "shift_id": "shift-1234-uuid",
                "store_id": "store-88-uuid",
                "drawer_id": "drawer-01-uuid",
                "drawer_name": "Laci Utama Kasir 1",
                "opened_by": "staff-05-uuid",
                "opened_by_name": "Ahmad Kasir",
                "opened_at": "2026-11-13T08:00:00Z",
                "closed_by": "staff-05-uuid",
                "closed_by_name": "Ahmad Kasir",
                "closed_at": "2026-11-13T16:00:00Z",
                "status": "closed",
                "summary": {
                    "opening_float": 500000,
                    "cash_sales": 750000,
                    "cash_refunds": 0,
                    "refund_count": 0,
                    "paid_outs": 25000,
                    "cash_ins": 0,
                    "tips": 10000,
                    "staff_meals": 0,
                    "no_sale_count": 0,
                    "total_movements": 1
                },
                "expected_cash": 1235000,
                "counted_cash": 1235000,
                "variance": 0,
                "unsettled_open_tabs_count": 0,
                "unsettled_open_tabs_total_amount": 0
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(jsonPayload, HttpStatusCode.OK),
            serverSettings = FakeServerSettings(),
            authTokenProvider = { "mock-token" },
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.closeShift(
            shiftId = "shift-1234-uuid",
            countedCash = 1235000L
        )
        assertTrue(result.isSuccess)
        val report = result.getOrNull()
        assertNotNull(report)
        assertEquals("shift-1234-uuid", report?.shiftId)
        assertEquals(1235000L, report?.expectedCash)
        assertEquals(1235000L, report?.countedCash)
        assertEquals(0L, report?.variance)
        assertEquals(ShiftStatus.CLOSED, report?.status)
    }
}
