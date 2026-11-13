package com.azuredevid.cassy.ui.settings

import androidx.lifecycle.ViewModel
import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ServerConfigUiState(
    val urlInput: String = "",
    val urlError: String? = null,
    val isSaved: Boolean = false
)

class ServerConfigViewModel(
    private val serverSettings: ServerSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ServerConfigUiState(urlInput = serverSettings.getBaseUrl())
    )
    val uiState: StateFlow<ServerConfigUiState> = _uiState.asStateFlow()

    fun onUrlChanged(value: String) {
        _uiState.update { it.copy(urlInput = value, urlError = null, isSaved = false) }
    }

    fun applyPreset(url: String) {
        _uiState.update { it.copy(urlInput = url, urlError = null, isSaved = false) }
    }

    fun save(): Boolean {
        val raw = _uiState.value.urlInput.trim()
        val valid = raw.startsWith("https://", ignoreCase = true) ||
            raw.startsWith("http://", ignoreCase = true)
        if (!valid || raw.length < 10) {
            _uiState.update { it.copy(urlError = "Masukkan URL http:// atau https:// yang valid.") }
            return false
        }
        serverSettings.setBaseUrl(raw)
        _uiState.update {
            it.copy(
                urlInput = serverSettings.getBaseUrl(),
                urlError = null,
                isSaved = true
            )
        }
        return true
    }
}
