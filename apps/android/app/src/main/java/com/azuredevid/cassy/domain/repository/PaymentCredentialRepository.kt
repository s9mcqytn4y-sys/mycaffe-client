package com.azuredevid.cassy.domain.repository

import com.azuredevid.cassy.data.model.PaymentCredentialRequest
import com.azuredevid.cassy.data.model.PaymentCredentialResponse

/**
 * Interface abstraction for Midtrans BYOK payment credentials management.
 * Follows SOLID DIP (Dependency Inversion Principle).
 */
interface PaymentCredentialRepository {
    suspend fun getCredentials(): Result<PaymentCredentialResponse>
    suspend fun saveCredentials(request: PaymentCredentialRequest): Result<PaymentCredentialResponse>
}

