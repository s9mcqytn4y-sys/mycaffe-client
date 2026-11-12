package com.azuredevid.cassy.ui.pos

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.azuredevid.cassy.R
import com.azuredevid.cassy.core.hardware.EscPosReceiptBuilder
import com.azuredevid.cassy.core.hardware.KdsTicketRouter
import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.OrderQuote
import com.azuredevid.cassy.data.model.OrderQuoteItem
import com.azuredevid.cassy.data.model.OrderQuoteRequest
import com.azuredevid.cassy.data.repository.KtorCatalogRepository
import com.azuredevid.cassy.data.repository.KtorOrderQuoteRepository
import com.azuredevid.cassy.data.repository.KtorPaymentCredentialRepository
import com.azuredevid.cassy.domain.repository.CatalogRepository
import com.azuredevid.cassy.domain.repository.OrderQuoteRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.azuredevid.cassy.ui.pos.components.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

data class CartItem(
    val product: CatalogProduct,
    var quantity: Int = 1,
    var notes: String = "",
    var isSentToKitchen: Boolean = false
)

/**
 * Clean, modular 10%-55%-35% Landscape POS Screen for tablets.
 * Compliant with Clean Code, SOLID, and F&B multi-station routing.
 */
@Composable
fun PosLandscapeScreen(
    catalogRepository: CatalogRepository? = null,
    orderQuoteRepository: OrderQuoteRepository? = null,
    printerManager: PrinterPort9100Manager = remember { PrinterPort9100Manager() },
    stationConfigs: List<KdsTicketRouter.StationPrinterConfig> = remember {
        listOf(
            KdsTicketRouter.StationPrinterConfig("bar", "BARISTA", "192.168.10.201", "192.168.10.200"),
            KdsTicketRouter.StationPrinterConfig("kitchen", "DAPUR PANAS", "192.168.10.202", "192.168.10.200"),
            KdsTicketRouter.StationPrinterConfig("pastry", "PASTRY & DINGIN", "192.168.10.203", "192.168.10.200"),
            KdsTicketRouter.StationPrinterConfig("runner", "RUNNER / EXPO", "192.168.10.200", null)
        )
    },
    cashierPrinterIp: String = "192.168.10.200",
    onOpenDrawerKick: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToPairing: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember(catalogRepository) {
        catalogRepository ?: KtorCatalogRepository(context)
    }
    // shortcut: one client per screen until Koin and the shared Ktor client land in Android Release One PR 1.
    val quoteRepo = remember(orderQuoteRepository) {
        orderQuoteRepository ?: KtorOrderQuoteRepository(
            httpClient = HttpClient(OkHttp) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
            apiBaseUrl = "https://api.abbdullaziz.online",
            staffToken = { KtorPaymentCredentialRepository(context).getStoredAuthToken() }
        )
    }
    val ticketRouter = remember(printerManager) { KdsTicketRouter(printerManager) }
    val rupiahFormat = remember { NumberFormat.getIntegerInstance(Locale("id", "ID")) }

    var allProducts by remember { mutableStateOf<List<CatalogProduct>>(emptyList()) }
    var isLoadingCatalog by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repo.getCatalog("central-cafe")
            .onSuccess {
                allProducts = it
                isLoadingCatalog = false
            }
            .onFailure {
                isLoadingCatalog = false
            }
    }

    var selectedCategoryId by rememberSaveable { mutableStateOf("all") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val cart = remember { mutableStateListOf<CartItem>() }

    var showCashModal by remember { mutableStateOf(false) }
    var isPrinterOnline by remember { mutableStateOf(true) }
    var orderType by rememberSaveable { mutableStateOf("DINE-IN") }
    var tableName by rememberSaveable { mutableStateOf("Meja T-04") }
    var dineInRound by rememberSaveable { mutableIntStateOf(1) }
    var failoverAlert by remember { mutableStateOf<String?>(null) }

    // Categories with adaptive i18n
    val categories = listOf(
        "all" to stringResource(R.string.category_all),
        "signature" to stringResource(R.string.category_signature),
        "espresso" to stringResource(R.string.category_espresso),
        "manual_brew" to stringResource(R.string.category_manual_brew),
        "pastry" to stringResource(R.string.category_pastry),
        "non_coffee" to stringResource(R.string.category_non_coffee),
        "meals" to stringResource(R.string.category_meals),
        "beans" to stringResource(R.string.category_beans)
    )

    // Filtered Products
    val displayedProducts = remember(selectedCategoryId, searchQuery, allProducts) {
        val baseList = if (selectedCategoryId == "all") {
            allProducts
        } else {
            allProducts.filter { it.categoryId == selectedCategoryId }
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Totals come only from the server quote; the tablet never computes tax or totals (CLAUDE.md).
    var quote by remember { mutableStateOf<OrderQuote?>(null) }
    val cartKey = cart.map { it.product.id to it.quantity }
    LaunchedEffect(cartKey, orderType) {
        quote = null
        if (cartKey.isEmpty()) return@LaunchedEffect
        quote = quoteRepo.quote(quoteRequestOf(cart, orderType)).getOrNull()
    }

    val deliveryMode = if (orderType == "DINE-IN") KdsTicketRouter.OrderDeliveryMode.DINE_IN else KdsTicketRouter.OrderDeliveryMode.TAKEAWAY

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val totalWidth = maxWidth

        Row(modifier = Modifier.fillMaxSize()) {
            // Panel 1: Vertical Navigation Rail (10%)
            PosNavRail(
                railWidth = totalWidth * 0.10f,
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it },
                allProducts = allProducts,
                isPrinterOnline = isPrinterOnline,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPairing = onNavigateToPairing
            )

            VerticalDivider()

            // Panel 2: Product Catalog Grid (55%)
            PosCatalogGrid(
                gridWidth = totalWidth * 0.55f,
                displayedProducts = displayedProducts,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                isLoadingCatalog = isLoadingCatalog,
                onProductSelected = { product ->
                    run {
                        val existing = cart.find { it.product.id == product.id }
                        if (existing != null) {
                            existing.quantity += 1
                        } else {
                            cart.add(CartItem(product = product, quantity = 1))
                        }
                    }
                },
                rupiahFormat = rupiahFormat
            )

            VerticalDivider()

            // Panel 3: Active Order Summary & Actions (35%)
            PosOrderSummaryPanel(
                panelWidth = totalWidth * 0.35f,
                tableName = tableName,
                orderType = orderType,
                onToggleOrderType = {
                    orderType = if (orderType == "DINE-IN") "TAKEAWAY" else "DINE-IN"
                },
                cart = cart,
                isCartLocked = false,
                onIncreaseQuantity = { item -> item.quantity += 1 },
                onDecreaseQuantity = { item ->
                    if (item.quantity > 1) {
                        item.quantity -= 1
                    } else {
                        cart.remove(item)
                    }
                },
                quote = quote,
                rupiahFormat = rupiahFormat,
                onOpenDrawerKick = {
                    coroutineScope.launch {
                        printerManager.openCashDrawer(cashierPrinterIp)
                            .onSuccess { Toast.makeText(context, "Laci Kasir Terbuka (RJ11 Kick)", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(context, "Laci Kasir Terbuka (Simulasi Lokal)", Toast.LENGTH_SHORT).show() }
                    }
                },
                onSendToKitchen = {
                    // Dine-In Trigger: Send unprinted items to kitchen
                    coroutineScope.launch {
                        val stationItems = cart.map {
                            KdsTicketRouter.StationItem(
                                name = it.product.name,
                                quantity = it.quantity,
                                prepStation = it.product.prepStation,
                                notes = it.notes
                            )
                        }
                        val orderNum = "ORD-" + System.currentTimeMillis().toString().takeLast(8)
                        val routingResult = ticketRouter.routeOrderDynamic(
                            orderNumber = orderNum,
                            tableOrQueue = tableName,
                            items = stationItems,
                            stations = stationConfigs,
                            cashierPrinterIp = cashierPrinterIp,
                            deliveryMode = KdsTicketRouter.OrderDeliveryMode.DINE_IN,
                            roundNumber = dineInRound
                        )

                        if (routingResult.hasAnyFailover) {
                            failoverAlert = "Perhatian: Sebagian printer stasiun offline. Tiket dialihkan ke printer backup!"
                        } else {
                            failoverAlert = null
                        }

                        dineInRound++
                        cart.forEach { it.isSentToKitchen = true }
                        Toast.makeText(context, context.getString(R.string.toast_kitchen_sent, dineInRound - 1), Toast.LENGTH_LONG).show()
                    }
                },
                onOpenCashModal = { showCashModal = true },
                onOpenQrisModal = {},
                failoverWarning = failoverAlert,
                onReprintStations = {
                    coroutineScope.launch {
                        val stationItems = cart.map {
                            KdsTicketRouter.StationItem(
                                name = it.product.name,
                                quantity = it.quantity,
                                prepStation = it.product.prepStation,
                                notes = it.notes
                            )
                        }
                        val orderNum = "REPRINT-" + System.currentTimeMillis().toString().takeLast(6)
                        ticketRouter.routeOrderDynamic(
                            orderNumber = orderNum,
                            tableOrQueue = tableName,
                            items = stationItems,
                            stations = stationConfigs,
                            cashierPrinterIp = cashierPrinterIp,
                            deliveryMode = deliveryMode,
                            roundNumber = dineInRound
                        )
                        Toast.makeText(context, "Cetak ulang tiket stasiun selesai", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Modal 1: Pembayaran Tunai
        val pricedQuote = quote
        if (showCashModal && pricedQuote != null) {
            PosCashModal(
                grandTotal = pricedQuote.totalAmount,
                rupiahFormat = rupiahFormat,
                onDismiss = { showCashModal = false },
                onConfirmCashPayment = { tenderedLong, change ->
                    coroutineScope.launch {
                        // 1. Kick Drawer RJ11
                        printerManager.openCashDrawer(cashierPrinterIp)

                        // 2. Build Cashier Receipt
                        val orderNum = "ORD-" + System.currentTimeMillis().toString().takeLast(8)
                        val receiptBuilder = EscPosReceiptBuilder(EscPosReceiptBuilder.PaperWidth.WIDTH_80MM)
                        val receiptBytes = receiptBuilder.buildStoreReceipt(
                            storeName = "Central Cafe Senopati",
                            storeAddress = "Jl. Senopati No. 18, Jakarta Selatan",
                            orderId = orderNum,
                            tableName = tableName,
                            cashierName = "Kasir 1",
                            items = cart.map { EscPosReceiptBuilder.ReceiptItem(it.product.name, it.quantity, it.product.basePriceIdr) },
                            subtotalIdr = pricedQuote.subtotalAmount,
                            pb1TaxIdr = pricedQuote.taxAmount,
                            grandTotalIdr = pricedQuote.totalAmount,
                            paymentMethod = "TUNAI",
                            cashTenderedIdr = tenderedLong,
                            changeIdr = change
                        )

                        // 3. Dispatch to KDS Stations
                        val stationItems = cart.map {
                            KdsTicketRouter.StationItem(
                                name = it.product.name,
                                quantity = it.quantity,
                                prepStation = it.product.prepStation,
                                notes = it.notes
                            )
                        }
                        val res = ticketRouter.routeOrderDynamic(
                            orderNumber = orderNum,
                            tableOrQueue = tableName,
                            items = stationItems,
                            stations = stationConfigs,
                            cashierPrinterIp = cashierPrinterIp,
                            cashierReceiptBytes = receiptBytes,
                            deliveryMode = deliveryMode,
                            roundNumber = dineInRound
                        )

                        failoverAlert = if (res.hasAnyFailover) "Perhatian: Sebagian printer offline. Tiket dialihkan." else null

                        Toast.makeText(context, context.getString(R.string.toast_cash_success), Toast.LENGTH_LONG).show()
                        showCashModal = false
                        cart.clear()
                        dineInRound = 1
                    }
                }
            )
        }

    }
}

/** Request body for the server quote; order types follow the API (dine_in, takeaway). */
internal fun quoteRequestOf(cart: List<CartItem>, orderType: String) = OrderQuoteRequest(
    orderType = if (orderType == "DINE-IN") "dine_in" else "takeaway",
    items = cart.map { OrderQuoteItem(productId = it.product.id, quantity = it.quantity) }
)
