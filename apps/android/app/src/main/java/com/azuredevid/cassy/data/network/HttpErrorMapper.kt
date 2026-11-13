package com.azuredevid.cassy.data.network

import com.azuredevid.cassy.domain.error.DomainError
import com.azuredevid.cassy.domain.error.DomainException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

@Serializable
data class BackendErrorPayload(
    val code: String? = null,
    val error: String? = null,
    val message: String? = null,
    val field: String? = null
)

object HttpErrorMapper {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun toDomainException(response: HttpResponse, fallbackMessage: String): DomainException {
        val rawBody = runCatching { response.bodyAsText() }.getOrDefault("")
        val parsed = runCatching { json.decodeFromString(BackendErrorPayload.serializer(), rawBody) }.getOrNull()
        val message = parsed?.error?.takeIf { it.isNotBlank() }
            ?: parsed?.message?.takeIf { it.isNotBlank() }
            ?: fallbackMessage

        val error = when (response.status) {
            HttpStatusCode.Unauthorized -> DomainError.Unauthorized(message)
            HttpStatusCode.Forbidden -> DomainError.Forbidden(message)
            HttpStatusCode.Conflict -> DomainError.Conflict(message, parsed?.code)
            HttpStatusCode.BadRequest, HttpStatusCode.UnprocessableEntity ->
                DomainError.Validation(message, parsed?.field)
            HttpStatusCode.TooManyRequests -> {
                val retryAfter = response.headers["Retry-After"]?.toLongOrNull()
                DomainError.RateLimited(message, retryAfter)
            }
            else -> DomainError.ServerError(message, response.status.value)
        }
        return DomainException(error)
    }

    fun fromThrowable(throwable: Throwable, fallbackMessage: String): DomainException {
        if (throwable is DomainException) return throwable
        val error = when (throwable) {
            is IOException -> DomainError.NetworkUnavailable("Koneksi ke server terputus. Periksa jaringan Anda.")
            else -> DomainError.ServerError(throwable.message ?: fallbackMessage)
        }
        return DomainException(error, throwable)
    }
}
