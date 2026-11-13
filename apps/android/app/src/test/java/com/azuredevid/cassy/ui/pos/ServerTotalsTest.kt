package com.azuredevid.cassy.ui.pos

import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.repository.KtorOrderQuoteRepository
import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import com.azuredevid.cassy.domain.repository.NoStaffSessionException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The POS shows only server-priced totals; it never computes tax on the device (design critique 2026-11).
class ServerTotalsTest {
    private val latte = CatalogProduct("11111111-0000-4000-8000-000000000001", "Latte", "c1", "Kopi", "", 32000, "", "bar")

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Test
    fun requestUsesApiOrderTypesAndQuantities() {
        val req = quoteRequestOf(listOf(CartItem(latte, quantity = 2)), "DINE-IN")
        assertEquals("dine_in", req.orderType)
        assertEquals(latte.id, req.items.single().productId)
        assertEquals(2, req.items.single().quantity)
        assertEquals("takeaway", quoteRequestOf(emptyList(), "TAKEAWAY").orderType)
    }

    private fun settings(url: String = "https://api.test") = object : ServerSettingsRepository {
        override fun getBaseUrl(): String = url
        override fun setBaseUrl(url: String) {}
    }

    @Test
    fun withoutStaffSessionNoRequestIsSentAndNoTotalExists() = runTest {
        var calls = 0
        val engine = MockEngine { calls++; respond("{}") }
        val repo = KtorOrderQuoteRepository(client(engine), settings(), staffToken = { null })
        val result = repo.quote(quoteRequestOf(listOf(CartItem(latte)), "DINE-IN"))
        assertTrue(result.exceptionOrNull() is NoStaffSessionException)
        assertEquals(0, calls)
    }

    @Test
    fun totalsComeFromTheServerAsWholeRupiah() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://api.test/api/v1/pos/orders/quote", request.url.toString())
            assertEquals("Bearer t0k", request.headers[HttpHeaders.Authorization])
            val body = String(request.body.toByteArray())
            assertTrue(body.contains("\"order_type\":\"dine_in\""))
            respond(
                """{"subtotal_amount":64000,"service_charge_amount":3200,"tax_amount":6720,"total_amount":73920,"quote_fingerprint":"fp"}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val quote = KtorOrderQuoteRepository(client(engine), settings(), staffToken = { "t0k" })
            .quote(quoteRequestOf(listOf(CartItem(latte, quantity = 2)), "DINE-IN")).getOrThrow()
        assertEquals(64000L, quote.subtotalAmount)
        assertEquals(6720L, quote.taxAmount)
        assertEquals(73920L, quote.totalAmount)
    }

    @Test
    fun serverErrorGivesNoTotal() = runTest {
        val engine = MockEngine { respond("{}", HttpStatusCode.Unauthorized) }
        val result = KtorOrderQuoteRepository(client(engine), settings(), staffToken = { "expired" })
            .quote(quoteRequestOf(listOf(CartItem(latte)), "TAKEAWAY"))
        assertTrue(result.isFailure)
    }
}
