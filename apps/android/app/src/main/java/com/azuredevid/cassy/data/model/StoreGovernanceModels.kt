package com.azuredevid.cassy.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StorePaymentCredentialDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("store_id")
    val storeId: String? = null,
    @SerialName("environment")
    val environment: String = "sandbox",
    @SerialName("merchant_id")
    val merchantId: String = "",
    @SerialName("client_key")
    val clientKey: String = "",
    @SerialName("server_key_masked")
    val serverKeyMasked: String = "",
    @SerialName("is_enabled")
    val isEnabled: Boolean = true,
    @SerialName("fallback_static_qris_enabled")
    val fallbackStaticQrisEnabled: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class PaymentCredentialInputDto(
    @SerialName("environment")
    val environment: String,
    @SerialName("merchant_id")
    val merchantId: String,
    @SerialName("client_key")
    val clientKey: String,
    @SerialName("server_key")
    val serverKey: String,
    @SerialName("is_enabled")
    val isEnabled: Boolean = true,
    @SerialName("fallback_static_qris_enabled")
    val fallbackStaticQrisEnabled: Boolean? = true,
    @SerialName("verify_live")
    val verifyLive: Boolean = false
)

@Serializable
data class StoreCMSConfigDto(
    @SerialName("default_locale")
    val defaultLocale: String = "id-ID",
    @SerialName("supported_locales")
    val supportedLocales: List<String> = listOf("id-ID", "en-US"),
    @SerialName("hero_title_id")
    val heroTitleId: String = "Pesan & Bayar dari Meja",
    @SerialName("hero_title_en")
    val heroTitleEn: String = "Order & Pay at Table",
    @SerialName("hero_subtitle_id")
    val heroSubtitleId: String = "Pindai QR, pilih sajian kopi dan makanan favorit Anda",
    @SerialName("hero_subtitle_en")
    val heroSubtitleEn: String = "Scan QR, browse our signature coffee & freshly baked pastry",
    @SerialName("hero_banner_url")
    val heroBannerUrl: String = "",
    @SerialName("announcement_banner_id")
    val announcementBannerId: String = "",
    @SerialName("announcement_banner_en")
    val announcementBannerEn: String = "",
    @SerialName("primary_hex")
    val primaryHex: String = "#2C1A12",
    @SerialName("accent_hex")
    val accentHex: String = "#C68B59",
    @SerialName("high_contrast_mode")
    val highContrastMode: Boolean = false,
    @SerialName("hide_powered_by_badge")
    val hidePoweredByBadge: Boolean = false,
    @SerialName("custom_domain")
    val customDomain: String = ""
)

@Serializable
data class StoreCMSConfigInputDto(
    @SerialName("default_locale")
    val defaultLocale: String,
    @SerialName("supported_locales")
    val supportedLocales: List<String>,
    @SerialName("hero_title_id")
    val heroTitleId: String,
    @SerialName("hero_title_en")
    val heroTitleEn: String,
    @SerialName("hero_subtitle_id")
    val heroSubtitleId: String,
    @SerialName("hero_subtitle_en")
    val heroSubtitleEn: String,
    @SerialName("hero_banner_url")
    val heroBannerUrl: String,
    @SerialName("announcement_banner_id")
    val announcementBannerId: String? = null,
    @SerialName("announcement_banner_en")
    val announcementBannerEn: String? = null,
    @SerialName("primary_hex")
    val primaryHex: String,
    @SerialName("accent_hex")
    val accentHex: String,
    @SerialName("high_contrast_mode")
    val highContrastMode: Boolean = false,
    @SerialName("hide_powered_by_badge")
    val hidePoweredByBadge: Boolean = false,
    @SerialName("custom_domain")
    val customDomain: String = ""
)

@Serializable
data class StoreSettingsDto(
    @SerialName("qr_service_mode")
    val qrServiceMode: String = "table",
    @SerialName("qr_ordering_paused")
    val qrOrderingPaused: Boolean = false,
    @SerialName("table_time_limit_minutes")
    val tableTimeLimitMinutes: Int = 90,
    @SerialName("table_timer_warning_minutes")
    val tableTimerWarningMinutes: Int = 15,
    @SerialName("static_qris_fallback_mode")
    val staticQrisFallbackMode: String = "auto",
    @SerialName("cashier_handles_bar")
    val cashierHandlesBar: Boolean = false,
    @SerialName("outlet_daily_expense_limit")
    val outletDailyExpenseLimit: Long = 1000000L,
    @SerialName("default_opening_float")
    val defaultOpeningFloat: Long = 500000L
)

@Serializable
data class StoreSettingsPatchDto(
    @SerialName("qr_service_mode")
    val qrServiceMode: String? = null,
    @SerialName("qr_ordering_paused")
    val qrOrderingPaused: Boolean? = null,
    @SerialName("table_time_limit_minutes")
    val tableTimeLimitMinutes: Int? = null,
    @SerialName("table_timer_warning_minutes")
    val tableTimerWarningMinutes: Int? = null,
    @SerialName("static_qris_fallback_mode")
    val staticQrisFallbackMode: String? = null,
    @SerialName("cashier_handles_bar")
    val cashierHandlesBar: Boolean? = null
)

@Serializable
data class CreateMediaUploadRequest(
    @SerialName("content_type")
    val contentType: String = "image/webp",
    @SerialName("size_bytes")
    val sizeBytes: Long
)

@Serializable
data class MediaUploadTicketDto(
    @SerialName("id")
    val id: String,
    @SerialName("upload_url")
    val uploadUrl: String,
    @SerialName("expires_at")
    val expiresAt: String? = null
)

@Serializable
data class CompletedMediaUploadDto(
    @SerialName("id")
    val id: String,
    @SerialName("public_url")
    val publicUrl: String
)


