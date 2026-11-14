package com.azuredevid.cassy.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuredevid.cassy.core.hardware.EscPosReceiptBuilder
import com.azuredevid.cassy.core.hardware.KdsTicketRouter
import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.data.model.CatalogCategory
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.OrderQuote
import com.azuredevid.cassy.data.network.CassyRealtimeClient
import com.azuredevid.cassy.domain.model.CreatePosOrderInput
import com.azuredevid.cassy.domain.model.CreatePosOrderItemInput
import com.azuredevid.cassy.domain.model.InitiatePosPaymentInput
import com.azuredevid.cassy.domain.model.PosOrder
import com.azuredevid.cassy.domain.model.PosPaymentAttempt
import com.azuredevid.cassy.domain.repository.CatalogRepository
import com.azuredevid.cassy.domain.repository.OrderQuoteRepository
import com.azuredevid.cassy.domain.repository.PosOrderRepository
import com.azuredevid.cassy.domain.repository.PrinterConfig
import com.azuredevid.cassy.domain.repository.PrinterSettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class PosUiState(
    val categories: List<CatalogCategory> = emptyList(),
    val allProducts: List<CatalogProduct> = emptyList(),
    val isLoadingCatalog: Boolean = true,
    val catalogError: String? = null,
    val selectedCategoryId: String = "all",
    val searchQuery: String = "",
    val cart: List<CartItem> = emptyList(),
    val orderType: String = "DINE-IN",
    val tableName: String = "",
    val customerName: String = "",
    val dineInRound: Int = 1,
    val quote: OrderQuote? = null,
    val isQuoting: Boolean = false,
    val quoteError: String? = null,
    val storeName: String = "",
    val cashierName: String = "",
    val printerConfig: PrinterConfig = PrinterConfig(),
    val isPrinterConfigured: Boolean = false,
    val failoverAlert: String? = null,
    val isSubmittingOrder: Boolean = false,
    val settlementError: String? = null,
    val showCashModal: Boolean = false,
    val pendingOrder: PosOrder? = null,
    val pendingOrderIdempotencyKey: String? = null,
    val activeQrisPayment: PosPaymentAttempt? = null,
    val isCheckingQrisStatus: Boolean = false
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

    val isCartLocked: Boolean
        get() = activeQrisPayment != null || isSubmittingOrder
}

sealed interface PosUiEffect {
    data class ShowToast(val message: String) : PosUiEffect
    data class CashPaymentCompleted(val orderNumber: String) : PosUiEffect
    data class QrisPaymentCompleted(val orderNumber: String) : PosUiEffect
}

