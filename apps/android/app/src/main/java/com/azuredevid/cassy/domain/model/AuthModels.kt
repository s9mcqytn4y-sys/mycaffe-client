package com.azuredevid.cassy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StaffIdentity(
    val userId: String,
    val fullName: String,
    val email: String = "",
    val role: String,
    val storeId: String,
    val storeSlug: String,
    val storeName: String
)

@Serializable
data class OfflinePinVerifier(
    val verifier: String = "",
    val updatedAt: String = ""
)

@Serializable
data class StoredCookie(
    val name: String,
    val value: String,
    val domain: String? = null,
    val path: String? = null,
    val expiresAtEpochMillis: Long? = null,
    val secure: Boolean = false,
    val httpOnly: Boolean = false
)

@Serializable
data class StoredSession(
    val accessToken: String = "",
    val accessTokenExpiresAt: String = "",
    val sessionExpiresAt: String = "",
    val staff: StaffIdentity? = null,
    val permissions: List<String> = emptyList(),
    val pinChangeRequired: Boolean = false,
    val deviceCredential: String? = null,
    val offlinePinVerifier: OfflinePinVerifier? = null,
    val cookies: List<StoredCookie> = emptyList()
) {
    val isAuthenticated: Boolean
        get() = accessToken.isNotBlank() && staff != null && staff.storeSlug.isNotBlank()
}
