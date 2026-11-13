package com.azuredevid.cassy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import com.azuredevid.cassy.domain.model.MidtransEnvironment
import com.azuredevid.cassy.domain.model.PaymentCredentialInput
import com.azuredevid.cassy.domain.model.QRServiceMode
import com.azuredevid.cassy.domain.model.StoreCMSConfigInput
import com.azuredevid.cassy.domain.model.StoreSettingsPatch
import com.azuredevid.cassy.domain.repository.StoreGovernanceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoreSettingsUiState(
    val isLoading: Boolean = false,
    val selectedTabIndex: Int = 0,
    val isSupervisorAuthenticated: Boolean = false,
    val showSupervisorPinModal: Boolean = false,
    val supervisorPinInput: String = "",
    val supervisorPinError: String? = null,

    // Midtrans BYOK Form
    val merchantId: String = "",
    val clientKey: String = "",
    val serverKey: String = "",
    val serverKeyMasked: String = "",
    val isServerKeyVisible: Boolean = false,
    val isMidtransEnabled: Boolean = true,
    val fallbackStaticQris: Boolean = true,
    val probeStatus: String? = null,
    val probeMessage: String? = null,
    val isSavingMidtrans: Boolean = false,

    // Brand CMS Form
    val heroTitleId: String = "Pesan & Bayar dari Meja",
    val heroTitleEn: String = "Order & Pay at Table",
    val heroSubtitleId: String = "Pindai QR, pilih menu, lalu bayar dari meja",
    val heroSubtitleEn: String = "Scan the QR, pick from the menu, and pay at your table",
    val heroBannerUrl: String = "",
    // Empty means no outlet colour: guests see the neutral ink fallback (PRD v4 Q9).
    val primaryHex: String = "",
    val accentHex: String = "",
    val hidePoweredByBadge: Boolean = false,
    val isUploadingBanner: Boolean = false,
    val isSavingCms: Boolean = false,

    // Service Mode & Operational Policies (US4)
    val qrServiceMode: QRServiceMode = QRServiceMode.TABLE,
    val qrOrderingPaused: Boolean = false,
    val tableTimeLimitMinutes: Int = 90,
    val tableTimerWarningMinutes: Int = 15,
    val isSavingSettings: Boolean = false,

    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val isSandbox: Boolean
        get() = serverKey.startsWith("SB-") || clientKey.startsWith("SB-") || serverKeyMasked.startsWith("SB-")

    val isMerchantIdValid: Boolean get() = merchantId.trim().length >= 4
    val isClientKeyValid: Boolean
        get() {
            val key = clientKey.trim()
            if (key.length < 16) return false
            return if (isSandbox) key.startsWith("SB-Mid-client-") else key.startsWith("Mid-client-")
        }
    val isServerKeyValid: Boolean
        get() {
            val key = serverKey.trim()
            if (key.isEmpty() && serverKeyMasked.isNotEmpty()) return true
            if (key.length < 16) return false
            return if (isSandbox) key.startsWith("SB-Mid-server-") else key.startsWith("Mid-server-")
        }
}

sealed interface StoreSettingsUiEffect {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : StoreSettingsUiEffect
}

sealed interface StoreSettingsUiEvent {
    data class SelectTab(val tabIndex: Int) : StoreSettingsUiEvent
    data object RequestSupervisorAuth : StoreSettingsUiEvent
    data class UpdateSupervisorPinInput(val pin: String) : StoreSettingsUiEvent
    data object SubmitSupervisorPin : StoreSettingsUiEvent
    data object DismissSupervisorPinModal : StoreSettingsUiEvent

    // Midtrans Events
    data class UpdateMerchantId(val value: String) : StoreSettingsUiEvent
    data class UpdateClientKey(val value: String) : StoreSettingsUiEvent
    data class UpdateServerKey(val value: String) : StoreSettingsUiEvent
    data object ToggleServerKeyVisibility : StoreSettingsUiEvent
    data class ToggleMidtransEnabled(val enabled: Boolean) : StoreSettingsUiEvent
    data class ToggleFallbackStaticQris(val enabled: Boolean) : StoreSettingsUiEvent
    data object RunLiveProbe : StoreSettingsUiEvent
    data object SaveMidtransCredentials : StoreSettingsUiEvent

    // CMS Events
    data class UpdateHeroTitleId(val value: String) : StoreSettingsUiEvent
    data class UpdateHeroTitleEn(val value: String) : StoreSettingsUiEvent
    data class UpdateHeroSubtitleId(val value: String) : StoreSettingsUiEvent
    data class UpdateHeroSubtitleEn(val value: String) : StoreSettingsUiEvent
    data class UpdatePrimaryHex(val value: String) : StoreSettingsUiEvent
    data class UpdateAccentHex(val value: String) : StoreSettingsUiEvent
    data class ToggleWhiteLabel(val hideBadge: Boolean) : StoreSettingsUiEvent
    data class UploadBanner(val imageBytes: ByteArray) : StoreSettingsUiEvent
    data object SaveStoreCms : StoreSettingsUiEvent

