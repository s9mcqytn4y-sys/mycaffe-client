package com.azuredevid.cassy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.domain.model.MidtransEnvironment
import com.azuredevid.cassy.domain.model.PaymentCredentialInput
import com.azuredevid.cassy.domain.model.QRServiceMode
import com.azuredevid.cassy.domain.model.StoreCMSConfigInput
import com.azuredevid.cassy.domain.model.StoreSettingsPatch
import com.azuredevid.cassy.domain.repository.PrinterConfig
import com.azuredevid.cassy.domain.repository.PrinterSettingsRepository
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
    val hasManageAccess: Boolean = false,
    val staffRoleLabel: String = "",

    // Midtrans BYOK Form
    val merchantId: String = "",
    val clientKey: String = "",
    val serverKey: String = "",
    val serverKeyMasked: String = "",
    val isServerKeyVisible: Boolean = false,
    val isMidtransEnabled: Boolean = true,
    val fallbackStaticQris: Boolean = true,
    val validationStatus: String = "",
    val lastVerifiedAt: String? = null,
    val isVerifyingMidtrans: Boolean = false,
    val verificationStatus: String? = null,
    val verificationMessage: String? = null,
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
    val isSavingCms: Boolean = false,

    // Service Mode & Operational Policies (US4)
    val qrServiceMode: QRServiceMode = QRServiceMode.TABLE,
    val qrOrderingPaused: Boolean = false,
    val tableTimeLimitMinutes: Int = 90,
    val tableTimerWarningMinutes: Int = 15,
    val isSavingSettings: Boolean = false,

    // Local LAN Printer Config (Port 9100)
    val cashierPrinterIp: String = "",
    val cashierPrinterPort: String = "9100",
    val kitchenPrinterIp: String = "",
    val barPrinterIp: String = "",
    val pastryPrinterIp: String = "",
    val expediterPrinterIp: String = "",
    val paperWidthMm: Int = 80,
    val isTestingPrinter: Boolean = false,
    val printerTestOk: Boolean? = null,
    val printerTestMessage: String? = null,
    val isSavingPrinter: Boolean = false,

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
            if (key.length !in 16..128) return false
            return if (isSandbox) key.startsWith("SB-Mid-server-") else key.startsWith("Mid-server-")
        }

    val isHeroBannerUrlValid: Boolean
        get() {
            val clean = heroBannerUrl.trim()
            return clean.isEmpty() || clean.startsWith("https://", ignoreCase = true)
        }

    val isPrimaryHexValid: Boolean get() = isOptionalHexValid(primaryHex)
    val isAccentHexValid: Boolean get() = isOptionalHexValid(accentHex)

    companion object {
        private val HEX_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")

        fun isOptionalHexValid(value: String): Boolean {
            val clean = value.trim()
            return clean.isEmpty() || HEX_PATTERN.matches(clean)
        }
    }
}

sealed interface StoreSettingsUiEffect {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : StoreSettingsUiEffect
}

sealed interface StoreSettingsUiEvent {
    data class SelectTab(val tabIndex: Int) : StoreSettingsUiEvent

    // Midtrans Events
    data class UpdateMerchantId(val value: String) : StoreSettingsUiEvent
    data class UpdateClientKey(val value: String) : StoreSettingsUiEvent
    data class UpdateServerKey(val value: String) : StoreSettingsUiEvent
    data object ToggleServerKeyVisibility : StoreSettingsUiEvent
    data class ToggleMidtransEnabled(val enabled: Boolean) : StoreSettingsUiEvent
    data class ToggleFallbackStaticQris(val enabled: Boolean) : StoreSettingsUiEvent
    data object VerifyAndSaveMidtrans : StoreSettingsUiEvent
    data object SaveMidtransCredentials : StoreSettingsUiEvent

