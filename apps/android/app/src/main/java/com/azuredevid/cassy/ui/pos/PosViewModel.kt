package com.azuredevid.cassy.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.OrderQuote
import com.azuredevid.cassy.domain.repository.CatalogRepository
import com.azuredevid.cassy.domain.repository.OrderQuoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PosUiState(
    val allProducts: List<CatalogProduct> = emptyList(),
    val isLoadingCatalog: Boolean = true,
    val catalogError: String? = null,
    val selectedCategoryId: String = "all",
    val searchQuery: String = "",
    val cart: List<CartItem> = emptyList(),
    val orderType: String = "DINE-IN",
    val tableName: String = "Meja T-04",
    val dineInRound: Int = 1,
    val quote: OrderQuote? = null,
    val isQuoting: Boolean = false,
    val quoteError: String? = null,
    val storeName: String = "",
    val cashierName: String = ""
) {
    val displayedProducts: List<CatalogProduct>
        get() {
            val baseList = if (selectedCategoryId == "all") {
                allProducts
            } else {
                allProducts.filter { it.categoryId == selectedCategoryId }
            }
            return if (searchQuery.isBlank()) {
                baseList
            } else {
                baseList.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
                }
            }
        }
}

class PosViewModel(
    private val catalogRepository: CatalogRepository,
    private val orderQuoteRepository: OrderQuoteRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private var quoteJob: Job? = null

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        val session = sessionManager.currentSession()
        val storeSlug = session?.staff?.storeSlug
        _uiState.update {
            it.copy(
                storeName = session?.staff?.storeName.orEmpty(),
                cashierName = session?.staff?.fullName.orEmpty()
            )
        }

        if (storeSlug.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoadingCatalog = false,
                    catalogError = "Sesi toko tidak ditemukan. Silakan hubungkan gerai kembali."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCatalog = true, catalogError = null) }
            catalogRepository.getCatalog(storeSlug)
                .onSuccess { products ->
                    _uiState.update {
                        it.copy(
                            allProducts = products,
                            isLoadingCatalog = false,
                            catalogError = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingCatalog = false,
                            catalogError = err.message ?: "Gagal memuat katalog."
                        )
                    }
                }
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleOrderType() {
        _uiState.update {
            val next = if (it.orderType == "DINE-IN") "TAKEAWAY" else "DINE-IN"
            it.copy(orderType = next)
        }
        refreshQuote()
    }

    fun addProductToCart(product: CatalogProduct) {
        _uiState.update { state ->
            val existingIndex = state.cart.indexOfFirst { it.product.id == product.id }
            val updatedCart = if (existingIndex >= 0) {
                state.cart.mapIndexed { idx, item ->
                    if (idx == existingIndex) item.copy(quantity = item.quantity + 1) else item
                }
            } else {
                state.cart + CartItem(product = product, quantity = 1)
            }
            state.copy(cart = updatedCart)
        }
        refreshQuote()
    }

    fun increaseItemQuantity(productId: String) {
        _uiState.update { state ->
            state.copy(
                cart = state.cart.map { item ->
                    if (item.product.id == productId) item.copy(quantity = item.quantity + 1) else item
                }
            )
        }
        refreshQuote()
    }

    fun decreaseItemQuantity(productId: String) {
        _uiState.update { state ->
            val updatedCart = state.cart.mapNotNull { item ->
                if (item.product.id != productId) {
                    item
                } else if (item.quantity > 1) {
                    item.copy(quantity = item.quantity - 1)
                } else {
                    null
                }
            }
            state.copy(cart = updatedCart)
        }
        refreshQuote()
    }

    fun markCartSentToKitchen() {
        _uiState.update { state ->
            state.copy(
                dineInRound = state.dineInRound + 1,
                cart = state.cart.map { it.copy(isSentToKitchen = true) }
            )
        }
    }

    fun clearCartAfterPayment() {
        quoteJob?.cancel()
        _uiState.update {
            it.copy(
                cart = emptyList(),
                quote = null,
                quoteError = null,
                isQuoting = false,
                dineInRound = 1
            )
        }
    }

    private fun refreshQuote() {
        quoteJob?.cancel()
        val snapshot = _uiState.value
        if (snapshot.cart.isEmpty()) {
            _uiState.update { it.copy(quote = null, isQuoting = false, quoteError = null) }
            return
        }

        quoteJob = viewModelScope.launch {
            _uiState.update { it.copy(isQuoting = true, quoteError = null) }
            orderQuoteRepository.quote(quoteRequestOf(snapshot.cart, snapshot.orderType))
                .onSuccess { quote ->
                    _uiState.update { it.copy(quote = quote, isQuoting = false, quoteError = null) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            quote = null,
                            isQuoting = false,
                            quoteError = err.message ?: "Gagal menghitung total dari server."
                        )
                    }
                }
        }
    }
}