    // Service Mode Events (US4)
    data class SetServiceMode(val mode: QRServiceMode) : StoreSettingsUiEvent
    data class ToggleEmergencyQrPause(val paused: Boolean) : StoreSettingsUiEvent
    data class UpdateTableTimeLimit(val minutes: Int) : StoreSettingsUiEvent
    data object SaveStoreSettings : StoreSettingsUiEvent

    data object DismissMessage : StoreSettingsUiEvent
}

class StoreSettingsViewModel(
    private val repository: StoreGovernanceRepository,
    private val serverSettings: ServerSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StoreSettingsUiState(
            isSupervisorAuthenticated = true
        )
    )
    val uiState: StateFlow<StoreSettingsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<StoreSettingsUiEffect>()
    val effects: SharedFlow<StoreSettingsUiEffect> = _effects.asSharedFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Load Midtrans Credentials
            repository.getPaymentCredentials().onSuccess { creds ->
                _uiState.update { current ->
                    current.copy(
                        merchantId = creds.merchantId,
                        clientKey = creds.clientKey,
                        serverKeyMasked = creds.serverKeyMasked,
                        isMidtransEnabled = creds.isEnabled,
                        fallbackStaticQris = creds.fallbackStaticQrisEnabled
                    )
                }
            }

            // 2. Load Store CMS
            repository.getStoreCMS().onSuccess { cms ->
                _uiState.update { current ->
                    current.copy(
                        heroTitleId = cms.heroTitleId,
                        heroTitleEn = cms.heroTitleEn,
                        heroSubtitleId = cms.heroSubtitleId,
                        heroSubtitleEn = cms.heroSubtitleEn,
                        heroBannerUrl = cms.heroBannerUrl,
                        primaryHex = cms.primaryHex,
                        accentHex = cms.accentHex,
                        hidePoweredByBadge = cms.hidePoweredByBadge
                    )
                }
            }

            // 3. Load Store Settings (US4)
            repository.getStoreSettings().onSuccess { settings ->
                _uiState.update { current ->
                    current.copy(
                        qrServiceMode = settings.qrServiceMode,
                        qrOrderingPaused = settings.qrOrderingPaused,
                        tableTimeLimitMinutes = settings.tableTimeLimitMinutes,
                        tableTimerWarningMinutes = settings.tableTimerWarningMinutes
                    )
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onEvent(event: StoreSettingsUiEvent) {
        when (event) {
            is StoreSettingsUiEvent.SelectTab -> {
                _uiState.update { it.copy(selectedTabIndex = event.tabIndex) }
            }
            StoreSettingsUiEvent.RequestSupervisorAuth,
            is StoreSettingsUiEvent.UpdateSupervisorPinInput,
            StoreSettingsUiEvent.SubmitSupervisorPin,
            StoreSettingsUiEvent.DismissSupervisorPinModal -> {
                // Otorisasi Midtrans BYOK divalidasi oleh RBAC sesi staf di server, bukan PIN lokal perangkat.
                _uiState.update {
                    it.copy(
                        isSupervisorAuthenticated = true,
                        showSupervisorPinModal = false,
                        supervisorPinInput = "",
                        supervisorPinError = null
                    )
                }
            }
            is StoreSettingsUiEvent.UpdateMerchantId -> _uiState.update { it.copy(merchantId = event.value) }
            is StoreSettingsUiEvent.UpdateClientKey -> _uiState.update { it.copy(clientKey = event.value) }
            is StoreSettingsUiEvent.UpdateServerKey -> _uiState.update { it.copy(serverKey = event.value) }
            StoreSettingsUiEvent.ToggleServerKeyVisibility -> {
                _uiState.update { it.copy(isServerKeyVisible = !it.isServerKeyVisible) }
            }
            is StoreSettingsUiEvent.ToggleMidtransEnabled -> _uiState.update { it.copy(isMidtransEnabled = event.enabled) }
            is StoreSettingsUiEvent.ToggleFallbackStaticQris -> _uiState.update { it.copy(fallbackStaticQris = event.enabled) }
            StoreSettingsUiEvent.RunLiveProbe -> runLiveProbe()
            StoreSettingsUiEvent.SaveMidtransCredentials -> saveMidtrans()

            is StoreSettingsUiEvent.UpdateHeroTitleId -> _uiState.update { it.copy(heroTitleId = event.value) }
            is StoreSettingsUiEvent.UpdateHeroTitleEn -> _uiState.update { it.copy(heroTitleEn = event.value) }
            is StoreSettingsUiEvent.UpdateHeroSubtitleId -> _uiState.update { it.copy(heroSubtitleId = event.value) }
            is StoreSettingsUiEvent.UpdateHeroSubtitleEn -> _uiState.update { it.copy(heroSubtitleEn = event.value) }
            is StoreSettingsUiEvent.UpdatePrimaryHex -> _uiState.update { it.copy(primaryHex = event.value) }
            is StoreSettingsUiEvent.UpdateAccentHex -> _uiState.update { it.copy(accentHex = event.value) }
            is StoreSettingsUiEvent.ToggleWhiteLabel -> _uiState.update { it.copy(hidePoweredByBadge = event.hideBadge) }
            is StoreSettingsUiEvent.UploadBanner -> uploadBanner(event.imageBytes)
            StoreSettingsUiEvent.SaveStoreCms -> saveCms()

            is StoreSettingsUiEvent.SetServiceMode -> _uiState.update { it.copy(qrServiceMode = event.mode) }
            is StoreSettingsUiEvent.ToggleEmergencyQrPause -> _uiState.update { it.copy(qrOrderingPaused = event.paused) }
            is StoreSettingsUiEvent.UpdateTableTimeLimit -> _uiState.update { it.copy(tableTimeLimitMinutes = event.minutes) }
            StoreSettingsUiEvent.SaveStoreSettings -> saveSettings()

            StoreSettingsUiEvent.DismissMessage -> {
                _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            }
        }
    }

    private fun runLiveProbe() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(probeStatus = "testing", probeMessage = "Menghubungi endpoint Midtrans...") }

            val env = if (state.isSandbox) "sandbox" else "production"
            val keyToTest = if (state.serverKey.isNotBlank()) state.serverKey else state.serverKeyMasked

            repository.probeMidtransApi(keyToTest, env)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            probeStatus = "success",
                            probeMessage = "Kredensial valid. Respons Midtrans Core API: 200 OK."
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar("Live Probe Midtrans Berhasil"))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            probeStatus = "error",
                            probeMessage = error.message ?: "Uji live probe gagal."
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar("Uji live probe gagal", isError = true))
                }
        }
    }

    private fun saveMidtrans() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSavingMidtrans = true, errorMessage = null) }

            val env = if (state.isSandbox) MidtransEnvironment.SANDBOX else MidtransEnvironment.PRODUCTION
            val input = PaymentCredentialInput(
                environment = env,
                merchantId = state.merchantId,
                clientKey = state.clientKey,
                serverKey = state.serverKey,
                isEnabled = state.isMidtransEnabled,
                fallbackStaticQrisEnabled = state.fallbackStaticQris,
                verifyLive = false
            )

            repository.savePaymentCredentials(input)
                .onSuccess { creds ->
                    _uiState.update {
                        it.copy(
                            isSavingMidtrans = false,
                            serverKeyMasked = creds.serverKeyMasked,
                            serverKey = "",
                            successMessage = "Kredensial Midtrans berhasil disimpan dan terenkripsi."
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar("Kredensial Midtrans tersimpan"))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingMidtrans = false,
                            errorMessage = error.message ?: "Gagal menyimpan kredensial Midtrans"
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar(error.message ?: "Gagal simpan kredensial", isError = true))
                }
        }
    }

    private fun uploadBanner(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingBanner = true, errorMessage = null) }
            repository.uploadHeroBanner(imageBytes)
                .onSuccess { publicUrl ->
                    _uiState.update {
                        it.copy(
                            isUploadingBanner = false,
                            heroBannerUrl = publicUrl,
                            successMessage = "Hero banner WebP berhasil diunggah."
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar("Foto banner berhasil diperbarui"))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isUploadingBanner = false,
                            errorMessage = error.message ?: "Gagal mengunggah foto banner"
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar(error.message ?: "Gagal upload banner", isError = true))
                }
        }
    }

    private fun saveCms() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSavingCms = true, errorMessage = null) }

            val input = StoreCMSConfigInput(
                heroTitleId = state.heroTitleId,
                heroTitleEn = state.heroTitleEn,
                heroSubtitleId = state.heroSubtitleId,
                heroSubtitleEn = state.heroSubtitleEn,
                heroBannerUrl = state.heroBannerUrl,
                primaryHex = state.primaryHex,
                accentHex = state.accentHex,
                hidePoweredByBadge = state.hidePoweredByBadge
            )

            repository.saveStoreCMS(input)
                .onSuccess { cms ->
                    _uiState.update {
                        it.copy(
                            isSavingCms = false,
                            successMessage = "Tampilan Brand CMS WPA berhasil disimpan."
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar("Pengaturan Brand CMS tersimpan"))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingCms = false,
                            errorMessage = error.message ?: "Gagal menyimpan Brand CMS"
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar(error.message ?: "Gagal simpan CMS", isError = true))
                }
        }
    }

    private fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSavingSettings = true, errorMessage = null) }

            val patch = StoreSettingsPatch(
                qrServiceMode = state.qrServiceMode,
                qrOrderingPaused = state.qrOrderingPaused,
                tableTimeLimitMinutes = state.tableTimeLimitMinutes,
                tableTimerWarningMinutes = state.tableTimerWarningMinutes
            )

            repository.updateStoreSettings(patch)
                .onSuccess { settings ->
                    _uiState.update {
                        it.copy(
                            isSavingSettings = false,
                            qrServiceMode = settings.qrServiceMode,
                            qrOrderingPaused = settings.qrOrderingPaused,
                            successMessage = "Kebijakan operasional gerai berhasil diperbarui."
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar("Mode layanan gerai diperbarui"))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingSettings = false,
                            errorMessage = error.message ?: "Gagal memperbarui pengaturan gerai"
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar(error.message ?: "Gagal simpan pengaturan", isError = true))
                }
        }
    }
}

