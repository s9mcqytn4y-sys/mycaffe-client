package com.azuredevid.cassy.domain.error

sealed interface DomainError {
    val message: String

    data class Unauthorized(override val message: String) : DomainError
    data class Forbidden(override val message: String) : DomainError
    data class Conflict(override val message: String, val code: String? = null) : DomainError
    data class Validation(override val message: String, val field: String? = null) : DomainError
    data class RateLimited(override val message: String, val retryAfterSeconds: Long? = null) : DomainError
    data class NetworkUnavailable(override val message: String) : DomainError
    data class ServerError(override val message: String, val statusCode: Int? = null) : DomainError
    data class NoStaffSession(override val message: String = "Sesi staf tidak tersedia. Silakan masuk terlebih dahulu.") : DomainError
}

class DomainException(
    val error: DomainError,
    cause: Throwable? = null
) : IllegalStateException(error.message, cause)
