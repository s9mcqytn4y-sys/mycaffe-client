package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.data.model.OrderQuote
import com.azuredevid.cassy.data.model.OrderQuoteRequest
import com.azuredevid.cassy.domain.repository.NoStaffSessionException
import com.azuredevid.cassy.domain.repository.OrderQuoteRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asks the server for subtotal, tax and total. Without a staff token it fails without a request, so the
 * screen shows that totals come from the server instead of guessing them on the device.
 */
class KtorOrderQuoteRepository(
    private val httpClient: HttpClient,
    private val apiBaseUrl: String,
    private val staffToken: () -> String?
) : OrderQuoteRepository {

    override suspend fun quote(request: OrderQuoteRequest): Result<OrderQuote> = withContext(Dispatchers.IO) {
        val token = staffToken() ?: return@withContext Result.failure(NoStaffSessionException())
        runCatching {
            val response = httpClient.post("$apiBaseUrl/api/v1/pos/orders/quote") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            check(response.status.isSuccess()) { "quote failed with HTTP ${response.status.value}" }
            response.body<OrderQuote>()
        }
    }
}