    // CMS Events
    data class UpdateHeroTitleId(val value: String) : StoreSettingsUiEvent
    data class UpdateHeroTitleEn(val value: String) : StoreSettingsUiEvent
    data class UpdateHeroSubtitleId(val value: String) : StoreSettingsUiEvent
    data class UpdateHeroSubtitleEn(val value: String) : StoreSettingsUiEvent
    data class UpdateHeroBannerUrl(val value: String) : StoreSettingsUiEvent
    data class UpdatePrimaryHex(val value: String) : StoreSettingsUiEvent
    data class UpdateAccentHex(val value: String) : StoreSettingsUiEvent
    data class ToggleWhiteLabel(val hideBadge: Boolean) : StoreSettingsUiEvent
    data object SaveStoreCms : StoreSettingsUiEvent

    // Service Mode Events (US4)
    data class SetServiceMode(val mode: QRServiceMode) : StoreSettingsUiEvent
    data class ToggleEmergencyQrPause(val paused: Boolean) : StoreSettingsUiEvent
    data class UpdateTableTimeLimit(val minutes: Int) : StoreSettingsUiEvent
    data object SaveStoreSettings : StoreSettingsUiEvent

    // Printer LAN Events
    data class UpdateCashierPrinterIp(val value: String) : StoreSettingsUiEvent
    data class UpdateCashierPrinterPort(val value: String) : StoreSettingsUiEvent
    data class UpdateKitchenPrinterIp(val value: String) : StoreSettingsUiEvent
    data class UpdateBarPrinterIp(val value: String) : StoreSettingsUiEvent
    data class UpdatePastryPrinterIp(val value: String) : StoreSettingsUiEvent
    data class UpdateExpediterPrinterIp(val value: String) : StoreSettingsUiEvent
    data class UpdatePaperWidthMm(val widthMm: Int) : StoreSettingsUiEvent
    data object TestCashierPrinter : StoreSettingsUiEvent
    data object SavePrinterConfig : StoreSettingsUiEvent

    data object DismissMessage : StoreSettingsUiEvent
}

