package com.azuredevid.cassy.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuredevid.cassy.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class StorePairingUiState(
    val storeSlug: String = "",
    val email: String = "",
    val badge: String = "",
    val pin: String = "",
    val deviceCredential: String = "",
    val isCameraActive: Boolean = false,
    val isSubmitting: Boolean = false,
    val qrScannedInfo: String? = null,
    val storeSlugError: String? = null,
    val emailError: String? = null,
    val pinError: String? = null,
    val formError: String? = null
)

sealed interface StorePairingUiEvent {
    data class UpdateStoreSlug(val value: String) : StorePairingUiEvent
    data class UpdateEmail(val value: String) : StorePairingUiEvent
    data class UpdatePin(val value: String) : StorePairingUiEvent
    data class ToggleCamera(val active: Boolean) : StorePairingUiEvent
    data class HandleQrPayload(val rawPayload: String) : StorePairingUiEvent
    data object SubmitLogin : StorePairingUiEvent
}

sealed interface StorePairingUiEffect {
    data class PairingSucceeded(val storeSlug: String, val storeName: String) : StorePairingUiEffect
}

class StorePairingViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorePairingUiState())
    val uiState: StateFlow<StorePairingUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<StorePairingUiEffect>()
    val effects: SharedFlow<StorePairingUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: StorePairingUiEvent) {
        when (event) {
            is StorePairingUiEvent.UpdateStoreSlug -> _uiState.update {
                it.copy(storeSlug = event.value, storeSlugError = null, formError = null)
            }
            is StorePairingUiEvent.UpdateEmail -> _uiState.update {
                it.copy(email = event.value, emailError = null, formError = null)
            }
            is StorePairingUiEvent.UpdatePin -> {
                val digits = event.value.filter { ch -> ch.isDigit() }.take(6)
                _uiState.update { it.copy(pin = digits, pinError = null, formError = null) }
            }
            is StorePairingUiEvent.ToggleCamera -> _uiState.update {
                it.copy(isCameraActive = event.active, formError = null)
            }
            is StorePairingUiEvent.HandleQrPayload -> parseQrPayload(event.rawPayload)
            StorePairingUiEvent.SubmitLogin -> submitLogin()
        }
    }

    private fun parseQrPayload(rawPayload: String) {
        val trimmed = rawPayload.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(formError = "QR Code kosong atau tidak terbaca.") }
            return
        }

        val parsed = runCatching {
            val json = Json.parseToJsonElement(trimmed).jsonObject
            val slug = json.stringField("store_slug", "storeSlug")
            val email = json.stringField("email")
            val badge = json.stringField("badge")
            val deviceCred = json.stringField("device_credential", "pairing_token")
            Triple(slug, Pair(email, badge), deviceCred)
        }.getOrNull()

        if (parsed != null && (parsed.first.isNotBlank() || parsed.third.isNotBlank() || parsed.second.second.isNotBlank())) {
            _uiState.update { current ->
                current.copy(
                    storeSlug = parsed.first.ifBlank { current.storeSlug },
                    email = parsed.second.first.ifBlank { current.email },
                    badge = parsed.second.second.ifBlank { current.badge },
                    deviceCredential = parsed.third.ifBlank { current.deviceCredential },
                    isCameraActive = false,
                    qrScannedInfo = "QR terdeteksi. Masukkan PIN 6 digit staf untuk masuk.",
                    storeSlugError = null,
                    emailError = null,
                    formError = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isCameraActive = false,
                    formError = "Format QR tidak valid. Pindai QR perangkat/badge resmi dari Dashboard Pemilik."
                )
            }
        }
    }

    private fun submitLogin() {
        val state = _uiState.value
        val slug = state.storeSlug.trim().lowercase()
        val email = state.email.trim()
        val badge = state.badge.trim()
        val pin = state.pin.trim()

        var slugErr: String? = null
        var emailErr: String? = null
        var pinErr: String? = null

        if (slug.isBlank() && badge.isBlank()) {
            slugErr = "Kode toko wajib diisi."
        }
        if (email.isBlank() && badge.isBlank()) {
            emailErr = "Email staf wajib diisi."
        }
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            pinErr = "PIN staf wajib 6 digit angka."
        }

        if (slugErr != null || emailErr != null || pinErr != null) {
            _uiState.update {
                it.copy(
                    storeSlugError = slugErr,
                    emailError = emailErr,
                    pinError = pinErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, formError = null) }
            val loginResult = authRepository.login(
                storeSlug = slug,
                email = email,
                badge = badge,
                pin = pin,
                deviceCredential = state.deviceCredential.ifBlank { null }
            )

            loginResult.fold(
                onSuccess = { auth ->
                    val bootstrap = authRepository.fetchMobileBootstrap()
                    val resolvedSlug = bootstrap.getOrNull()?.storeSlug?.ifBlank { auth.staff.storeSlug }
                        ?: auth.staff.storeSlug
                    val resolvedName = bootstrap.getOrNull()?.storeName?.ifBlank { auth.staff.storeName }
                        ?: auth.staff.storeName

                    _uiState.update { it.copy(isSubmitting = false, pin = "") }
                    _effects.emit(StorePairingUiEffect.PairingSucceeded(resolvedSlug, resolvedName))
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            pin = "",
                            formError = err.message ?: "Autentikasi staf gagal."
                        )
                    }
                }
            )
        }
    }

    private fun JsonObject.stringField(vararg keys: String): String {
        for (key in keys) {
            val raw = this[key]?.jsonPrimitive?.contentOrNull?.trim()
            if (!raw.isNullOrEmpty()) return raw
        }
        return ""
    }
}
