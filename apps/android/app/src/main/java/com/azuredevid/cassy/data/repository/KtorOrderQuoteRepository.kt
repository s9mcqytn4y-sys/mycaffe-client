package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.data.model.OrderQuote
import com.azuredevid.cassy.data.model.OrderQuoteRequest
import com.azuredevid.cassy.data.network.HttpErrorMapper
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asks the server for subtotal, tax and total. Without a staff token it fails closed without a request,
 * so totals always come from the server instead of being guessed on the device.
 */
class KtorOrderQuoteRepository(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val staffToken: () -> String?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OrderQuoteRepository {

    override suspend fun quote(request: OrderQuoteRequest): Result<OrderQuote> = withContext(ioDispatcher) {
        val token = staffToken() ?: return@withContext Result.failure(NoStaffSessionException())
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.post("$baseUrl/api/v1/pos/orders/quote") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal menghitung total pesanan dari server.")
                )
            }
            Result.success(response.body<OrderQuote>())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal menghitung total pesanan dari server."))
        }
    }
}
