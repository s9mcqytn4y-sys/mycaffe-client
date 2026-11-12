package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.domain.model.MidtransEnvironment
import com.azuredevid.cassy.domain.model.PaymentCredentialInput
import com.azuredevid.cassy.domain.model.QRServiceMode
import com.azuredevid.cassy.domain.model.StoreCMSConfigInput
import com.azuredevid.cassy.domain.model.StoreSettingsPatch
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreGovernanceRepositoryTest {

    private class FakeServerSettingsRepository(private var currentUrl: String = "http://10.0.2.2:8080/api/v1") : ServerSettingsRepository {
        var pinRequired: Boolean = true
        var pin: String = "1234"

        override fun isSupervisorPinRequired(): Boolean = pinRequired
        override fun verifySupervisorPin(pin: String): Boolean = pin.trim() == this.pin
        override fun setSupervisorPin(pin: String) { this.pin = pin }
        override fun getBaseUrl(): String = currentUrl
        override fun setBaseUrl(url: String) { this.currentUrl = url }
    }

    private fun createMockClient(responseJson: String, statusCode: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = responseJson,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }
    }

    @Test
    fun getPaymentCredentials_success_returnsDomainModel() = runTest {
        val jsonPayload = """
            {
                "environment": "sandbox",
                "merchant_id": "G357841497",
                "client_key": "SB-Mid-client-sample123456",
                "server_key_masked": "SB-Mid-server-••••••••497",
                "is_enabled": true,
                "fallback_static_qris_enabled": true,
                "created_at": "2026-11-12T10:00:00Z",
                "updated_at": "2026-11-12T10:00:00Z"
            }
        """.trimIndent()

        val repository = KtorStoreGovernanceRepository(
            httpClient = createMockClient(jsonPayload),
            serverSettings = FakeServerSettingsRepository(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.getPaymentCredentials()
        assertTrue(result.isSuccess)
        val creds = result.getOrThrow()
        assertEquals("G357841497", creds.merchantId)
        assertEquals(MidtransEnvironment.SANDBOX, creds.environment)
        assertEquals("SB-Mid-client-sample123456", creds.clientKey)
        assertTrue(creds.isEnabled)
        assertTrue(creds.fallbackStaticQrisEnabled)
    }

    @Test
    fun updatePaymentCredentials_success_returnsUpdatedDomain() = runTest {
        val jsonPayload = """
            {
                "environment": "production",
                "merchant_id": "G357841497",
                "client_key": "Mid-client-livesample123",
                "server_key_masked": "Mid-server-••••••••497",
                "is_enabled": true,
                "fallback_static_qris_enabled": false,
                "created_at": "2026-11-12T10:00:00Z",
                "updated_at": "2026-11-12T11:00:00Z"
            }
        """.trimIndent()

        val repository = KtorStoreGovernanceRepository(
            httpClient = createMockClient(jsonPayload),
            serverSettings = FakeServerSettingsRepository(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val input = PaymentCredentialInput(
            environment = MidtransEnvironment.PRODUCTION,
            merchantId = "G357841497",
            clientKey = "Mid-client-livesample123",
            serverKey = "Mid-server-secret12345678",
            isEnabled = true,
            fallbackStaticQrisEnabled = false,
            verifyLive = false
        )

        val result = repository.savePaymentCredentials(input)
        assertTrue(result.isSuccess)
        val creds = result.getOrThrow()
        assertEquals(MidtransEnvironment.PRODUCTION, creds.environment)
        assertFalse(creds.fallbackStaticQrisEnabled)
    }

    @Test
    fun getStoreCMS_success_mapsAllFieldsCorrectly() = runTest {
        val jsonPayload = """
            {
                "default_locale": "id-ID",
                "supported_locales": ["id-ID", "en-US"],
                "hero_title_id": "Kopi & Roti Segar",
                "hero_title_en": "Fresh Coffee & Pastry",
                "hero_subtitle_id": "Pesan dari meja Anda",
                "hero_subtitle_en": "Order from your table",
                "hero_banner_url": "https://cdn.cassy.pos/banner.webp",
                "primary_hex": "#2C1A12",
                "accent_hex": "#C68B59",
                "high_contrast_mode": true,
                "hide_powered_by_badge": true,
                "custom_domain": "order.kafe.id"
            }
        """.trimIndent()

        val repository = KtorStoreGovernanceRepository(
            httpClient = createMockClient(jsonPayload),
            serverSettings = FakeServerSettingsRepository(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.getStoreCMS()
        assertTrue(result.isSuccess)
        val cms = result.getOrThrow()
        assertEquals("Kopi & Roti Segar", cms.heroTitleId)
        assertEquals("Fresh Coffee & Pastry", cms.heroTitleEn)
        assertEquals("#2C1A12", cms.primaryHex)
        assertTrue(cms.hidePoweredByBadge)
    }

    @Test
    fun getStoreSettings_serviceModeTable_mapsToEnum() = runTest {
        val jsonPayload = """
            {
                "qr_service_mode": "table",
                "qr_ordering_paused": false,
                "table_time_limit_minutes": 90,
                "table_timer_warning_minutes": 15,
                "static_qris_fallback_mode": "manual",
                "cashier_handles_bar": false
            }
        """.trimIndent()

        val repository = KtorStoreGovernanceRepository(
            httpClient = createMockClient(jsonPayload),
            serverSettings = FakeServerSettingsRepository(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = repository.getStoreSettings()
        assertTrue(result.isSuccess)
        val settings = result.getOrThrow()
        assertEquals(QRServiceMode.TABLE, settings.qrServiceMode)
        assertFalse(settings.qrOrderingPaused)
        assertEquals(90, settings.tableTimeLimitMinutes)
    }

    @Test
    fun probeMidtransApi_sandboxKeyValidation_enforcesPrefix() = runTest {
        val repository = KtorStoreGovernanceRepository(
            httpClient = createMockClient("{}"),
            serverSettings = FakeServerSettingsRepository(),
            ioDispatcher = Dispatchers.Unconfined
        )

        // Valid sandbox key
        val validSandbox = repository.probeMidtransApi("SB-Mid-server-testkey12345", "sandbox")
        assertTrue(validSandbox.isSuccess)

        // Invalid prefix for sandbox
        val invalidSandbox = repository.probeMidtransApi("Mid-server-testkey12345", "sandbox")
        assertTrue(invalidSandbox.isFailure)

        // Valid production key
        val validProd = repository.probeMidtransApi("Mid-server-prodkey123456", "production")
        assertTrue(validProd.isSuccess)
    }
}

