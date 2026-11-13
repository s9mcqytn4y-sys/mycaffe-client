package com.azuredevid.cassy.domain.repository

import com.azuredevid.cassy.domain.model.OfflinePinVerifier
import com.azuredevid.cassy.domain.model.StaffIdentity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobileBootstrapData(
    @SerialName("store_id") val storeId: String = "",
    @SerialName("store_slug") val storeSlug: String = "",
    @SerialName("store_name") val storeName: String = "",
    @SerialName("permissions") val permissions: List<String> = emptyList(),
    @SerialName("app_brand") val appBrand: MobileAppBrandDto? = null,
    @SerialName("pos_layout") val posLayout: MobilePosLayoutDto? = null,
    @SerialName("hardware_config") val hardwareConfig: MobileHardwareConfigDto? = null
)

@Serializable
data class MobileAppBrandDto(
    @SerialName("display_name") val displayName: String = "Cassy",
    @SerialName("monogram_text") val monogramText: String = "CS"
)

@Serializable
data class MobilePosLayoutDto(
    @SerialName("tablet_orientation") val tabletOrientation: String = "landscape_primary",
    @SerialName("auto_lock_seconds") val autoLockSeconds: Int = 300,
    @SerialName("kds_auto_refresh_seconds") val kdsAutoRefreshSeconds: Int = 5,
    @SerialName("kds_sound_alert_enabled") val kdsSoundAlertEnabled: Boolean = true
)

@Serializable
data class MobileHardwareConfigDto(
    @SerialName("cash_drawer_kick_on_cash") val cashDrawerKickOnCash: Boolean = true,
    @SerialName("default_printer_interface") val defaultPrinterInterface: String = "lan_tcp",
    @SerialName("default_printer_port") val defaultPrinterPort: Int = 9100
)

data class AuthLoginResult(
    val staff: StaffIdentity,
    val pinChangeRequired: Boolean,
    val offlinePinVerifier: OfflinePinVerifier?
)

interface AuthRepository {
    suspend fun login(
        storeSlug: String,
        email: String,
        badge: String = "",
        pin: String,
        deviceCredential: String? = null
    ): Result<AuthLoginResult>

    suspend fun fetchMobileBootstrap(): Result<MobileBootstrapData>

    suspend fun fetchMe(): Result<StaffIdentity>

    suspend fun logout(): Result<Unit>
}
