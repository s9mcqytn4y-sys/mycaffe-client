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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    }
                )
            }
        }
    }

    @Test
    fun getDrawers_returnsDrawerList() = runTest {
        val jsonPayload = """
            {
                "drawers": [
                    {
                        "id": "drawer-01-uuid",
                        "store_id": "store-88-uuid",
                        "name": "Laci Kasir 1",
                        "device_name": "Tablet Kasir",
                        "is_active": true
                    }
                ]
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(jsonPayload),
            serverSettings = FakeServerSettings(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.getDrawers()
        assertTrue(result.isSuccess)
        val drawers = result.getOrThrow()
        assertEquals(1, drawers.size)
        assertEquals("drawer-01-uuid", drawers.first().id)
        assertEquals("Laci Kasir 1", drawers.first().name)
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
    fun getCurrentShift_whenNotFoundOrConflict_returnsNullWithoutError() = runTest {
        val conflictJson = """
            {
                "code": "SHIFT_NOT_FOUND",
                "error": "Tidak ada shift terbuka."
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(conflictJson, HttpStatusCode.NotFound),
            serverSettings = FakeServerSettings(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.getCurrentShift()
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun openShift_unwrapsShiftAndOpenDrawerFlag() = runTest {
        val jsonPayload = """
            {
                "shift": {
                    "id": "new-shift-uuid",
                    "store_id": "store-88-uuid",
                    "drawer_id": "drawer-01-uuid",
                    "opened_by": "staff-05-uuid",
                    "opening_float": 200000,
                    "opened_at": "2026-11-13T09:00:00Z",
                    "status": "open",
                    "notes": "Shift Pagi Kasir 1"
                },
                "open_drawer": true
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(jsonPayload, HttpStatusCode.Created),
            serverSettings = FakeServerSettings(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.openShift(openingFloat = 200000L, notes = "Shift Pagi Kasir 1")
        assertTrue(result.isSuccess)
        val openResult = result.getOrThrow()
        assertTrue(openResult.openDrawer)
        assertEquals("new-shift-uuid", openResult.shift.id)
        assertEquals(200000L, openResult.shift.openingFloat)
        assertEquals(ShiftStatus.OPEN, openResult.shift.status)
        assertEquals("Shift Pagi Kasir 1", openResult.shift.notes)
    }

    @Test
    fun recordCashMovement_unwrapsMovementAndOpenDrawerFlag() = runTest {
        val jsonPayload = """
            {
                "movement": {
                    "id": "movement-01-uuid",
                    "store_id": "store-88-uuid",
                    "shift_id": "shift-1234-uuid",
                    "movement_type": "paid_out",
                    "amount": 25000,
                    "actor_id": "staff-05-uuid",
                    "reason": "Beli es batu kristal darurat",
                    "created_at": "2026-11-13T10:15:00Z"
                },
                "open_drawer": false
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(jsonPayload, HttpStatusCode.Created),
            serverSettings = FakeServerSettings(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.recordCashMovement(
            movementType = CashMovementType.PAID_OUT,
            amount = 25000L,
            reason = "Beli es batu kristal darurat"
        )
        assertTrue(result.isSuccess)
        val movementResult = result.getOrThrow()
        assertFalse(movementResult.openDrawer)
        assertEquals(CashMovementType.PAID_OUT, movementResult.movement.movementType)
        assertEquals(25000L, movementResult.movement.amount)
        assertEquals("Beli es batu kristal darurat", movementResult.movement.reason)
    }

    @Test
    fun getXReport_returnsRedactedBlindCountReading() = runTest {
        val jsonPayload = """
            {
                "shift_id": "shift-1234-uuid",
                "store_id": "store-88-uuid",
                "drawer_id": "drawer-01-uuid",
                "drawer_name": "Laci Utama",
                "opened_by": "staff-05-uuid",
                "opened_by_name": "Ahmad Kasir",
                "opened_at": "2026-11-13T08:00:00Z",
                "expected_redacted": true,
                "expected_cash": null,
                "summary": {
                    "opening_float": 500000,
                    "cash_sales": 750000,
                    "cash_refunds": 0,
                    "refund_count": 0,
                    "paid_outs": 25000,
                    "cash_ins": 0,
                    "tips": 0,
                    "staff_meals": 0,
                    "no_sale_count": 0,
                    "total_movements": 1
                }
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(jsonPayload),
            serverSettings = FakeServerSettings(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.getXReport("shift-1234-uuid")
        assertTrue(result.isSuccess)
        val report = result.getOrThrow()
        assertTrue(report.expectedRedacted)
        assertNull(report.expectedCash)
        assertEquals(750000L, report.summary.cashSales)
    }

    @Test
    fun closeShift_unwrapsZReportAndOpenDrawerFlag() = runTest {
        val jsonPayload = """
            {
                "report": {
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
                },
                "open_drawer": true
            }
        """.trimIndent()

        val repository = KtorShiftRepository(
            httpClient = createMockClient(jsonPayload, HttpStatusCode.OK),
            serverSettings = FakeServerSettings(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.closeShift(
            shiftId = "shift-1234-uuid",
            countedCash = 1235000L
        )
        assertTrue(result.isSuccess)
        val closeResult = result.getOrThrow()
        assertTrue(closeResult.openDrawer)
        assertEquals("shift-1234-uuid", closeResult.report.shiftId)
        assertEquals(1235000L, closeResult.report.expectedCash)
        assertEquals(1235000L, closeResult.report.countedCash)
        assertEquals(0L, closeResult.report.variance)
        assertEquals(ShiftStatus.CLOSED, closeResult.report.status)
    }
}
