package com.azuredevid.cassy.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.core.hardware.ShiftReportThermalBuilder
import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import com.azuredevid.cassy.domain.model.CashMovement
import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.domain.model.ShiftStatus
import com.azuredevid.cassy.domain.model.ZReport
import com.azuredevid.cassy.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShiftUiState(
    val isLoading: Boolean = false,
    val currentShift: Shift? = null,
    val isShiftOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    
    // Modal controls
    val showOpenShiftModal: Boolean = false,
    val showCloseShiftModal: Boolean = false,
    val showCashMovementModal: Boolean = false,
    
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

    // Print state
    val isPrintingZReport: Boolean = false,
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
    private val serverSettings: ServerSettingsRepository,
    private val printerManager: PrinterPort9100Manager = PrinterPort9100Manager(),
    private val thermalBuilder: ShiftReportThermalBuilder = ShiftReportThermalBuilder()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftUiState())
    val uiState: StateFlow<ShiftUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ShiftUiEffect>()
    val effects: SharedFlow<ShiftUiEffect> = _effects.asSharedFlow()

    companion object {
        const val VARIANCE_TOLERANCE_RUPIAH = 50_000L
    }

    init {
        loadCurrentShift()
    }

    fun loadCurrentShift() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            shiftRepository.getCurrentShift()
                .onSuccess { shift ->
                    val isOpen = shift != null && shift.status == ShiftStatus.OPEN
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentShift = shift,
                            isShiftOpen = isOpen
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
    }

    fun openShiftModal() {
        _uiState.update {
            it.copy(
                showOpenShiftModal = true,
                openingFloatInput = "200000",
                openShiftNotes = "",
                errorMessage = null
            )
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
        if (amount < 0) {
            _uiState.update { it.copy(errorMessage = "Saldo awal tidak boleh negatif") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isOpeningShift = true, errorMessage = null) }
            val notes = _uiState.value.openShiftNotes
            shiftRepository.openShift(openingFloat = amount, notes = notes)
                .onSuccess { shift ->
                    _uiState.update {
                        it.copy(
                            isOpeningShift = false,
                            showOpenShiftModal = false,
                            currentShift = shift,
                            isShiftOpen = true,
                            successMessage = "Shift berhasil dibuka dengan modal awal Rp $amount"
                        )
                    }
                    _effects.emit(ShiftUiEffect.ShiftOpenedSuccessfully(shift))
                    _effects.emit(ShiftUiEffect.ShowToast("Shift kasir berhasil dibuka"))
                }
                .onFailure { error ->
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

        if (amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Nominal kas harus lebih dari Rp 0") }
            return
        }
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Alasan mutasi kas wajib diisi") }
            return
        }

        val type = _uiState.value.movementType
        val shiftId = _uiState.value.currentShift?.id

        viewModelScope.launch {
            _uiState.update { it.copy(isRecordingMovement = true, errorMessage = null) }
            shiftRepository.recordCashMovement(
                movementType = type,
                amount = amount,
                reason = reason,
                shiftId = shiftId
            ).onSuccess { movement ->
                _uiState.update {
                    it.copy(
                        isRecordingMovement = false,
                        showCashMovementModal = false,
                        successMessage = "Mutasi kas berhasil dicatat"
                    )
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
        val counted = sanitized.toLongOrNull() ?: 0L
        val expected = _uiState.value.currentShift?.expectedCash ?: 0L
        val variance = counted - expected
        val requiresSupervisor = Math.abs(variance) > VARIANCE_TOLERANCE_RUPIAH

        _uiState.update {
            it.copy(
                countedCashInput = sanitized,
                calculatedVariance = variance,
                isSupervisorAuthRequired = requiresSupervisor
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

    fun submitCloseShift(cashierPrinterIp: String = "192.168.10.200") {
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
            if (supervisorPin.isBlank() || !serverSettings.verifySupervisorPin(supervisorPin)) {
                _uiState.update { it.copy(errorMessage = "PIN Supervisor tidak valid") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isClosingShift = true, errorMessage = null) }
            val notes = _uiState.value.closeShiftNotes
            shiftRepository.closeShift(
                shiftId = shift.id,
                countedCash = counted,
                ownerPin = if (isSupervisorRequired) supervisorPin else null,
                varianceReason = if (isSupervisorRequired) varianceReason else null,
                notes = notes
            ).onSuccess { report ->
                _uiState.update {
                    it.copy(
                        isClosingShift = false,
                        showCloseShiftModal = false,
                        currentShift = null,
                        isShiftOpen = false,
                        lastZReport = report,
                        successMessage = "Shift berhasil ditutup"
                    )
                }
                _effects.emit(ShiftUiEffect.ShiftClosedSuccessfully(report))
                _effects.emit(ShiftUiEffect.ShowToast("Shift berhasil ditutup"))

                if (_uiState.value.printReceiptOnClose) {
                    printZReport(report, cashierPrinterIp)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isClosingShift = false,
                        errorMessage = error.message ?: "Gagal menutup shift"
                    )
                }
            }
        }
    }

    fun printZReport(report: ZReport, printerIp: String, storeName: String = "CASSY POS") {
        viewModelScope.launch {
            _uiState.update { it.copy(isPrintingZReport = true) }
            val payload = thermalBuilder.buildZReportReceipt(storeName, report)
            printerManager.printRawBytes(printerIp, payload)
                .onSuccess {
                    _uiState.update { it.copy(isPrintingZReport = false) }
                    _effects.emit(ShiftUiEffect.ShowToast("Struk Z-Report berhasil dicetak"))
                }
                .onFailure {
                    _uiState.update { it.copy(isPrintingZReport = false) }
                    _effects.emit(ShiftUiEffect.ShowToast("Simulasi Cetak Z-Report (Printer tidak terjangkau)"))
                }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
