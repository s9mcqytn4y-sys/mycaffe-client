package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.data.model.PaymentCredentialInputDto
import com.azuredevid.cassy.data.model.StoreCMSConfigDto
import com.azuredevid.cassy.data.model.StoreCMSConfigInputDto
import com.azuredevid.cassy.data.model.StorePaymentCredentialDto
import com.azuredevid.cassy.data.model.StoreSettingsDto
import com.azuredevid.cassy.data.model.StoreSettingsPatchDto
import com.azuredevid.cassy.data.network.HttpErrorMapper
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
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KtorStoreGovernanceRepository(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StoreGovernanceRepository {

    override suspend fun getPaymentCredentials(): Result<PaymentCredentials> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val response: HttpResponse = httpClient.get("$baseUrl/api/v1/store/payment-credentials")
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memuat kredensial Midtrans.")
                )
            }
            val dto = response.body<StorePaymentCredentialDto>()
            Result.success(dto.toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat kredensial Midtrans."))
        }
    }

    override suspend fun savePaymentCredentials(input: PaymentCredentialInput): Result<PaymentCredentials> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val payload = PaymentCredentialInputDto(
                environment = input.environment.raw,
                merchantId = input.merchantId.trim(),
                clientKey = input.clientKey.trim(),
                serverKey = input.serverKey.trim(),
                isEnabled = input.isEnabled,
                fallbackStaticQrisEnabled = input.fallbackStaticQrisEnabled,
                verifyLive = input.verifyLive
            )

            val response: HttpResponse = httpClient.put("$baseUrl/api/v1/store/payment-credentials") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal menyimpan kredensial Midtrans.")
                )
            }
            val dto = response.body<StorePaymentCredentialDto>()
            Result.success(dto.toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal menyimpan kredensial Midtrans."))
        }
    }

    override suspend fun getStoreCMS(): Result<StoreCMSConfig> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val response: HttpResponse = httpClient.get("$baseUrl/api/v1/store/cms")
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memuat konfigurasi Store CMS.")
                )
            }
            Result.success(response.body<StoreCMSConfigDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat konfigurasi Store CMS."))
        }
    }

    override suspend fun saveStoreCMS(input: StoreCMSConfigInput): Result<StoreCMSConfig> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
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

            val response: HttpResponse = httpClient.put("$baseUrl/api/v1/store/cms") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memperbarui konfigurasi Store CMS.")
                )
            }
            Result.success(response.body<StoreCMSConfigDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memperbarui konfigurasi Store CMS."))
        }
    }

    override suspend fun getStoreSettings(): Result<StoreSettings> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val response: HttpResponse = httpClient.get("$baseUrl/api/v1/store/settings")
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memuat konfigurasi kebijakan gerai.")
                )
            }
            Result.success(response.body<StoreSettingsDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat konfigurasi kebijakan gerai."))
        }
    }

    override suspend fun updateStoreSettings(patch: StoreSettingsPatch): Result<StoreSettings> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val payload = StoreSettingsPatchDto(
                qrServiceMode = patch.qrServiceMode?.raw,
                qrOrderingPaused = patch.qrOrderingPaused,
                tableTimeLimitMinutes = patch.tableTimeLimitMinutes,
                tableTimerWarningMinutes = patch.tableTimerWarningMinutes,
                staticQrisFallbackMode = patch.staticQrisFallbackMode,
                cashierHandlesBar = patch.cashierHandlesBar
            )

            val response: HttpResponse = httpClient.patch("$baseUrl/api/v1/store/settings") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memperbarui kebijakan gerai.")
                )
            }
            Result.success(response.body<StoreSettingsDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memperbarui kebijakan gerai."))
        }
    }

    private fun StorePaymentCredentialDto.toDomain(): PaymentCredentials = PaymentCredentials(
        environment = MidtransEnvironment.fromRaw(environment),
        merchantId = merchantId,
        clientKey = clientKey,
        serverKeyMasked = serverKeyMasked,
        isEnabled = isEnabled,
        fallbackStaticQrisEnabled = fallbackStaticQrisEnabled,
        validationStatus = validationStatus,
        lastVerifiedAt = lastVerifiedAt
    )

    private fun StoreCMSConfigDto.toDomain(): StoreCMSConfig = StoreCMSConfig(
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

    private fun StoreSettingsDto.toDomain(): StoreSettings = StoreSettings(
        qrServiceMode = QRServiceMode.fromRaw(qrServiceMode),
        qrOrderingPaused = qrOrderingPaused,
        tableTimeLimitMinutes = tableTimeLimitMinutes,
        tableTimerWarningMinutes = tableTimerWarningMinutes,
        staticQrisFallbackMode = staticQrisFallbackMode,
        cashierHandlesBar = cashierHandlesBar
    )
}
