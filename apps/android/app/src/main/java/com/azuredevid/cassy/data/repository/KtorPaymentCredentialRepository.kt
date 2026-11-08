package com.azuredevid.cassy.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.azuredevid.cassy.data.model.PaymentCredentialRequest
import com.azuredevid.cassy.data.model.PaymentCredentialResponse
import com.azuredevid.cassy.domain.repository.PaymentCredentialRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Production Ktor implementation of PaymentCredentialRepository.
 * Stores auth session tokens in Android Keystore (EncryptedSharedPreferences)
 * and mutates credentials via PUT /api/v1/store/payment-credentials.
 */
class KtorPaymentCredentialRepository(
    private val context: Context,
    private val apiBaseUrl: String = "https://api.abbdullaziz.online",
    customHttpClient: HttpClient? = null
) : PaymentCredentialRepository {

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val secureStorage by lazy {
        EncryptedSharedPreferences.create(
            context,
            "cassy_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val httpClient: HttpClient = customHttpClient ?: HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    fun getStoredAuthToken(): String? {
        return secureStorage.getString("auth_token", null)
    }

    fun saveAuthToken(token: String) {
        secureStorage.edit().putString("auth_token", token).apply()
    }

    override suspend fun getCredentials(): Result<PaymentCredentialResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val token = getStoredAuthToken()
            val response = httpClient.get("$apiBaseUrl/api/v1/store/payment-credentials") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!response.status.isSuccess()) {
                throw Exception("Gagal memuat kredensial: HTTP ${response.status.value}")
            }
            response.body<PaymentCredentialResponse>()
        }
    }

    override suspend fun saveCredentials(request: PaymentCredentialRequest): Result<PaymentCredentialResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val token = getStoredAuthToken()
            val response = httpClient.put("$apiBaseUrl/api/v1/store/payment-credentials") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception("Gagal menyimpan kredensial: HTTP ${response.status.value}")
            }
            response.body<PaymentCredentialResponse>()
        }
    }
}

