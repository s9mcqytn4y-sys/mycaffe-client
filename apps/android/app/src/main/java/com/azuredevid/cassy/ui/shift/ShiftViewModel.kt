package com.azuredevid.cassy.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.core.hardware.ShiftReportThermalBuilder
import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.domain.error.DomainError
import com.azuredevid.cassy.domain.error.DomainException
import com.azuredevid.cassy.domain.model.CashDrawer
import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.domain.model.ShiftStatus
import com.azuredevid.cassy.domain.model.XReport
import com.azuredevid.cassy.domain.model.ZReport
import com.azuredevid.cassy.domain.repository.PrinterSettingsRepository
import com.azuredevid.cassy.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class ShiftUiState(
    val isLoading: Boolean = false,
    val drawers: List<CashDrawer> = emptyList(),
    val selectedDrawerId: String? = null,
    val currentShift: Shift? = null,
    val isShiftOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Modal controls
    val showOpenShiftModal: Boolean = false,
    val showCloseShiftModal: Boolean = false,
    val showCashMovementModal: Boolean = false,
    val showXReportModal: Boolean = false,

    // Open shift form
    val openingFloatInput: String = "200000",
    val openShiftNotes: String = "",
    val isOpeningShift: Boolean = false,

    // Cash movement form
    val movementType: CashMovementType = CashMovementType.PAID_OUT,
    val movementAmountInput: String = "",
    val movementReason: String = "",
    val isRecordingMovement: Boolean = false,

    // Close shift form
    val countedCashInput: String = "",
    val varianceReason: String = "",
    val closeShiftNotes: String = "",
    val supervisorPinInput: String = "",
    val printReceiptOnClose: Boolean = true,
    val calculatedVariance: Long? = null,
    val isSupervisorAuthRequired: Boolean = false,
    val isClosingShift: Boolean = false,

    // Reports & Printing
    val currentXReport: XReport? = null,
    val isLoadingXReport: Boolean = false,
    val isPrintingReport: Boolean = false,
    val lastZReport: ZReport? = null
)

sealed interface ShiftUiEffect {
    data class ShowToast(val message: String) : ShiftUiEffect
    data object PromptOpenShift : ShiftUiEffect
    data class ShiftOpenedSuccessfully(val shift: Shift) : ShiftUiEffect
    data class ShiftClosedSuccessfully(val report: ZReport) : ShiftUiEffect
}

