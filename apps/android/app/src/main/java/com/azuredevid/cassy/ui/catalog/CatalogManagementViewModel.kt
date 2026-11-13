package com.azuredevid.cassy.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.core.util.ImageCompressor
import com.azuredevid.cassy.data.model.CatalogCategory
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.ProductUpsertInput
import com.azuredevid.cassy.data.model.VariantUpsertInput
import com.azuredevid.cassy.domain.repository.CatalogRepository
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

data class CatalogManagementUiState(
    val categories: List<CatalogCategory> = emptyList(),
    val products: List<CatalogProduct> = emptyList(),
    val selectedCategoryId: String = "all",
    val searchQuery: String = "",
    val canManageCatalog: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isUploadingImage: Boolean = false,
    val errorMessage: String? = null
) {
    val displayedProducts: List<CatalogProduct>
        get() {
            val filteredByCategory = if (selectedCategoryId == "all") {
                products
            } else {
                products.filter { it.categoryId == selectedCategoryId }
            }
            if (searchQuery.isBlank()) return filteredByCategory
            return filteredByCategory.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
            }
        }
}

sealed interface CatalogManagementUiEffect {
    data class ShowMessage(val message: String) : CatalogManagementUiEffect
    data object ProductSaved : CatalogManagementUiEffect
    data object VariantSaved : CatalogManagementUiEffect
}

