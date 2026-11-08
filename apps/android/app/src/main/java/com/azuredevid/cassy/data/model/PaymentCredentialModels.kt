package com.azuredevid.cassy.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentCredentialRequest(
    @SerialName("environment") val environment: String, // "sandbox" or "production"
    @SerialName("merchant_id") val merchantId: String,
    @SerialName("client_key") val clientKey: String,
    @SerialName("server_key") val serverKey: String,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("fallback_static_qris_enabled") val fallbackStaticQrisEnabled: Boolean = true,
    @SerialName("verify_live") val verifyLive: Boolean = false
)

@Serializable
data class PaymentCredentialResponse(
    @SerialName("store_id") val storeId: String,
    @SerialName("provider") val provider: String = "midtrans",
    @SerialName("environment") val environment: String,
    @SerialName("merchant_id") val merchantId: String,
    @SerialName("client_key") val clientKey: String,
    @SerialName("server_key_masked") val serverKeyMasked: String,
    @SerialName("key_version") val keyVersion: Int = 1,
    @SerialName("validation_status") val validationStatus: String = "verified",
    @SerialName("is_enabled") val isEnabled: Boolean,
    @SerialName("fallback_static_qris_enabled") val fallbackStaticQrisEnabled: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null
)

