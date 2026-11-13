package com.azuredevid.cassy.data.network

import com.azuredevid.cassy.BuildConfig
import com.azuredevid.cassy.core.security.SessionCookiesStorage
import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import com.azuredevid.cassy.domain.model.StaffIdentity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StaffSessionRefreshDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("access_token_expires_at") val accessTokenExpiresAt: String = "",
    @SerialName("session_expires_at") val sessionExpiresAt: String = "",
    val staff: StaffIdentityDto? = null
)

@Serializable
data class StaffIdentityDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
    val role: String = "",
    val store: StaffStoreDto? = null
) {
    fun toDomain(): StaffIdentity = StaffIdentity(
        userId = userId,
        fullName = fullName,
        email = email,
        role = role,
        storeId = store?.id.orEmpty(),
        storeSlug = store?.slug.orEmpty(),
        storeName = store?.name.orEmpty()
    )
}

@Serializable
data class StaffStoreDto(
    val id: String = "",
    val slug: String = "",
    val name: String = ""
)

object CassyHttpClientFactory {

    val defaultJson: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun create(
        sessionManager: SessionManager,
        serverSettings: ServerSettingsRepository,
        engine: HttpClientEngine? = null
    ): HttpClient {
        val refreshMutex = Mutex()
        val clientEngine = engine ?: OkHttp.create()

        return HttpClient(clientEngine) {
            install(ContentNegotiation) {
                json(defaultJson)
            }

            install(HttpTimeout) {
                connectTimeoutMillis = 10_000L
                requestTimeoutMillis = 20_000L
                socketTimeoutMillis = 20_000L
            }

            install(HttpCookies) {
                storage = SessionCookiesStorage(sessionManager)
            }

            install(WebSockets) {
                pingIntervalMillis = 20_000L
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("CassyHttp", message)
                        }
                    }
                }
                level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
                sanitizeHeader { header ->
                    header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                        header.equals(HttpHeaders.Cookie, ignoreCase = true) ||
                        header.equals(HttpHeaders.SetCookie, ignoreCase = true)
                }
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val token = sessionManager.getAccessToken()
                        if (token.isNullOrBlank()) null else BearerTokens(token, "")
                    }

                    refreshTokens {
                        refreshMutex.withLock {
                            val latestToken = sessionManager.getAccessToken()
                            if (!latestToken.isNullOrBlank() && latestToken != oldTokens?.accessToken) {
                                return@withLock BearerTokens(latestToken, "")
                            }

                            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
                            val response = runCatching {
                                client.post("$baseUrl/api/v1/auth/staff/refresh") {
                                    header("X-CSRF-Protection", "1")
                                    markAsRefreshTokenRequest()
                                }
                            }.getOrNull()

                            if (response != null && response.status.isSuccess()) {
                                val dto = runCatching { response.body<StaffSessionRefreshDto>() }.getOrNull()
                                if (dto != null && dto.accessToken.isNotBlank()) {
                                    sessionManager.updateRefreshedTokens(
                                        accessToken = dto.accessToken,
                                        accessTokenExpiresAt = dto.accessTokenExpiresAt,
                                        sessionExpiresAt = dto.sessionExpiresAt,
                                        staff = dto.staff?.toDomain()
                                    )
                                    return@withLock BearerTokens(dto.accessToken, "")
                                }
                            }

                            if (response == null || response.status.value in listOf(401, 403)) {
                                sessionManager.clearSession(preserveDeviceBinding = true)
                            }
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        val apiHost = runCatching { Url(serverSettings.getBaseUrl()).host }.getOrNull()
                        val targetHost = request.url.host
                        val path = request.url.build().encodedPath
                        apiHost != null &&
                            targetHost.equals(apiHost, ignoreCase = true) &&
                            !path.startsWith("/api/v1/public/") &&
                            !path.endsWith("/auth/staff/login") &&
                            !path.endsWith("/auth/staff/refresh")
                    }
                }
            }
        }
    }
}
