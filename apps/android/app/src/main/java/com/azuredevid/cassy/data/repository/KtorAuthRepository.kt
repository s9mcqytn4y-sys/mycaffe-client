package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.data.network.HttpErrorMapper
import com.azuredevid.cassy.data.network.StaffIdentityDto
import com.azuredevid.cassy.domain.model.OfflinePinVerifier
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.repository.AuthLoginResult
import com.azuredevid.cassy.domain.repository.AuthRepository
import com.azuredevid.cassy.domain.repository.MobileBootstrapData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class StaffLoginRequestDto(
    @SerialName("store_slug") val storeSlug: String,
    val email: String = "",
    val badge: String = "",
    val pin: String,
    @SerialName("device_credential") val deviceCredential: String = ""
)

@Serializable
private data class OfflinePinVerifierDto(
    val verifier: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    fun toDomain(): OfflinePinVerifier = OfflinePinVerifier(verifier = verifier, updatedAt = updatedAt)
}

@Serializable
private data class StaffSessionResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("access_token_expires_at") val accessTokenExpiresAt: String = "",
    @SerialName("session_expires_at") val sessionExpiresAt: String = "",
    val staff: StaffIdentityDto,
    @SerialName("pin_change_required") val pinChangeRequired: Boolean = false,
    @SerialName("offline_pin_verifier") val offlinePinVerifier: OfflinePinVerifierDto? = null
)

@Serializable
private data class StaffMeResponseDto(
    val staff: StaffIdentityDto,
    val permissions: List<String> = emptyList(),
    @SerialName("session_expires_at") val sessionExpiresAt: String = "",
    @SerialName("pin_change_required") val pinChangeRequired: Boolean = false
)

class KtorAuthRepository(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val sessionManager: SessionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    override suspend fun login(
        storeSlug: String,
        email: String,
        badge: String,
        pin: String,
        deviceCredential: String?
    ): Result<AuthLoginResult> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val effectiveDeviceCred = deviceCredential?.trim().orEmpty()
                .ifBlank { sessionManager.getDeviceCredential().orEmpty() }

            val response = httpClient.post("$baseUrl/api/v1/auth/staff/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    StaffLoginRequestDto(
                        storeSlug = storeSlug.trim().lowercase(),
                        email = email.trim(),
                        badge = badge.trim(),
                        pin = pin.trim(),
                        deviceCredential = effectiveDeviceCred
                    )
                )
            }

            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal masuk. Periksa kode toko, email, dan PIN.")
                )
            }

            val dto = response.body<StaffSessionResponseDto>()
            val staff = dto.staff.toDomain()
            val offlineVerifier = dto.offlinePinVerifier?.toDomain()

            sessionManager.saveLoginSession(
                accessToken = dto.accessToken,
                accessTokenExpiresAt = dto.accessTokenExpiresAt,
                sessionExpiresAt = dto.sessionExpiresAt,
                staff = staff,
                pinChangeRequired = dto.pinChangeRequired,
                deviceCredential = effectiveDeviceCred.ifBlank { null },
                offlinePinVerifier = offlineVerifier
            )

            Result.success(
                AuthLoginResult(
                    staff = staff,
                    pinChangeRequired = dto.pinChangeRequired,
                    offlinePinVerifier = offlineVerifier
                )
            )
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal terhubung ke server autentikasi."))
        }
    }

    override suspend fun fetchMobileBootstrap(): Result<MobileBootstrapData> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val token = sessionManager.getAccessToken()
            val response = httpClient.get("$baseUrl/api/v1/mobile/bootstrap") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memuat konfigurasi gerai dari server.")
                )
            }

            val bootstrap = response.body<MobileBootstrapData>()
            val existing = sessionManager.currentSession()
            if (existing?.staff != null && bootstrap.permissions.isNotEmpty()) {
                sessionManager.updateRefreshedTokens(
                    accessToken = existing.accessToken,
                    accessTokenExpiresAt = existing.accessTokenExpiresAt,
                    sessionExpiresAt = existing.sessionExpiresAt,
                    staff = existing.staff.copy(
                        storeId = bootstrap.storeId.ifBlank { existing.staff.storeId },
                        storeSlug = bootstrap.storeSlug.ifBlank { existing.staff.storeSlug },
                        storeName = bootstrap.storeName.ifBlank { existing.staff.storeName }
                    ),
                    permissions = bootstrap.permissions
                )
            }

            Result.success(bootstrap)
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat konfigurasi gerai."))
        }
    }

    override suspend fun fetchMe(): Result<StaffIdentity> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val token = sessionManager.getAccessToken()
            val response = httpClient.get("$baseUrl/api/v1/auth/staff/me") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Sesi staf tidak valid.")
                )
            }
            val dto = response.body<StaffMeResponseDto>()
            Result.success(dto.staff.toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memverifikasi sesi staf."))
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            runCatching {
                httpClient.post("$baseUrl/api/v1/auth/staff/logout") {
                    header("X-CSRF-Protection", "1")
                }
            }
            sessionManager.clearSession(preserveDeviceBinding = true)
            Result.success(Unit)
        } catch (t: Throwable) {
            sessionManager.clearSession(preserveDeviceBinding = true)
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal keluar dari sesi."))
        }
    }
}
