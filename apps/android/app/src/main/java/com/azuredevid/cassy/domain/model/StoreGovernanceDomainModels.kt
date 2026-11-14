package com.azuredevid.cassy.domain.model

enum class MidtransEnvironment(val raw: String) {
    SANDBOX("sandbox"),
    PRODUCTION("production");

    companion object {
        fun fromRaw(raw: String): MidtransEnvironment =
            entries.find { it.raw.equals(raw, ignoreCase = true) } ?: SANDBOX
    }
}

enum class QRServiceMode(val raw: String) {
    PICKUP("pickup"),
    TABLE("table");

    companion object {
        fun fromRaw(raw: String): QRServiceMode =
            entries.find { it.raw.equals(raw, ignoreCase = true) } ?: TABLE
    }
}

data class PaymentCredentials(
    val environment: MidtransEnvironment,
    val merchantId: String,
    val clientKey: String,
    val serverKeyMasked: String,
    val isEnabled: Boolean,
    val fallbackStaticQrisEnabled: Boolean,
    val validationStatus: String = "",
    val lastVerifiedAt: String? = null
)

data class StoreCMSConfig(
    val defaultLocale: String,
    val supportedLocales: List<String>,
    val heroTitleId: String,
    val heroTitleEn: String,
    val heroSubtitleId: String,
    val heroSubtitleEn: String,
    val heroBannerUrl: String,
    val primaryHex: String,
    val accentHex: String,
    val highContrastMode: Boolean,
    val hidePoweredByBadge: Boolean,
    val customDomain: String
)

data class StoreSettings(
    val qrServiceMode: QRServiceMode,
    val qrOrderingPaused: Boolean,
    val tableTimeLimitMinutes: Int,
    val tableTimerWarningMinutes: Int,
    val staticQrisFallbackMode: String,
    val cashierHandlesBar: Boolean
)

data class PaymentCredentialInput(
    val environment: MidtransEnvironment,
    val merchantId: String,
    val clientKey: String,
    val serverKey: String,
    val isEnabled: Boolean = true,
    val fallbackStaticQrisEnabled: Boolean = true,
    val verifyLive: Boolean = false
)

data class StoreCMSConfigInput(
    val defaultLocale: String = "id-ID",
    val supportedLocales: List<String> = listOf("id-ID", "en-US"),
    val heroTitleId: String,
    val heroTitleEn: String,
    val heroSubtitleId: String,
    val heroSubtitleEn: String,
    val heroBannerUrl: String,
    val primaryHex: String,
    val accentHex: String,
    val highContrastMode: Boolean = false,
    val hidePoweredByBadge: Boolean = false,
    val customDomain: String = ""
)

data class StoreSettingsPatch(
    val qrServiceMode: QRServiceMode? = null,
    val qrOrderingPaused: Boolean? = null,
    val tableTimeLimitMinutes: Int? = null,
    val tableTimerWarningMinutes: Int? = null,
    val staticQrisFallbackMode: String? = null,
    val cashierHandlesBar: Boolean? = null
)