class CatalogManagementViewModel(
    private val catalogRepository: CatalogRepository,
    private val sessionManager: SessionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogManagementUiState())
    val uiState: StateFlow<CatalogManagementUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CatalogManagementUiEffect>()
    val effects: SharedFlow<CatalogManagementUiEffect> = _effects.asSharedFlow()

    init {
        refreshCatalog()
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            val session = sessionManager.currentSession()
            if (session == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        canManageCatalog = false,
                        errorMessage = "Sesi staf tidak aktif."
                    )
                }
                return@launch
            }
            val role = session.staff?.role?.lowercase().orEmpty()
            val canManage = role == "owner" || role == "manager"

            _uiState.update { it.copy(isLoading = true, canManageCatalog = canManage, errorMessage = null) }
            catalogRepository.getStaffCatalog().fold(
                onSuccess = { snapshot ->
                    _uiState.update {
                        it.copy(
                            categories = snapshot.categories,
                            products = snapshot.products,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Gagal memuat katalog."
                        )
                    }
                }
            )
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleProductAvailability(product: CatalogProduct) {
        val nextValue = !product.isAvailable
        _uiState.update { state ->
            state.copy(
                products = state.products.map {
                    if (it.id == product.id) it.copy(isAvailable = nextValue) else it
                }
            )
        }
        viewModelScope.launch {
            catalogRepository.setProductAvailability(product.id, nextValue).onFailure { err ->
                // Rollback optimistic toggle on server rejection/network failure.
                _uiState.update { state ->
                    state.copy(
                        products = state.products.map {
                            if (it.id == product.id) it.copy(isAvailable = product.isAvailable) else it
                        }
                    )
                }
                _effects.emit(
                    CatalogManagementUiEffect.ShowMessage(
                        err.message ?: "Gagal mengubah status tersedia."
                    )
                )
            }
        }
    }

    fun saveCategory(
        editingId: String?,
        name: String,
        displayOrder: Int,
        isActive: Boolean
    ) {
        if (!_uiState.value.canManageCatalog) return
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch {
                _effects.emit(CatalogManagementUiEffect.ShowMessage("Nama kategori wajib diisi."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val res = if (editingId.isNullOrBlank()) {
                catalogRepository.createCategory(trimmed, displayOrder, isActive)
            } else {
                catalogRepository.updateCategory(editingId, trimmed, displayOrder, isActive)
            }
            res.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.ShowMessage("Kategori tersimpan."))
                    refreshCatalog()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.ShowMessage(err.message ?: "Gagal menyimpan kategori."))
                }
            )
        }
    }

    fun deleteCategory(categoryId: String) {
        if (!_uiState.value.canManageCatalog) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            catalogRepository.deleteCategory(categoryId).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            selectedCategoryId = if (it.selectedCategoryId == categoryId) "all" else it.selectedCategoryId
                        )
                    }
                    _effects.emit(CatalogManagementUiEffect.ShowMessage("Kategori dihapus."))
                    refreshCatalog()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.ShowMessage(err.message ?: "Gagal menghapus kategori."))
                }
            )
        }
    }

    fun saveProduct(editingId: String?, input: ProductUpsertInput) {
        if (!_uiState.value.canManageCatalog || _uiState.value.isSubmitting) return
        if (input.name.isBlank()) {
            viewModelScope.launch {
                _effects.emit(CatalogManagementUiEffect.ShowMessage("Nama produk wajib diisi."))
            }
            return
        }
        if (editingId.isNullOrBlank() && (input.basePriceIdr == null || input.basePriceIdr < 0L)) {
            viewModelScope.launch {
                _effects.emit(CatalogManagementUiEffect.ShowMessage("Harga awal produk wajib diisi."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val res = if (editingId.isNullOrBlank()) {
                catalogRepository.createProduct(input)
            } else {
                catalogRepository.updateProduct(editingId, input)
            }
            res.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.ProductSaved)
                    _effects.emit(CatalogManagementUiEffect.ShowMessage("Produk tersimpan."))
                    refreshCatalog()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.ShowMessage(err.message ?: "Gagal menyimpan produk."))
                }
            )
        }
    }

    fun archiveProduct(productId: String) {
        if (!_uiState.value.canManageCatalog) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            catalogRepository.archiveProduct(productId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.ShowMessage("Produk diarsipkan."))
                    refreshCatalog()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.ShowMessage(err.message ?: "Gagal mengarsipkan produk."))
                }
            )
        }
    }

    fun saveVariant(productId: String, editingVariantId: String?, input: VariantUpsertInput) {
        if (!_uiState.value.canManageCatalog || _uiState.value.isSubmitting) return
        if (input.name.isBlank() || input.price < 0L) {
            viewModelScope.launch {
                _effects.emit(CatalogManagementUiEffect.ShowMessage("Nama dan harga varian harus valid."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val res = if (editingVariantId.isNullOrBlank()) {
                catalogRepository.createVariant(productId, input)
            } else {
                catalogRepository.updateVariant(editingVariantId, input)
            }
            res.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.VariantSaved)
                    _effects.emit(CatalogManagementUiEffect.ShowMessage("Varian tersimpan."))
                    refreshCatalog()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.ShowMessage(err.message ?: "Gagal menyimpan varian."))
                }
            )
        }
    }

    fun archiveVariant(variantId: String) {
        if (!_uiState.value.canManageCatalog) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            catalogRepository.archiveVariant(variantId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.ShowMessage("Varian diarsipkan."))
                    refreshCatalog()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(CatalogManagementUiEffect.ShowMessage(err.message ?: "Gagal mengarsipkan varian."))
                }
            )
        }
    }

    fun uploadProductImage(productId: String, readBytes: () -> ByteArray?) {
        if (!_uiState.value.canManageCatalog || _uiState.value.isUploadingImage) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true) }
            val rawBytes = withContext(ioDispatcher) { runCatching(readBytes).getOrNull() }
            if (rawBytes == null || rawBytes.isEmpty()) {
                _uiState.update { it.copy(isUploadingImage = false) }
                _effects.emit(CatalogManagementUiEffect.ShowMessage("Gagal membaca file gambar."))
                return@launch
            }
            if (rawBytes.size > MAX_RAW_IMAGE_BYTES) {
                _uiState.update { it.copy(isUploadingImage = false) }
                _effects.emit(CatalogManagementUiEffect.ShowMessage("Ukuran gambar terlalu besar (maksimal 10 MB)."))
                return@launch
            }
            uploadCompressedBytes(productId, rawBytes)
        }
    }

    fun uploadProductImage(productId: String, rawImageBytes: ByteArray) {
        if (!_uiState.value.canManageCatalog || _uiState.value.isUploadingImage) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true) }
            uploadCompressedBytes(productId, rawImageBytes)
        }
    }

    private suspend fun uploadCompressedBytes(productId: String, rawImageBytes: ByteArray) {
        val webpBytes = withContext(ioDispatcher) {
            ImageCompressor.compressBytes(rawImageBytes)
        }
        catalogRepository.uploadProductImage(productId, webpBytes).fold(
            onSuccess = { imageUrl ->
                _uiState.update { state ->
                    state.copy(
                        isUploadingImage = false,
                        products = state.products.map {
                            if (it.id == productId) it.copy(imageUrl = imageUrl) else it
                        }
                    )
                }
                _effects.emit(CatalogManagementUiEffect.ShowMessage("Foto produk berhasil diunggah."))
            },
            onFailure = { err ->
                _uiState.update { it.copy(isUploadingImage = false) }
                _effects.emit(CatalogManagementUiEffect.ShowMessage(err.message ?: "Gagal mengunggah foto."))
            }
        )
    }

    private companion object {
        private const val MAX_RAW_IMAGE_BYTES = 10 * 1024 * 1024
    }
}