class StoreSettingsViewModel(
    private val repository: StoreGovernanceRepository,
    private val sessionManager: SessionManager,
    private val printerSettings: PrinterSettingsRepository,
    private val printerManager: PrinterPort9100Manager = PrinterPort9100Manager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreSettingsUiState())
    val uiState: StateFlow<StoreSettingsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<StoreSettingsUiEffect>()
    val effects: SharedFlow<StoreSettingsUiEffect> = _effects.asSharedFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val config = printerSettings.getConfig()
            val session = sessionManager.awaitLoaded() ?: sessionManager.currentSession()
            val role = session?.staff?.role.orEmpty()
            val canManage = session != null && (
                role.equals("owner", ignoreCase = true) ||
                    session.permissions.any { it == "store.settings.manage" }
                )

            _uiState.update { current ->
                current.copy(
                    hasManageAccess = canManage,
                    staffRoleLabel = role,
                    cashierPrinterIp = config.cashierPrinterIp,
                    cashierPrinterPort = config.cashierPrinterPort.toString(),
                    kitchenPrinterIp = config.kitchenPrinterIp,
                    barPrinterIp = config.barPrinterIp,
                    pastryPrinterIp = config.pastryPrinterIp,
                    expediterPrinterIp = config.expediterPrinterIp,
                    paperWidthMm = config.paperWidthMm
                )
            }

            if (!canManage) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            repository.getPaymentCredentials().onSuccess { creds ->
                _uiState.update { current ->
                    current.copy(
                        merchantId = creds.merchantId,
                        clientKey = creds.clientKey,
                        serverKeyMasked = creds.serverKeyMasked,
                        isMidtransEnabled = creds.isEnabled,
                        fallbackStaticQris = creds.fallbackStaticQrisEnabled,
                        validationStatus = creds.validationStatus,
                        lastVerifiedAt = creds.lastVerifiedAt
                    )
                }
            }

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
            is StoreSettingsUiEvent.SelectTab -> _uiState.update { it.copy(selectedTabIndex = event.tabIndex) }

            is StoreSettingsUiEvent.UpdateMerchantId -> _uiState.update { it.copy(merchantId = event.value) }
            is StoreSettingsUiEvent.UpdateClientKey -> _uiState.update { it.copy(clientKey = event.value) }
            is StoreSettingsUiEvent.UpdateServerKey -> _uiState.update { it.copy(serverKey = event.value) }
            StoreSettingsUiEvent.ToggleServerKeyVisibility -> {
                _uiState.update { it.copy(isServerKeyVisible = !it.isServerKeyVisible) }
            }
            is StoreSettingsUiEvent.ToggleMidtransEnabled -> _uiState.update { it.copy(isMidtransEnabled = event.enabled) }
            is StoreSettingsUiEvent.ToggleFallbackStaticQris -> _uiState.update { it.copy(fallbackStaticQris = event.enabled) }
            StoreSettingsUiEvent.VerifyAndSaveMidtrans -> persistMidtransCredentials(verifyLive = true)
            StoreSettingsUiEvent.SaveMidtransCredentials -> persistMidtransCredentials(verifyLive = false)

            is StoreSettingsUiEvent.UpdateHeroTitleId -> _uiState.update { it.copy(heroTitleId = event.value) }
            is StoreSettingsUiEvent.UpdateHeroTitleEn -> _uiState.update { it.copy(heroTitleEn = event.value) }
            is StoreSettingsUiEvent.UpdateHeroSubtitleId -> _uiState.update { it.copy(heroSubtitleId = event.value) }
            is StoreSettingsUiEvent.UpdateHeroSubtitleEn -> _uiState.update { it.copy(heroSubtitleEn = event.value) }
            is StoreSettingsUiEvent.UpdateHeroBannerUrl -> _uiState.update { it.copy(heroBannerUrl = event.value) }
            is StoreSettingsUiEvent.UpdatePrimaryHex -> _uiState.update { it.copy(primaryHex = event.value) }
            is StoreSettingsUiEvent.UpdateAccentHex -> _uiState.update { it.copy(accentHex = event.value) }
            is StoreSettingsUiEvent.ToggleWhiteLabel -> _uiState.update { it.copy(hidePoweredByBadge = event.hideBadge) }
            StoreSettingsUiEvent.SaveStoreCms -> saveCms()

            is StoreSettingsUiEvent.SetServiceMode -> _uiState.update { it.copy(qrServiceMode = event.mode) }
            is StoreSettingsUiEvent.ToggleEmergencyQrPause -> _uiState.update { it.copy(qrOrderingPaused = event.paused) }
            is StoreSettingsUiEvent.UpdateTableTimeLimit -> _uiState.update { it.copy(tableTimeLimitMinutes = event.minutes) }
            StoreSettingsUiEvent.SaveStoreSettings -> saveSettings()

            is StoreSettingsUiEvent.UpdateCashierPrinterIp -> _uiState.update { it.copy(cashierPrinterIp = event.value) }
            is StoreSettingsUiEvent.UpdateCashierPrinterPort -> {
                val digits = event.value.filter { ch -> ch.isDigit() }
                _uiState.update { it.copy(cashierPrinterPort = digits) }
            }
            is StoreSettingsUiEvent.UpdateKitchenPrinterIp -> _uiState.update { it.copy(kitchenPrinterIp = event.value) }
            is StoreSettingsUiEvent.UpdateBarPrinterIp -> _uiState.update { it.copy(barPrinterIp = event.value) }
            is StoreSettingsUiEvent.UpdatePastryPrinterIp -> _uiState.update { it.copy(pastryPrinterIp = event.value) }
            is StoreSettingsUiEvent.UpdateExpediterPrinterIp -> _uiState.update { it.copy(expediterPrinterIp = event.value) }
            is StoreSettingsUiEvent.UpdatePaperWidthMm -> _uiState.update { it.copy(paperWidthMm = event.widthMm) }
            StoreSettingsUiEvent.TestCashierPrinter -> testCashierPrinter()
            StoreSettingsUiEvent.SavePrinterConfig -> savePrinterConfig()

            StoreSettingsUiEvent.DismissMessage -> {
                _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            }
        }
    }

    private fun persistMidtransCredentials(verifyLive: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.hasManageAccess) {
                val denied = "Akses ditolak: izin kelola gerai diperlukan."
                _uiState.update { it.copy(errorMessage = denied) }
                _effects.emit(StoreSettingsUiEffect.ShowSnackbar(denied, isError = true))
                return@launch
            }

            if (!state.isMerchantIdValid || !state.isClientKeyValid || !state.isServerKeyValid) {
                val msg = "Lengkapi Merchant ID, Client Key, dan Server Key penuh yang valid."
                _uiState.update {
                    it.copy(
                        verificationStatus = if (verifyLive) "error" else it.verificationStatus,
                        verificationMessage = if (verifyLive) msg else it.verificationMessage,
                        errorMessage = msg
                    )
                }
                _effects.emit(StoreSettingsUiEffect.ShowSnackbar(msg, isError = true))
                return@launch
            }

            _uiState.update {
                it.copy(
                    isSavingMidtrans = !verifyLive,
                    isVerifyingMidtrans = verifyLive,
                    verificationStatus = if (verifyLive) "testing" else it.verificationStatus,
                    verificationMessage = if (verifyLive) "Menghubungi Midtrans melalui server..." else it.verificationMessage,
                    errorMessage = null
                )
            }

            val env = if (state.isSandbox) MidtransEnvironment.SANDBOX else MidtransEnvironment.PRODUCTION
            val input = PaymentCredentialInput(
                environment = env,
                merchantId = state.merchantId,
                clientKey = state.clientKey,
                serverKey = state.serverKey,
                isEnabled = state.isMidtransEnabled,
                fallbackStaticQrisEnabled = state.fallbackStaticQris,
                verifyLive = verifyLive
            )

            repository.savePaymentCredentials(input)
                .onSuccess { creds ->
                    val successText = if (verifyLive) {
                        "Kredensial Midtrans terverifikasi live (${creds.validationStatus.ifBlank { "valid" }})."
                    } else {
                        "Kredensial Midtrans tersimpan aman di server."
                    }
                    _uiState.update {
                        it.copy(
                            isSavingMidtrans = false,
                            isVerifyingMidtrans = false,
                            serverKeyMasked = creds.serverKeyMasked,
                            serverKey = "",
                            validationStatus = creds.validationStatus,
                            lastVerifiedAt = creds.lastVerifiedAt,
                            verificationStatus = if (verifyLive) "success" else it.verificationStatus,
                            verificationMessage = if (verifyLive) successText else it.verificationMessage,
                            successMessage = successText
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar(successText))
                }
                .onFailure { error ->
                    val msg = error.message ?: "Gagal menyimpan kredensial Midtrans."
                    _uiState.update {
                        it.copy(
                            isSavingMidtrans = false,
                            isVerifyingMidtrans = false,
                            verificationStatus = if (verifyLive) "error" else it.verificationStatus,
                            verificationMessage = if (verifyLive) msg else it.verificationMessage,
                            errorMessage = msg
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar(msg, isError = true))
                }
        }
    }

    private fun saveCms() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.hasManageAccess) {
                val denied = "Akses ditolak: izin kelola gerai diperlukan."
                _uiState.update { it.copy(errorMessage = denied) }
                _effects.emit(StoreSettingsUiEffect.ShowSnackbar(denied, isError = true))
                return@launch
            }
            if (!state.isHeroBannerUrlValid || !state.isPrimaryHexValid || !state.isAccentHexValid) {
                val msg = "Periksa URL banner HTTPS dan format warna HEX (#RRGGBB)."
                _uiState.update { it.copy(errorMessage = msg) }
                _effects.emit(StoreSettingsUiEffect.ShowSnackbar(msg, isError = true))
                return@launch
            }

            _uiState.update { it.copy(isSavingCms = true, errorMessage = null) }

            val input = StoreCMSConfigInput(
                heroTitleId = state.heroTitleId.trim(),
                heroTitleEn = state.heroTitleEn.trim(),
                heroSubtitleId = state.heroSubtitleId.trim(),
                heroSubtitleEn = state.heroSubtitleEn.trim(),
                heroBannerUrl = state.heroBannerUrl.trim(),
                primaryHex = state.primaryHex.trim(),
                accentHex = state.accentHex.trim(),
                hidePoweredByBadge = state.hidePoweredByBadge
            )

            repository.saveStoreCMS(input)
                .onSuccess {
                    val msg = "Pengaturan tampilan tamu tersimpan."
                    _uiState.update { it.copy(isSavingCms = false, successMessage = msg) }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar(msg))
                }
                .onFailure { error ->
                    val msg = error.message ?: "Gagal menyimpan pengaturan tampilan."
                    _uiState.update { it.copy(isSavingCms = false, errorMessage = msg) }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar(msg, isError = true))
                }
        }
    }

    private fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.hasManageAccess) {
                val denied = "Akses ditolak: izin kelola gerai diperlukan."
                _uiState.update { it.copy(errorMessage = denied) }
                _effects.emit(StoreSettingsUiEffect.ShowSnackbar(denied, isError = true))
                return@launch
            }

            _uiState.update { it.copy(isSavingSettings = true, errorMessage = null) }

            val patch = StoreSettingsPatch(
                qrServiceMode = state.qrServiceMode,
                qrOrderingPaused = state.qrOrderingPaused,
                tableTimeLimitMinutes = state.tableTimeLimitMinutes,
                tableTimerWarningMinutes = state.tableTimerWarningMinutes
            )

            repository.updateStoreSettings(patch)
                .onSuccess { settings ->
                    val msg = "Kebijakan layanan gerai diperbarui."
                    _uiState.update {
                        it.copy(
                            isSavingSettings = false,
                            qrServiceMode = settings.qrServiceMode,
                            qrOrderingPaused = settings.qrOrderingPaused,
                            successMessage = msg
                        )
                    }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar(msg))
                }
                .onFailure { error ->
                    val msg = error.message ?: "Gagal memperbarui kebijakan layanan."
                    _uiState.update { it.copy(isSavingSettings = false, errorMessage = msg) }
                    _effects.emit(StoreSettingsUiEffect.ShowSnackbar(msg, isError = true))
                }
        }
    }

    private fun testCashierPrinter() {
        viewModelScope.launch {
            val state = _uiState.value
            val ip = state.cashierPrinterIp.trim()
            val port = state.cashierPrinterPort.toIntOrNull()?.takeIf { it in 1..65535 } ?: 9100
            if (ip.isBlank()) {
                _uiState.update {
                    it.copy(
                        printerTestOk = false,
                        printerTestMessage = "Masukkan IP printer kasir terlebih dahulu."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isTestingPrinter = true,
                    printerTestOk = null,
                    printerTestMessage = "Memeriksa $ip:$port..."
                )
            }

            val result = printerManager.checkPrinterHealth(ip, port)
            val isReady = result.health == PrinterPort9100Manager.PrinterHealth.READY
            _uiState.update {
                it.copy(
                    isTestingPrinter = false,
                    printerTestOk = isReady,
                    printerTestMessage = result.details
                )
            }
        }
    }

    private fun savePrinterConfig() {
        viewModelScope.launch {
            val state = _uiState.value
            val port = state.cashierPrinterPort.toIntOrNull()?.takeIf { it in 1..65535 } ?: 9100
            _uiState.update { it.copy(isSavingPrinter = true, errorMessage = null) }

            val config = PrinterConfig(
                cashierPrinterIp = state.cashierPrinterIp.trim(),
                cashierPrinterPort = port,
                barPrinterIp = state.barPrinterIp.trim(),
                kitchenPrinterIp = state.kitchenPrinterIp.trim(),
                pastryPrinterIp = state.pastryPrinterIp.trim(),
                expediterPrinterIp = state.expediterPrinterIp.trim(),
                paperWidthMm = state.paperWidthMm
            )
            printerSettings.saveConfig(config)

            val msg = "Pengaturan printer LAN tersimpan."
            _uiState.update {
                it.copy(
                    isSavingPrinter = false,
                    cashierPrinterPort = port.toString(),
                    successMessage = msg
                )
            }
            _effects.emit(StoreSettingsUiEffect.ShowSnackbar(msg))
        }
    }
}