class PosViewModel(
    private val catalogRepository: CatalogRepository,
    private val orderQuoteRepository: OrderQuoteRepository,
    private val posOrderRepository: PosOrderRepository,
    private val printerSettings: PrinterSettingsRepository,
    private val sessionManager: SessionManager,
    private val printerManager: PrinterPort9100Manager = PrinterPort9100Manager(),
    private val realtimeClient: CassyRealtimeClient? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PosUiEffect>()
    val effects: SharedFlow<PosUiEffect> = _effects.asSharedFlow()

    private var quoteJob: Job? = null
    private var qrisPollJob: Job? = null

    init {
        observePrinterConfig()
        observeRealtimeEvents()
        loadInitialData()
    }

    private fun observePrinterConfig() {
        viewModelScope.launch {
            printerSettings.configFlow.collect { config ->
                _uiState.update {
                    it.copy(
                        printerConfig = config,
                        isPrinterConfigured = config.hasCashierPrinter
                    )
                }
            }
        }
    }

    private fun observeRealtimeEvents() {
        val client = realtimeClient ?: return
        viewModelScope.launch {
            client.events.collect { event ->
                val activeOrder = _uiState.value.pendingOrder ?: return@collect
                if (_uiState.value.activeQrisPayment == null) return@collect
                val matchesOrder = event.payloadJson.contains(activeOrder.id, ignoreCase = true) ||
                    (activeOrder.orderNumber.isNotBlank() && event.payloadJson.contains(activeOrder.orderNumber, ignoreCase = true))
                if (matchesOrder && (event.type.contains("paid", ignoreCase = true) || event.type.contains("settled", ignoreCase = true))) {
                    checkQrisStatusOnce()
                }
            }
        }
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

        if (session == null || storeSlug.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoadingCatalog = false,
                    catalogError = "Sesi toko tidak ditemukan. Silakan masuk kembali."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCatalog = true, catalogError = null) }
            catalogRepository.getStaffCatalog()
                .onSuccess { snapshot ->
                    val activeCategories = snapshot.categories
                        .filter { it.isActive }
                        .sortedBy { it.displayOrder }
                    _uiState.update {
                        it.copy(
                            categories = activeCategories,
                            allProducts = snapshot.products,
                            isLoadingCatalog = false,
                            catalogError = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            categories = emptyList(),
                            allProducts = emptyList(),
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

    fun updateTableName(tableName: String) {
        if (_uiState.value.isCartLocked) return
        _uiState.update {
            it.copy(
                tableName = tableName.trim(),
                pendingOrder = null,
                pendingOrderIdempotencyKey = null
            )
        }
    }

    fun toggleOrderType() {
        if (_uiState.value.isCartLocked) return
        _uiState.update {
            val next = if (it.orderType == "DINE-IN") "TAKEAWAY" else "DINE-IN"
            it.copy(
                orderType = next,
                tableName = if (next == "TAKEAWAY") "" else it.tableName,
                pendingOrder = null,
                pendingOrderIdempotencyKey = null
            )
        }
        refreshQuote()
    }

    fun addProductToCart(product: CatalogProduct) {
        if (_uiState.value.isCartLocked || !product.isAvailable) return
        _uiState.update { state ->
            val existingIndex = state.cart.indexOfFirst { it.product.id == product.id }
            val updatedCart = if (existingIndex >= 0) {
                state.cart.mapIndexed { idx, item ->
                    if (idx == existingIndex) item.copy(quantity = item.quantity + 1) else item
                }
            } else {
                state.cart + CartItem(product = product, quantity = 1)
            }
            state.copy(
                cart = updatedCart,
                pendingOrder = null,
                pendingOrderIdempotencyKey = null,
                settlementError = null
            )
        }
        refreshQuote()
    }

    fun increaseItemQuantity(productId: String) {
        if (_uiState.value.isCartLocked) return
        _uiState.update { state ->
            state.copy(
                cart = state.cart.map { item ->
                    if (item.product.id == productId) item.copy(quantity = item.quantity + 1) else item
                },
                pendingOrder = null,
                pendingOrderIdempotencyKey = null,
                settlementError = null
            )
        }
        refreshQuote()
    }

    fun decreaseItemQuantity(productId: String) {
        if (_uiState.value.isCartLocked) return
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
            state.copy(
                cart = updatedCart,
                pendingOrder = null,
                pendingOrderIdempotencyKey = null,
                settlementError = null
            )
        }
        refreshQuote()
    }

    fun openCashModal() {
        if (_uiState.value.cart.isEmpty() || _uiState.value.quote == null) return
        _uiState.update { it.copy(showCashModal = true, settlementError = null) }
    }

    fun closeCashModal() {
        if (_uiState.value.isSubmittingOrder) return
        _uiState.update { it.copy(showCashModal = false, settlementError = null) }
    }

    fun submitCashPayment(tenderedAmount: Long, drawerId: String? = null) {
        val snapshot = _uiState.value
        val quote = snapshot.quote ?: return
        if (snapshot.cart.isEmpty() || snapshot.isSubmittingOrder) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingOrder = true, settlementError = null) }
            val orderResult = ensureOrderCreated(snapshot, quote)
            val order = orderResult.getOrElse { err ->
                _uiState.update {
                    it.copy(
                        isSubmittingOrder = false,
                        settlementError = err.message ?: "Gagal mengirim pesanan ke server."
                    )
                }
                _effects.emit(PosUiEffect.ShowToast(err.message ?: "Gagal mengirim pesanan ke server."))
                return@launch
            }

            val paymentResult = posOrderRepository.initiatePayment(
                InitiatePosPaymentInput(
                    orderId = order.id,
                    idempotencyKey = UUID.randomUUID().toString(),
                    method = "cash",
                    cashTenderedAmount = tenderedAmount,
                    drawerId = drawerId
                )
            )
            paymentResult.onSuccess { res ->
                val settledOrder = res.order ?: order
                val config = printerSettings.getConfig()
                if (res.openDrawer && config.hasCashierPrinter) {
                    printerManager.openCashDrawer(config.cashierPrinterIp, config.cashierPrinterPort)
                }
                dispatchReceiptsAndKitchenTickets(
                    order = settledOrder,
                    cartSnapshot = snapshot.cart,
                    tableLabel = resolveTableLabel(snapshot),
                    paymentLabel = "TUNAI",
                    cashTenderedIdr = res.payment.cashTenderedAmount ?: tenderedAmount,
                    changeIdr = res.payment.changeGivenAmount ?: (tenderedAmount - settledOrder.totalAmount).coerceAtLeast(0L),
                    roundingIdr = res.payment.roundingAmount ?: 0L,
                    config = config,
                    roundNumber = snapshot.dineInRound
                )
                clearCartAfterPayment()
                val orderNo = settledOrder.orderNumber.ifBlank { order.id.takeLast(8).uppercase() }
                _effects.emit(PosUiEffect.CashPaymentCompleted(orderNo))
                _effects.emit(PosUiEffect.ShowToast("Transaksi #$orderNo selesai."))
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSubmittingOrder = false,
                        settlementError = err.message ?: "Gagal memproses pembayaran tunai."
                    )
                }
                _effects.emit(PosUiEffect.ShowToast(err.message ?: "Gagal memproses pembayaran tunai."))
            }
        }
    }

    fun startQrisPayment(drawerId: String? = null) {
        val snapshot = _uiState.value
        val quote = snapshot.quote ?: return
        if (snapshot.cart.isEmpty() || snapshot.isSubmittingOrder) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingOrder = true, settlementError = null) }
            val orderResult = ensureOrderCreated(snapshot, quote)
            val order = orderResult.getOrElse { err ->
                _uiState.update {
                    it.copy(
                        isSubmittingOrder = false,
                        settlementError = err.message ?: "Gagal membuat pesanan QRIS."
                    )
                }
                _effects.emit(PosUiEffect.ShowToast(err.message ?: "Gagal membuat pesanan QRIS."))
                return@launch
            }

            posOrderRepository.initiatePayment(
                InitiatePosPaymentInput(
                    orderId = order.id,
                    idempotencyKey = UUID.randomUUID().toString(),
                    method = "qris",
                    drawerId = drawerId
                )
            ).onSuccess { res ->
                val updatedOrder = res.order ?: order
                _uiState.update {
                    it.copy(
                        isSubmittingOrder = false,
                        pendingOrder = updatedOrder,
                        activeQrisPayment = res.payment
                    )
                }
                if (updatedOrder.paymentStatus.equals("paid", ignoreCase = true) ||
                    res.payment.status.equals("settled", ignoreCase = true)
                ) {
                    finalizeQrisSettlement(updatedOrder)
                } else {
                    startQrisPolling()
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSubmittingOrder = false,
                        settlementError = err.message ?: "Gagal membuat tagihan QRIS."
                    )
                }
                _effects.emit(PosUiEffect.ShowToast(err.message ?: "Gagal membuat tagihan QRIS."))
            }
        }
    }

    fun refreshQrisStatus() {
        viewModelScope.launch {
            checkQrisStatusOnce()
        }
    }

    fun dismissQrisModal() {
        qrisPollJob?.cancel()
        qrisPollJob = null
        _uiState.update {
            it.copy(
                activeQrisPayment = null,
                isCheckingQrisStatus = false
            )
        }
    }

    private fun startQrisPolling() {
        qrisPollJob?.cancel()
        qrisPollJob = viewModelScope.launch {
            while (isActive && _uiState.value.activeQrisPayment != null) {
                delay(3_000L)
                checkQrisStatusOnce()
            }
        }
    }

    private suspend fun checkQrisStatusOnce() {
        val order = _uiState.value.pendingOrder ?: return
        if (_uiState.value.activeQrisPayment == null) return
        _uiState.update { it.copy(isCheckingQrisStatus = true) }
        posOrderRepository.getOrder(order.id)
            .onSuccess { detail ->
                _uiState.update { it.copy(isCheckingQrisStatus = false, pendingOrder = detail.order) }
                val paid = detail.order.paymentStatus.equals("paid", ignoreCase = true) ||
                    detail.payments.any { it.status.equals("settled", ignoreCase = true) || it.status.equals("paid", ignoreCase = true) }
                if (paid) {
                    finalizeQrisSettlement(detail.order)
                }
            }
            .onFailure {
                _uiState.update { it.copy(isCheckingQrisStatus = false) }
            }
    }

    private suspend fun finalizeQrisSettlement(order: PosOrder) {
        val snapshot = _uiState.value
        val config = printerSettings.getConfig()
        dispatchReceiptsAndKitchenTickets(
            order = order,
            cartSnapshot = snapshot.cart,
            tableLabel = resolveTableLabel(snapshot),
            paymentLabel = "QRIS DINAMIS",
            cashTenderedIdr = null,
            changeIdr = null,
            roundingIdr = 0L,
            config = config,
            roundNumber = snapshot.dineInRound
        )
        clearCartAfterPayment()
        val orderNo = order.orderNumber.ifBlank { order.id.takeLast(8).uppercase() }
        _effects.emit(PosUiEffect.QrisPaymentCompleted(orderNo))
        _effects.emit(PosUiEffect.ShowToast("Pembayaran QRIS #$orderNo lunas."))
    }

    fun kickDrawerManual() {
        viewModelScope.launch {
            val config = printerSettings.getConfig()
            if (!config.hasCashierPrinter) {
                _effects.emit(PosUiEffect.ShowToast("Printer kasir belum dikonfigurasi. Atur IP printer di Pengaturan."))
                return@launch
            }
            printerManager.openCashDrawer(config.cashierPrinterIp, config.cashierPrinterPort)
                .onSuccess {
                    _effects.emit(PosUiEffect.ShowToast("Laci kasir terbuka (RJ11)."))
                }
                .onFailure {
                    _effects.emit(PosUiEffect.ShowToast("Printer LAN (${config.cashierPrinterIp}) tidak terjangkau."))
                }
        }
    }

    fun sendToKitchen() {
        val snapshot = _uiState.value
        if (snapshot.cart.isEmpty()) return
        viewModelScope.launch {
            val config = printerSettings.getConfig()
            val stations = buildStationConfigs(config)
            if (stations.isEmpty()) {
                _effects.emit(PosUiEffect.ShowToast("Printer dapur belum dikonfigurasi. Atur IP printer di Pengaturan."))
                return@launch
            }
            val router = KdsTicketRouter(printerManager)
            val result = router.routeOrderDynamic(
                orderNumber = snapshot.pendingOrder?.orderNumber?.ifBlank { null } ?: "DAPUR-R${snapshot.dineInRound}",
                tableOrQueue = resolveTableLabel(snapshot),
                items = snapshot.cart.toStationItems(),
                stations = stations,
                cashierPrinterIp = config.cashierPrinterIp,
                deliveryMode = KdsTicketRouter.OrderDeliveryMode.DINE_IN,
                roundNumber = snapshot.dineInRound
            )
            markCartSentToKitchen()
            _uiState.update {
                it.copy(
                    failoverAlert = if (result.hasAnyFailover) {
                        "Sebagian printer stasiun offline. Tiket dialihkan ke printer cadangan."
                    } else null
                )
            }
            if (result.errors.isEmpty()) {
                _effects.emit(PosUiEffect.ShowToast("Tiket dapur ronde #${snapshot.dineInRound} terkirim."))
            } else {
                _effects.emit(PosUiEffect.ShowToast(result.errors.first()))
            }
        }
    }

    fun reprintStations() {
        val snapshot = _uiState.value
        if (snapshot.cart.isEmpty()) return
        viewModelScope.launch {
            val config = printerSettings.getConfig()
            val stations = buildStationConfigs(config)
            if (stations.isEmpty()) {
                _effects.emit(PosUiEffect.ShowToast("Printer dapur belum dikonfigurasi."))
                return@launch
            }
            val router = KdsTicketRouter(printerManager)
            val result = router.routeOrderDynamic(
                orderNumber = snapshot.pendingOrder?.orderNumber?.ifBlank { null } ?: "REPRINT-R${snapshot.dineInRound}",
                tableOrQueue = resolveTableLabel(snapshot),
                items = snapshot.cart.toStationItems(),
                stations = stations,
                cashierPrinterIp = config.cashierPrinterIp,
                deliveryMode = if (snapshot.orderType == "DINE-IN") {
                    KdsTicketRouter.OrderDeliveryMode.DINE_IN
                } else {
                    KdsTicketRouter.OrderDeliveryMode.TAKEAWAY
                },
                roundNumber = snapshot.dineInRound
            )
            if (result.errors.isEmpty()) {
                _effects.emit(PosUiEffect.ShowToast("Cetak ulang tiket stasiun selesai."))
            } else {
                _effects.emit(PosUiEffect.ShowToast(result.errors.first()))
            }
        }
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
        qrisPollJob?.cancel()
        qrisPollJob = null
        _uiState.update {
            it.copy(
                cart = emptyList(),
                quote = null,
                quoteError = null,
                isQuoting = false,
                dineInRound = 1,
                showCashModal = false,
                isSubmittingOrder = false,
                settlementError = null,
                pendingOrder = null,
                pendingOrderIdempotencyKey = null,
                activeQrisPayment = null,
                isCheckingQrisStatus = false,
                failoverAlert = null
            )
        }
    }

    private suspend fun ensureOrderCreated(snapshot: PosUiState, quote: OrderQuote): Result<PosOrder> {
        val existing = snapshot.pendingOrder
        if (existing != null) {
            return Result.success(existing)
        }
        val key = snapshot.pendingOrderIdempotencyKey ?: UUID.randomUUID().toString()
        val orderTypeApi = if (snapshot.orderType == "DINE-IN") "dine_in" else "takeaway"
        val customer = snapshot.customerName.trim().ifBlank {
            if (orderTypeApi == "dine_in" && snapshot.tableName.isNotBlank()) {
                "Tamu ${snapshot.tableName}"
            } else {
                "Pelanggan Walk-In"
            }
        }
        val result = posOrderRepository.createOrder(
            CreatePosOrderInput(
                idempotencyKey = key,
                tableNumber = if (orderTypeApi == "dine_in") snapshot.tableName.ifBlank { null } else null,
                customerName = customer,
                orderType = orderTypeApi,
                expectedTotal = quote.totalAmount,
                expectedQuoteFingerprint = quote.quoteFingerprint.ifBlank { null },
                items = snapshot.cart.map {
                    CreatePosOrderItemInput(
                        productId = it.product.id,
                        quantity = it.quantity,
                        notes = it.notes
                    )
                }
            )
        )
        result.onSuccess { created ->
            _uiState.update { it.copy(pendingOrder = created, pendingOrderIdempotencyKey = key) }
        }
        return result
    }

    private suspend fun dispatchReceiptsAndKitchenTickets(
        order: PosOrder,
        cartSnapshot: List<CartItem>,
        tableLabel: String,
        paymentLabel: String,
        cashTenderedIdr: Long?,
        changeIdr: Long?,
        roundingIdr: Long,
        config: PrinterConfig,
        roundNumber: Int
    ) {
        val stations = buildStationConfigs(config)
        val hasAnyPrinter = config.hasCashierPrinter || stations.isNotEmpty()
        if (!hasAnyPrinter) return

        val paperWidth = if (config.paperWidthMm == 58) {
            EscPosReceiptBuilder.PaperWidth.WIDTH_58MM
        } else {
            EscPosReceiptBuilder.PaperWidth.WIDTH_80MM
        }
        val receiptItems = if (order.items.isNotEmpty()) {
            order.items.map {
                EscPosReceiptBuilder.ReceiptItem(
                    name = it.snapshotName,
                    qty = it.quantity,
                    priceIdr = if (it.snapshotPrice > 0L) it.snapshotPrice else it.unitBasePrice
                )
            }
        } else {
            cartSnapshot.map {
                EscPosReceiptBuilder.ReceiptItem(it.product.name, it.quantity, it.product.basePriceIdr)
            }
        }

        val receiptBytes = if (config.hasCashierPrinter) {
            EscPosReceiptBuilder(paperWidth).buildStoreReceipt(
                storeName = _uiState.value.storeName.ifBlank { "KASIR POS" },
                storeAddress = "",
                orderId = order.orderNumber.ifBlank { order.id },
                tableName = tableLabel,
                cashierName = _uiState.value.cashierName.ifBlank { "Kasir" },
                items = receiptItems,
                subtotalIdr = order.subtotalAmount,
                pb1TaxIdr = order.taxAmount,
                grandTotalIdr = order.totalAmount,
                paymentMethod = paymentLabel,
                cashTenderedIdr = cashTenderedIdr,
                changeIdr = changeIdr,
                serviceChargeIdr = order.serviceChargeAmount,
                discountIdr = order.discountAmount,
                roundingIdr = roundingIdr
            )
        } else {
            null
        }

        val router = KdsTicketRouter(printerManager)
        val deliveryMode = if (order.orderType.equals("takeaway", ignoreCase = true)) {
            KdsTicketRouter.OrderDeliveryMode.TAKEAWAY
        } else {
            KdsTicketRouter.OrderDeliveryMode.DINE_IN
        }
        val result = router.routeOrderDynamic(
            orderNumber = order.orderNumber.ifBlank { order.id.takeLast(8).uppercase() },
            tableOrQueue = tableLabel,
            items = cartSnapshot.toStationItems(),
            stations = stations,
            cashierPrinterIp = config.cashierPrinterIp,
            cashierReceiptBytes = receiptBytes,
            deliveryMode = deliveryMode,
            roundNumber = roundNumber
        )
        if (result.hasAnyFailover) {
            _uiState.update {
                it.copy(failoverAlert = "Sebagian printer stasiun offline. Tiket dialihkan ke printer cadangan.")
            }
        }
    }

    private fun buildStationConfigs(config: PrinterConfig): List<KdsTicketRouter.StationPrinterConfig> {
        val fallback = config.cashierPrinterIp.takeIf { it.isNotBlank() }
        val list = mutableListOf<KdsTicketRouter.StationPrinterConfig>()
        if (config.barPrinterIp.isNotBlank()) {
            list.add(KdsTicketRouter.StationPrinterConfig("bar", "BARISTA", config.barPrinterIp, fallback, config.cashierPrinterPort))
        }
        if (config.kitchenPrinterIp.isNotBlank()) {
            list.add(KdsTicketRouter.StationPrinterConfig("kitchen", "DAPUR PANAS", config.kitchenPrinterIp, fallback, config.cashierPrinterPort))
        }
        if (config.pastryPrinterIp.isNotBlank()) {
            list.add(KdsTicketRouter.StationPrinterConfig("pastry", "PASTRY & DINGIN", config.pastryPrinterIp, fallback, config.cashierPrinterPort))
        }
        if (config.expediterPrinterIp.isNotBlank()) {
            list.add(KdsTicketRouter.StationPrinterConfig("runner", "RUNNER / EXPO", config.expediterPrinterIp, fallback, config.cashierPrinterPort))
        }
        return list
    }

    private fun resolveTableLabel(state: PosUiState): String {
        return if (state.orderType == "TAKEAWAY") {
            "Takeaway"
        } else {
            state.tableName.ifBlank { "Dine-In" }
        }
    }

    private fun List<CartItem>.toStationItems(): List<KdsTicketRouter.StationItem> = map {
        KdsTicketRouter.StationItem(
            name = it.product.name,
            quantity = it.quantity,
            prepStation = it.product.prepStation,
            notes = it.notes
        )
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

    override fun onCleared() {
        quoteJob?.cancel()
        qrisPollJob?.cancel()
        super.onCleared()
    }
}
