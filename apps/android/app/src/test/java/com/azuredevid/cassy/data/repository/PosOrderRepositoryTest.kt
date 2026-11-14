package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.domain.model.CreatePosOrderInput
import com.azuredevid.cassy.domain.model.CreatePosOrderItemInput
import com.azuredevid.cassy.domain.model.InitiatePosPaymentInput
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
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
import org.junit.Assert.assertTrue
import org.junit.Test

class PosOrderRepositoryTest {

    private class FakeServerSettings(private var url: String = "https://api.test") : ServerSettingsRepository {
        override fun getBaseUrl(): String = url
        override fun setBaseUrl(url: String) { this.url = url }
    }

    private fun createClient(
        responseJson: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        onBody: ((String, String) -> Unit)? = null
    ): HttpClient {
        val engine = MockEngine { request ->
            onBody?.invoke(request.url.toString(), String(request.body.toByteArray()))
            respond(
                content = responseJson,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                )
            }
        }
    }

    @Test
    fun createOrder_unwrapsCreatedOrderEnvelope() = runTest {
        val responseJson = """
            {
                "order": {
                    "id": "ord-uuid-1",
                    "order_number": "CASSY-0042",
                    "table_number": "Meja 04",
                    "order_type": "dine_in",
                    "billing_mode": "pay_first",
                    "status": "pending",
                    "payment_status": "unpaid",
                    "subtotal_amount": 32000,
                    "tax_amount": 3200,
                    "service_charge_amount": 1600,
                    "total_amount": 36800
                }
            }
        """.trimIndent()

        var requestedUrl = ""
        var sentBody = ""
        val repo = KtorPosOrderRepository(
            httpClient = createClient(responseJson, HttpStatusCode.Created) { url, body ->
                requestedUrl = url
                sentBody = body
            },
            serverSettings = FakeServerSettings(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repo.createOrder(
            CreatePosOrderInput(
                idempotencyKey = "idem-1",
                tableNumber = "Meja 04",
                customerName = "Meja 04",
                orderType = "dine_in",
                expectedTotal = 36800L,
                items = listOf(CreatePosOrderItemInput(productId = "prod-1", quantity = 1))
            )
        )

        assertTrue(result.isSuccess)
        assertEquals("https://api.test/api/v1/pos/orders", requestedUrl)
        assertTrue(sentBody.contains("\"expected_total\":36800"))
        val order = result.getOrThrow()
        assertEquals("ord-uuid-1", order.id)
        assertEquals("CASSY-0042", order.orderNumber)
        assertEquals(36800L, order.totalAmount)
    }

    @Test
    fun initiatePayment_unwrapsPaymentAttemptAndOpenDrawer() = runTest {
        val responseJson = """
            {
                "is_idempotent_replay": false,
                "open_drawer": true,
                "payment": {
                    "id": "pay-1",
                    "provider_order_id": "CASSY-0042-CASH",
                    "method": "cash",
                    "status": "settled",
                    "amount": 36800,
                    "cash_tendered_amount": 50000,
                    "rounding_amount": 0,
                    "change_given_amount": 13200
                },
                "order": {
                    "id": "ord-uuid-1",
                    "order_number": "CASSY-0042",
                    "payment_status": "paid",
                    "total_amount": 36800,
                    "paid_amount": 36800
                }
            }
        """.trimIndent()

        var requestedUrl = ""
        val repo = KtorPosOrderRepository(
            httpClient = createClient(responseJson, HttpStatusCode.Created) { url, _ -> requestedUrl = url },
            serverSettings = FakeServerSettings(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repo.initiatePayment(
            InitiatePosPaymentInput(
                orderId = "ord-uuid-1",
                idempotencyKey = "pay-idem-1",
                method = "cash",
                cashTenderedAmount = 50000L,
                drawerId = "drawer-1"
            )
        )

        assertTrue(result.isSuccess)
        assertEquals("https://api.test/api/v1/pos/orders/ord-uuid-1/payments", requestedUrl)
        val paymentRes = result.getOrThrow()
        assertTrue(paymentRes.openDrawer)
        assertEquals(13200L, paymentRes.payment.changeGivenAmount)
        assertEquals("paid", paymentRes.order?.paymentStatus)
    }

    @Test
    fun getOrder_returnsOrderDetailAndPayments() = runTest {
        val responseJson = """
            {
                "order": {
                    "id": "ord-uuid-1",
                    "order_number": "CASSY-0042",
                    "payment_status": "paid",
                    "total_amount": 36800
                },
                "payments": [
                    {
                        "id": "pay-qris",
                        "method": "qris",
                        "status": "settled",
                        "amount": 36800
                    }
                ]
            }
        """.trimIndent()

        val repo = KtorPosOrderRepository(
            httpClient = createClient(responseJson),
            serverSettings = FakeServerSettings(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repo.getOrder("ord-uuid-1")
        assertTrue(result.isSuccess)
        val detail = result.getOrThrow()
        assertEquals("paid", detail.order.paymentStatus)
        assertEquals("settled", detail.payments.single().status)
    }
}