class ShiftViewModel(
    private val shiftRepository: ShiftRepository,
    private val printerSettings: PrinterSettingsRepository,
    private val sessionManager: SessionManager? = null,
    private val printerManager: PrinterPort9100Manager = PrinterPort9100Manager(),
    private val thermalBuilder: ShiftReportThermalBuilder = ShiftReportThermalBuilder()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftUiState())
    val uiState: StateFlow<ShiftUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ShiftUiEffect>()
    val effects: SharedFlow<ShiftUiEffect> = _effects.asSharedFlow()

    companion object {
        const val VARIANCE_TOLERANCE_RUPIAH = 50_000L
        private const val CODE_VARIANCE_EXCEEDS_TOLERANCE = "VARIANCE_EXCEEDS_TOLERANCE"
    }

    init {
        loadDrawersAndCurrentShift()
    }

    fun loadDrawersAndCurrentShift() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val drawers = shiftRepository.getDrawers().getOrDefault(emptyList())
            val selectedId = _uiState.value.selectedDrawerId
                ?.takeIf { id -> drawers.any { it.id == id } }
                ?: drawers.firstOrNull()?.id

            _uiState.update { it.copy(drawers = drawers, selectedDrawerId = selectedId) }
            fetchCurrentShiftInternal(selectedId)
        }
    }

    fun loadCurrentShift() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            fetchCurrentShiftInternal(_uiState.value.selectedDrawerId)
        }
    }

    private suspend fun fetchCurrentShiftInternal(drawerId: String?) {
        shiftRepository.getCurrentShift(drawerId)
            .onSuccess { shift ->
                val isOpen = shift != null && shift.status == ShiftStatus.OPEN
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentShift = shift,
                        isShiftOpen = isOpen,
                        selectedDrawerId = shift?.drawerId ?: it.selectedDrawerId
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Gagal memeriksa status shift"
                    )
                }
            }
    }

    fun onDrawerSelected(drawerId: String) {
        _uiState.update { it.copy(selectedDrawerId = drawerId) }
        loadCurrentShift()
    }

    fun openShiftModal() {
        viewModelScope.launch {
            if (_uiState.value.drawers.isEmpty()) {
                val drawers = shiftRepository.getDrawers().getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        drawers = drawers,
                        selectedDrawerId = it.selectedDrawerId ?: drawers.firstOrNull()?.id
                    )
                }
            }
            _uiState.update {
                it.copy(
                    showOpenShiftModal = true,
                    openingFloatInput = "200000",
                    openShiftNotes = "",
                    errorMessage = null
                )
            }
        }
    }

    fun closeOpenShiftModal() {
        _uiState.update { it.copy(showOpenShiftModal = false) }
    }

    fun onOpeningFloatChanged(input: String) {
        val sanitized = input.filter { it.isDigit() }
        _uiState.update { it.copy(openingFloatInput = sanitized) }
    }

    fun onOpenShiftNotesChanged(notes: String) {
        _uiState.update { it.copy(openShiftNotes = notes) }
    }

    fun submitOpenShift() {
        val amount = _uiState.value.openingFloatInput.toLongOrNull() ?: 0L
        if (amount < 0L) {
            _uiState.update { it.copy(errorMessage = "Saldo awal tidak boleh negatif") }
            return
        }

        val drawerId = _uiState.value.selectedDrawerId
        val notes = _uiState.value.openShiftNotes

        viewModelScope.launch {
            _uiState.update { it.copy(isOpeningShift = true, errorMessage = null) }
            shiftRepository.openShift(
                openingFloat = amount,
                drawerId = drawerId,
                notes = notes
            ).onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isOpeningShift = false,
                        showOpenShiftModal = false,
                        currentShift = result.shift,
                        isShiftOpen = result.shift.status == ShiftStatus.OPEN,
                        selectedDrawerId = result.shift.drawerId,
                        successMessage = "Shift berhasil dibuka dengan modal awal Rp $amount"
                    )
                }
                if (result.openDrawer) {
                    kickDrawerIfConfigured()
                }
                _effects.emit(ShiftUiEffect.ShiftOpenedSuccessfully(result.shift))
                _effects.emit(ShiftUiEffect.ShowToast("Shift kasir berhasil dibuka"))
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isOpeningShift = false,
                        errorMessage = error.message ?: "Gagal membuka shift"
                    )
                }
            }
        }
    }

    fun openCashMovementModal() {
        if (!_uiState.value.isShiftOpen) {
            viewModelScope.launch {
                _effects.emit(ShiftUiEffect.ShowToast("Buka shift kasir terlebih dahulu"))
                _effects.emit(ShiftUiEffect.PromptOpenShift)
            }
            return
        }
        _uiState.update {
            it.copy(
                showCashMovementModal = true,
                movementType = CashMovementType.PAID_OUT,
                movementAmountInput = "",
                movementReason = "",
                errorMessage = null
            )
        }
    }

    fun closeCashMovementModal() {
        _uiState.update { it.copy(showCashMovementModal = false) }
    }

    fun onMovementTypeChanged(type: CashMovementType) {
        _uiState.update { it.copy(movementType = type) }
    }

    fun onMovementAmountChanged(input: String) {
        val sanitized = input.filter { it.isDigit() }
        _uiState.update { it.copy(movementAmountInput = sanitized) }
    }

    fun onMovementReasonChanged(reason: String) {
        _uiState.update { it.copy(movementReason = reason) }
    }

    fun submitCashMovement() {
        val amount = _uiState.value.movementAmountInput.toLongOrNull() ?: 0L
        val reason = _uiState.value.movementReason.trim()

        if (amount <= 0L) {
            _uiState.update { it.copy(errorMessage = "Nominal kas harus lebih dari Rp 0") }
            return
        }
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Alasan mutasi kas wajib diisi") }
            return
        }

        val type = _uiState.value.movementType
        val shiftId = _uiState.value.currentShift?.id
        val drawerId = _uiState.value.currentShift?.drawerId ?: _uiState.value.selectedDrawerId

        viewModelScope.launch {
            _uiState.update { it.copy(isRecordingMovement = true, errorMessage = null) }
            shiftRepository.recordCashMovement(
                movementType = type,
                amount = amount,
                reason = reason,
                shiftId = shiftId,
                drawerId = drawerId
            ).onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isRecordingMovement = false,
                        showCashMovementModal = false,
                        successMessage = "Mutasi kas berhasil dicatat"
                    )
                }
                if (result.openDrawer) {
                    kickDrawerIfConfigured()
                }
                _effects.emit(ShiftUiEffect.ShowToast("Mutasi kas berhasil disimpan"))
                loadCurrentShift()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRecordingMovement = false,
                        errorMessage = error.message ?: "Gagal mencatat mutasi kas"
                    )
                }
            }
        }
    }

    fun openXReportModal() {
        val shift = _uiState.value.currentShift
        if (!_uiState.value.isShiftOpen || shift == null) {
            viewModelScope.launch {
                _effects.emit(ShiftUiEffect.ShowToast("Tidak ada shift aktif untuk X-Report"))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(showXReportModal = true, isLoadingXReport = true, errorMessage = null) }
            shiftRepository.getXReport(shift.id)
                .onSuccess { report ->
                    _uiState.update {
                        it.copy(
                            isLoadingXReport = false,
                            currentXReport = report
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingXReport = false,
                            showXReportModal = false,
                            errorMessage = error.message ?: "Gagal memuat X-Report"
                        )
                    }
                    _effects.emit(ShiftUiEffect.ShowToast(error.message ?: "Gagal memuat X-Report"))
                }
        }
    }

    fun closeXReportModal() {
        _uiState.update { it.copy(showXReportModal = false) }
    }

    fun printCurrentXReport() {
        val report = _uiState.value.currentXReport ?: return
        viewModelScope.launch {
            val config = printerSettings.getConfig()
            if (!config.hasCashierPrinter) {
                _effects.emit(ShiftUiEffect.ShowToast("Printer kasir belum dikonfigurasi. Atur IP printer di Pengaturan."))
                return@launch
            }
            _uiState.update { it.copy(isPrintingReport = true) }
            val payload = thermalBuilder.buildXReportReceipt(resolveStoreName(), report)
            printerManager.printRawBytes(config.cashierPrinterIp, payload, config.cashierPrinterPort)
                .onSuccess {
                    _uiState.update { it.copy(isPrintingReport = false) }
                    _effects.emit(ShiftUiEffect.ShowToast("X-Report berhasil dicetak"))
                }
                .onFailure {
                    _uiState.update { it.copy(isPrintingReport = false) }
                    _effects.emit(ShiftUiEffect.ShowToast("Gagal mencetak X-Report: printer kasir tidak terjangkau"))
                }
        }
    }

    fun openCloseShiftModal() {
        if (!_uiState.value.isShiftOpen) {
            viewModelScope.launch {
                _effects.emit(ShiftUiEffect.ShowToast("Tidak ada shift aktif untuk ditutup"))
            }
            return
        }
        _uiState.update {
            it.copy(
                showCloseShiftModal = true,
                countedCashInput = "",
                varianceReason = "",
                closeShiftNotes = "",
                supervisorPinInput = "",
                calculatedVariance = null,
                isSupervisorAuthRequired = false,
                errorMessage = null
            )
        }
    }

    fun closeCloseShiftModal() {
        _uiState.update { it.copy(showCloseShiftModal = false) }
    }

    fun onCountedCashChanged(input: String) {
        val sanitized = input.filter { it.isDigit() }
        val counted = sanitized.toLongOrNull()
        val expected = _uiState.value.currentShift?.expectedCash

        // When expectedCash is null (blind count for cashier role), never synthesize variance vs 0.
        val variance = if (counted != null && expected != null) counted - expected else null
        val requiresSupervisor = variance != null && abs(variance) > VARIANCE_TOLERANCE_RUPIAH

        _uiState.update {
            it.copy(
                countedCashInput = sanitized,
                calculatedVariance = variance,
                isSupervisorAuthRequired = requiresSupervisor || it.isSupervisorAuthRequired
            )
        }
    }

    fun onVarianceReasonChanged(reason: String) {
        _uiState.update { it.copy(varianceReason = reason) }
    }

    fun onCloseShiftNotesChanged(notes: String) {
        _uiState.update { it.copy(closeShiftNotes = notes) }
    }

    fun onSupervisorPinChanged(pin: String) {
        val sanitized = pin.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(supervisorPinInput = sanitized) }
    }

    fun onPrintReceiptOnCloseToggled(enabled: Boolean) {
        _uiState.update { it.copy(printReceiptOnClose = enabled) }
    }

    fun submitCloseShift() {
        val counted = _uiState.value.countedCashInput.toLongOrNull()
        if (counted == null) {
            _uiState.update { it.copy(errorMessage = "Masukkan jumlah fisik uang kas riil") }
            return
        }

        val shift = _uiState.value.currentShift
        if (shift == null) {
            _uiState.update { it.copy(errorMessage = "Shift tidak ditemukan") }
            return
        }

        val isSupervisorRequired = _uiState.value.isSupervisorAuthRequired
        val supervisorPin = _uiState.value.supervisorPinInput
        val varianceReason = _uiState.value.varianceReason.trim()

        if (isSupervisorRequired) {
            if (varianceReason.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Selisih kas melebihi toleransi. Alasan wajib diisi") }
                return
            }
            if (supervisorPin.length != 6) {
                _uiState.update { it.copy(errorMessage = "PIN Owner 6 digit wajib diisi untuk verifikasi server") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isClosingShift = true, errorMessage = null) }
            val notes = _uiState.value.closeShiftNotes
            shiftRepository.closeShift(
                shiftId = shift.id,
                countedCash = counted,
                ownerPin = if (isSupervisorRequired || supervisorPin.length == 6) supervisorPin.ifBlank { null } else null,
                varianceReason = varianceReason.ifBlank { null },
                notes = notes
            ).onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isClosingShift = false,
                        showCloseShiftModal = false,
                        currentShift = null,
                        isShiftOpen = false,
                        lastZReport = result.report,
                        successMessage = "Shift berhasil ditutup"
                    )
                }
                if (result.openDrawer) {
                    kickDrawerIfConfigured()
                }
                _effects.emit(ShiftUiEffect.ShiftClosedSuccessfully(result.report))
                _effects.emit(ShiftUiEffect.ShowToast("Shift berhasil ditutup"))

                if (_uiState.value.printReceiptOnClose) {
                    printZReport(result.report)
                }
            }.onFailure { error ->
                val isVarianceGate = error is DomainException &&
                    (error.error as? DomainError.Validation)?.code == CODE_VARIANCE_EXCEEDS_TOLERANCE
                _uiState.update {
                    it.copy(
                        isClosingShift = false,
                        isSupervisorAuthRequired = it.isSupervisorAuthRequired || isVarianceGate,
                        errorMessage = error.message ?: "Gagal menutup shift"
                    )
                }
            }
        }
    }

    fun printZReport(report: ZReport) {
        viewModelScope.launch {
            val config = printerSettings.getConfig()
            if (!config.hasCashierPrinter) {
                _effects.emit(ShiftUiEffect.ShowToast("Printer kasir belum dikonfigurasi. Atur IP printer di Pengaturan."))
                return@launch
            }
            _uiState.update { it.copy(isPrintingReport = true) }
            val payload = thermalBuilder.buildZReportReceipt(resolveStoreName(), report)
            printerManager.printRawBytes(config.cashierPrinterIp, payload, config.cashierPrinterPort)
                .onSuccess {
                    _uiState.update { it.copy(isPrintingReport = false) }
                    _effects.emit(ShiftUiEffect.ShowToast("Struk Z-Report berhasil dicetak"))
                }
                .onFailure {
                    _uiState.update { it.copy(isPrintingReport = false) }
                    _effects.emit(ShiftUiEffect.ShowToast("Gagal mencetak Z-Report: printer kasir tidak terjangkau"))
                }
        }
    }

    private suspend fun kickDrawerIfConfigured() {
        val config = printerSettings.getConfig()
        if (!config.hasCashierPrinter) {
            _effects.emit(ShiftUiEffect.ShowToast("Laci tidak terpicu: IP printer kasir belum dikonfigurasi"))
            return
        }
        printerManager.openCashDrawer(config.cashierPrinterIp, config.cashierPrinterPort)
            .onFailure {
                _effects.emit(ShiftUiEffect.ShowToast("Gagal membuka laci kasir LAN (${config.cashierPrinterIp})"))
            }
    }

    private fun resolveStoreName(): String {
        return sessionManager?.currentSession()?.staff?.storeName
            ?.takeIf { it.isNotBlank() }
            ?: "KASIR POS"
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
