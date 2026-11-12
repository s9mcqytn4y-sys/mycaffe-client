package com.azuredevid.cassy.data.repository

import android.content.Context
import com.azuredevid.cassy.data.model.CompletedMediaUploadDto
import com.azuredevid.cassy.data.model.CreateMediaUploadRequest
import com.azuredevid.cassy.data.model.MediaUploadTicketDto
import com.azuredevid.cassy.data.model.PaymentCredentialInputDto
import com.azuredevid.cassy.data.model.StoreCMSConfigDto
import com.azuredevid.cassy.data.model.StoreCMSConfigInputDto
import com.azuredevid.cassy.data.model.StorePaymentCredentialDto
import com.azuredevid.cassy.data.model.StoreSettingsDto
import com.azuredevid.cassy.data.model.StoreSettingsPatchDto
import com.azuredevid.cassy.domain.model.MidtransEnvironment
import com.azuredevid.cassy.domain.model.PaymentCredentialInput
import com.azuredevid.cassy.domain.model.PaymentCredentials
import com.azuredevid.cassy.domain.model.QRServiceMode
import com.azuredevid.cassy.domain.model.StoreCMSConfig
import com.azuredevid.cassy.domain.model.StoreCMSConfigInput
import com.azuredevid.cassy.domain.model.StoreSettings
import com.azuredevid.cassy.domain.model.StoreSettingsPatch
import com.azuredevid.cassy.domain.repository.StoreGovernanceRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class KtorStoreGovernanceRepository(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val authTokenProvider: () -> String? = { null },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StoreGovernanceRepository {

    constructor(
        context: Context,
        serverSettings: ServerSettingsRepository = SharedPreferencesServerSettingsRepository(context),
        authTokenProvider: () -> String? = { KtorPaymentCredentialRepository(context).getStoredAuthToken() },
        customHttpClient: HttpClient? = null
    ) : this(
        httpClient = customHttpClient ?: HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        },
        serverSettings = serverSettings,
        authTokenProvider = authTokenProvider
    )

    override suspend fun getPaymentCredentials(): Result<PaymentCredentials> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val token = authTokenProvider()
            val response: HttpResponse = httpClient.get("$baseUrl/store/payment-credentials") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    IllegalStateException("Gagal memuat kredensial Midtrans: HTTP ${response.status}")
                )
            }

            val dto = response.body<StorePaymentCredentialDto>()
            Result.success(
                PaymentCredentials(
                    environment = MidtransEnvironment.fromRaw(dto.environment),
                    merchantId = dto.merchantId,
                    clientKey = dto.clientKey,
                    serverKeyMasked = dto.serverKeyMasked,
                    isEnabled = dto.isEnabled,
                    fallbackStaticQrisEnabled = dto.fallbackStaticQrisEnabled
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun savePaymentCredentials(input: PaymentCredentialInput): Result<PaymentCredentials> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val token = authTokenProvider()
            val payload = PaymentCredentialInputDto(
                environment = input.environment.raw,
                merchantId = input.merchantId.trim(),
                clientKey = input.clientKey.trim(),
                serverKey = input.serverKey.trim(),
                isEnabled = input.isEnabled,
                fallbackStaticQrisEnabled = input.fallbackStaticQrisEnabled,
                verifyLive = input.verifyLive
            )

            val response: HttpResponse = httpClient.put("$baseUrl/store/payment-credentials") {
                contentType(ContentType.Application.Json)
                setBody(payload)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    IllegalStateException("Gagal menyimpan kredensial Midtrans: HTTP ${response.status}")
                )
            }

            val dto = response.body<StorePaymentCredentialDto>()
            Result.success(
                PaymentCredentials(
                    environment = MidtransEnvironment.fromRaw(dto.environment),
                    merchantId = dto.merchantId,
                    clientKey = dto.clientKey,
                    serverKeyMasked = dto.serverKeyMasked,
                    isEnabled = dto.isEnabled,
                    fallbackStaticQrisEnabled = dto.fallbackStaticQrisEnabled
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStoreCMS(): Result<StoreCMSConfig> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val token = authTokenProvider()
            val response: HttpResponse = httpClient.get("$baseUrl/store/cms") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    IllegalStateException("Gagal memuat konfigurasi Store CMS: HTTP ${response.status}")
                )
            }

            val dto = response.body<StoreCMSConfigDto>()
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveStoreCMS(input: StoreCMSConfigInput): Result<StoreCMSConfig> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val token = authTokenProvider()
            val payload = StoreCMSConfigInputDto(
                defaultLocale = input.defaultLocale,
                supportedLocales = input.supportedLocales,
                heroTitleId = input.heroTitleId,
                heroTitleEn = input.heroTitleEn,
                heroSubtitleId = input.heroSubtitleId,
                heroSubtitleEn = input.heroSubtitleEn,
                heroBannerUrl = input.heroBannerUrl,
                primaryHex = input.primaryHex,
                accentHex = input.accentHex,
                highContrastMode = input.highContrastMode,
                hidePoweredByBadge = input.hidePoweredByBadge,
                customDomain = input.customDomain
            )

            val response: HttpResponse = httpClient.put("$baseUrl/store/cms") {
                contentType(ContentType.Application.Json)
                setBody(payload)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    IllegalStateException("Gagal memperbarui konfigurasi Store CMS: HTTP ${response.status}")
                )
            }

            val dto = response.body<StoreCMSConfigDto>()
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStoreSettings(): Result<StoreSettings> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val token = authTokenProvider()
            val response: HttpResponse = httpClient.get("$baseUrl/store/settings") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    IllegalStateException("Gagal memuat konfigurasi kebijakan gerai: HTTP ${response.status}")
                )
            }

            val dto = response.body<StoreSettingsDto>()
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStoreSettings(patch: StoreSettingsPatch): Result<StoreSettings> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val token = authTokenProvider()
            val payload = StoreSettingsPatchDto(
                qrServiceMode = patch.qrServiceMode?.raw,
                qrOrderingPaused = patch.qrOrderingPaused,
                tableTimeLimitMinutes = patch.tableTimeLimitMinutes,
                tableTimerWarningMinutes = patch.tableTimerWarningMinutes,
                staticQrisFallbackMode = patch.staticQrisFallbackMode,
                cashierHandlesBar = patch.cashierHandlesBar
            )

            val response: HttpResponse = httpClient.patch("$baseUrl/store/settings") {
                contentType(ContentType.Application.Json)
                setBody(payload)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    IllegalStateException("Gagal memperbarui kebijakan gerai: HTTP ${response.status}")
                )
            }

            val dto = response.body<StoreSettingsDto>()
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadHeroBanner(imageBytes: ByteArray): Result<String> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val token = authTokenProvider()

            // 1. Create upload ticket
            val ticketResponse: HttpResponse = httpClient.post("$baseUrl/media/uploads") {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateMediaUploadRequest(
                        contentType = "image/webp",
                        sizeBytes = imageBytes.size.toLong()
                    )
                )
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!ticketResponse.status.isSuccess()) {
                return@withContext Result.failure(
                    IllegalStateException("Gagal membuat tiket upload banner: HTTP ${ticketResponse.status}")
                )
            }

            val ticket = ticketResponse.body<MediaUploadTicketDto>()

            // 2. Direct PUT binary payload to presigned upload URL
            val directPutResponse: HttpResponse = httpClient.put(ticket.uploadUrl) {
                contentType(ContentType.parse("image/webp"))
                setBody(imageBytes)
            }

            if (!directPutResponse.status.isSuccess()) {
                return@withContext Result.failure(
                    IllegalStateException("Gagal transfer gambar ke storage R2: HTTP ${directPutResponse.status}")
                )
            }

            // 3. Complete upload
            val completeResponse: HttpResponse = httpClient.post("$baseUrl/media/uploads/${ticket.id}/complete") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!completeResponse.status.isSuccess()) {
                return@withContext Result.failure(
                    IllegalStateException("Gagal finalisasi upload: HTTP ${completeResponse.status}")
                )
            }

            val completed = completeResponse.body<CompletedMediaUploadDto>()
            Result.success(completed.publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun probeMidtransApi(serverKey: String, environment: String): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val cleanKey = serverKey.trim()
            if (cleanKey.length < 16) {
                return@withContext Result.failure(IllegalArgumentException("Panjang Server Key minimal 16 karakter."))
            }

            val isSandbox = environment.equals("sandbox", ignoreCase = true) || cleanKey.startsWith("SB-")
            if (isSandbox && !cleanKey.startsWith("SB-Mid-server-")) {
                return@withContext Result.failure(IllegalArgumentException("Format Server Key Sandbox harus diawali 'SB-Mid-server-'."))
            }
            if (!isSandbox && !cleanKey.startsWith("Mid-server-")) {
                return@withContext Result.failure(IllegalArgumentException("Format Server Key Produksi harus diawali 'Mid-server-'."))
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun StoreCMSConfigDto.toDomain(): StoreCMSConfig {
        return StoreCMSConfig(
            defaultLocale = defaultLocale,
            supportedLocales = supportedLocales,
            heroTitleId = heroTitleId,
            heroTitleEn = heroTitleEn,
            heroSubtitleId = heroSubtitleId,
            heroSubtitleEn = heroSubtitleEn,
            heroBannerUrl = heroBannerUrl,
            primaryHex = primaryHex,
            accentHex = accentHex,
            highContrastMode = highContrastMode,
            hidePoweredByBadge = hidePoweredByBadge,
            customDomain = customDomain
        )
    }

    private fun StoreSettingsDto.toDomain(): StoreSettings {
        return StoreSettings(
            qrServiceMode = QRServiceMode.fromRaw(qrServiceMode),
            qrOrderingPaused = qrOrderingPaused,
            tableTimeLimitMinutes = tableTimeLimitMinutes,
            tableTimerWarningMinutes = tableTimerWarningMinutes,
            staticQrisFallbackMode = staticQrisFallbackMode,
            cashierHandlesBar = cashierHandlesBar
        )
    }
}
