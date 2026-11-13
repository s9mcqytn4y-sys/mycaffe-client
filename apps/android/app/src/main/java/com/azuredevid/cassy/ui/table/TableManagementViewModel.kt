package com.azuredevid.cassy.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.core.hardware.TableQrPosterFormatter
import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.data.model.TableBoardEntry
import com.azuredevid.cassy.data.model.TableInput
import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import com.azuredevid.cassy.domain.repository.TableRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TableManagementUiState(
    val tables: List<TableBoardEntry> = emptyList(),
    val selectedSection: String = "all",
    val storeName: String = "Cassy",
    val canManageTables: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isPrintingThermal: Boolean = false,
    val errorMessage: String? = null
) {
    val sections: List<String>
        get() = tables.map { it.section.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    val displayedTables: List<TableBoardEntry>
        get() = if (selectedSection == "all") {
            tables
        } else {
            tables.filter { it.section.equals(selectedSection, ignoreCase = true) }
        }
}

sealed interface TableManagementUiEffect {
    data class ShowMessage(val message: String) : TableManagementUiEffect
    data object TableSaved : TableManagementUiEffect
}

class TableManagementViewModel(
    private val tableRepository: TableRepository,
    private val sessionManager: SessionManager,
    private val serverSettings: ServerSettingsRepository,
    private val printerManager: PrinterPort9100Manager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableManagementUiState())
    val uiState: StateFlow<TableManagementUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<TableManagementUiEffect>()
    val effects: SharedFlow<TableManagementUiEffect> = _effects.asSharedFlow()

    init {
        refreshTables()
    }

    fun refreshTables() {
        viewModelScope.launch {
            val session = sessionManager.currentSession()
            if (session == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        canManageTables = false,
                        errorMessage = "Sesi staf tidak aktif."
                    )
                }
                return@launch
            }
            val role = session.staff?.role?.lowercase().orEmpty()
            val canManage = role == "owner" || role == "manager"
            val storeName = session.staff?.storeName.orEmpty().ifBlank { "Cassy" }

            _uiState.update {
                it.copy(
                    isLoading = true,
                    canManageTables = canManage,
                    storeName = storeName,
                    errorMessage = null
                )
            }
            tableRepository.listTables().fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            tables = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Gagal memuat papan meja."
                        )
                    }
                }
            )
        }
    }

    fun selectSection(section: String) {
        _uiState.update { it.copy(selectedSection = section) }
    }

    fun saveTable(editingTableId: String?, input: TableInput) {
        if (!_uiState.value.canManageTables || _uiState.value.isSubmitting) return
        if (input.tableNumber.isNullOrBlank()) {
            viewModelScope.launch {
                _effects.emit(TableManagementUiEffect.ShowMessage("Nomor meja wajib diisi."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = if (editingTableId.isNullOrBlank()) {
                tableRepository.createTable(input)
            } else {
                tableRepository.updateTable(editingTableId, input)
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(TableManagementUiEffect.TableSaved)
                    _effects.emit(TableManagementUiEffect.ShowMessage("Data meja tersimpan."))
                    refreshTables()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage(err.message ?: "Gagal menyimpan meja."))
                }
            )
        }
    }

    fun archiveTable(tableId: String) {
        if (!_uiState.value.canManageTables || _uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            tableRepository.archiveTable(tableId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage("Meja diarsipkan."))
                    refreshTables()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage(err.message ?: "Gagal mengarsipkan meja."))
                }
            )
        }
    }

    fun rotateQr(table: TableBoardEntry) {
        if (!_uiState.value.canManageTables || _uiState.value.isSubmitting) return
        if (table.status != "available") {
            viewModelScope.launch {
                _effects.emit(
                    TableManagementUiEffect.ShowMessage("Kosongkan sesi meja terlebih dahulu sebelum memutar QR.")
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            tableRepository.rotateQr(table.id).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage("QR meja berhasil diputar. Cetak poster baru."))
                    refreshTables()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage(err.message ?: "Gagal memutar QR meja."))
                }
            )
        }
    }

    fun openSession(tableId: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            tableRepository.openSession(tableId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage("Sesi meja dibuka."))
                    refreshTables()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage(err.message ?: "Gagal membuka sesi meja."))
                }
            )
        }
    }

    fun closeSession(sessionId: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            tableRepository.closeSession(sessionId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage("Sesi meja ditutup."))
                    refreshTables()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage(err.message ?: "Gagal menutup sesi meja."))
                }
            )
        }
    }

    fun buildGuestQrUrl(qrToken: String): String {
        val base = serverSettings.getBaseUrl().trimEnd('/')
        return "$base/m/$qrToken"
    }

    fun printThermalPoster(table: TableBoardEntry, printerIp: String) {
        if (_uiState.value.isPrintingThermal) return
        if (table.qrToken.isBlank()) {
            viewModelScope.launch {
                _effects.emit(TableManagementUiEffect.ShowMessage("Token QR tidak tersedia untuk akun ini."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isPrintingThermal = true) }
            val payload = withContext(ioDispatcher) {
                TableQrPosterFormatter.buildEscPosPoster(
                    storeName = _uiState.value.storeName,
                    tableLabel = table.displayTitle,
                    section = table.section,
                    qrPayloadUrl = buildGuestQrUrl(table.qrToken)
                )
            }
            printerManager.printRawBytes(printerIp, payload).fold(
                onSuccess = {
                    _uiState.update { it.copy(isPrintingThermal = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage("Poster QR dikirim ke printer thermal."))
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isPrintingThermal = false) }
                    _effects.emit(TableManagementUiEffect.ShowMessage(err.message ?: "Gagal mencetak ke printer thermal."))
                }
            )
        }
    }
}
