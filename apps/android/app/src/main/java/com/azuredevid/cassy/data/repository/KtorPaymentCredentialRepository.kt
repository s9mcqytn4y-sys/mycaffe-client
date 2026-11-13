package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.data.model.PaymentCredentialRequest
import com.azuredevid.cassy.data.model.PaymentCredentialResponse
import com.azuredevid.cassy.data.network.HttpErrorMapper
import com.azuredevid.cassy.domain.error.DomainError
import com.azuredevid.cassy.domain.error.DomainException
import com.azuredevid.cassy.domain.repository.PaymentCredentialRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KtorPaymentCredentialRepository(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val sessionManager: SessionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PaymentCredentialRepository {

    override suspend fun getCredentials(): Result<PaymentCredentialResponse> = withContext(ioDispatcher) {
        val token = sessionManager.getAccessToken()
            ?: return@withContext Result.failure(DomainException(DomainError.NoStaffSession()))

        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.get("$baseUrl/api/v1/store/payment-credentials") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memuat kredensial pembayaran.")
                )
            }
            Result.success(response.body<PaymentCredentialResponse>())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat kredensial pembayaran."))
        }
    }

    override suspend fun saveCredentials(request: PaymentCredentialRequest): Result<PaymentCredentialResponse> = withContext(ioDispatcher) {
        val token = sessionManager.getAccessToken()
            ?: return@withContext Result.failure(DomainException(DomainError.NoStaffSession()))

        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.put("$baseUrl/api/v1/store/payment-credentials") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal menyimpan kredensial pembayaran.")
                )
            }
            Result.success(response.body<PaymentCredentialResponse>())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal menyimpan kredensial pembayaran."))
        }
    }
}
