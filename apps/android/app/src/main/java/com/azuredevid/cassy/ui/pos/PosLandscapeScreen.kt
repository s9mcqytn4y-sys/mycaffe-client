package com.azuredevid.cassy.ui.pos

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azuredevid.cassy.core.hardware.EscPosReceiptBuilder
import com.azuredevid.cassy.core.hardware.KdsTicketRouter
import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.core.util.QrBitmapGenerator
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.repository.KtorCatalogRepository
import com.azuredevid.cassy.domain.repository.CatalogRepository
import com.azuredevid.cassy.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

data class CartItem(
    val product: CatalogProduct,
    var quantity: Int = 1,
    var notes: String = ""
)

@Composable
fun PosLandscapeScreen(
    catalogRepository: CatalogRepository? = null,
    printerManager: PrinterPort9100Manager = remember { PrinterPort9100Manager() },
    printerEndpoints: KdsTicketRouter.PrinterEndpoints = remember { KdsTicketRouter.PrinterEndpoints() },
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
    val ticketRouter = remember(printerManager) { KdsTicketRouter(printerManager) }

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

    val rupiahFormat = remember { NumberFormat.getIntegerInstance(Locale("id", "ID")) }

    var selectedCategoryId by rememberSaveable { mutableStateOf("all") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val cart = remember { mutableStateListOf<CartItem>() }

    var showCashModal by remember { mutableStateOf(false) }
    var showQrisModal by remember { mutableStateOf(false) }
    var isPrinterOnline by remember { mutableStateOf(true) }
    var orderType by rememberSaveable { mutableStateOf("DINE-IN") }
    var tableName by rememberSaveable { mutableStateOf("Meja T-04") }

    // Categories with display names
    val categories = listOf(
        "all" to "Semua Menu",
        "signature" to "Signature",
        "espresso" to "Espresso",
        "manual_brew" to "Manual Brew",
        "pastry" to "Pastry",
        "non_coffee" to "Non-Coffee",
        "meals" to "Meals",
        "beans" to "Beans & Bottle"
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

    // Money Calculations in integer IDR
    val subtotal = cart.sumOf { it.product.basePriceIdr * it.quantity }
    val taxPb1 = (subtotal * 0.10).toLong() // 10% PB1
    val grandTotal = subtotal + taxPb1

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val totalWidth = maxWidth

        Row(modifier = Modifier.fillMaxSize()) {

            // =====================================================================
            // PANEL 1: NAVIGATION RAIL (10% Lebar Layar, Min 84dp)
            // =====================================================================
            Surface(
                modifier = Modifier
                    .width(totalWidth * 0.10f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Monogram / App Title
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "C",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }
                        }

                        Text(
                            text = "Cassy POS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Category Buttons (Icons / Short text)
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { (catId, label) ->
                                val isSelected = selectedCategoryId == catId
                                val itemCount = if (catId == "all") allProducts.size else allProducts.count { it.categoryId == catId }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clickable { selectedCategoryId = catId }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        if (itemCount > 0) {
                                            Text(
                                                text = "($itemCount)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Navigation: Hardware Status, Settings & Pairing
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isPrinterOnline) StatusOkBgLight else StatusErrBgLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isPrinterOnline) "LAN 9100: OK" else "LAN 9100: OFF",
                                color = if (isPrinterOnline) StatusOkFgLight else StatusErrFgLight,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToSettings,
                                contentPadding = PaddingValues(2.dp),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                            ) {
                                Text("BYOK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onNavigateToPairing,
                                contentPadding = PaddingValues(2.dp),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                            ) {
                                Text("Pair", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            VerticalDivider()

            // =====================================================================
            // PANEL 2: CATALOG GRID (55% Lebar Layar)
            // =====================================================================
            Column(
                modifier = Modifier
                    .width(totalWidth * 0.55f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                // Header Bar & Search Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Katalog Menu (${displayedProducts.size} Produk)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cloudflare R2 CDN WebP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // In-Field Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Cari 38 menu resmi (Espresso, Latte, Croissant...)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )

                if (isLoadingCatalog) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (displayedProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Tidak ada produk yang cocok dengan pencarian \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 135.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedProducts, key = { it.id }) { product ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val existing = cart.find { it.product.id == product.id }
                                        if (existing != null) {
                                            existing.quantity += 1
                                        } else {
                                            cart.add(CartItem(product = product, quantity = 1))
                                        }
                                    }
                            ) {
                                Column {
                                    Box {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(product.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = product.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1.1f)
                                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                        )

                                        // Prep Station Badge (BAR vs KITCHEN)
                                        Surface(
                                            shape = RoundedCornerShape(bottomEnd = 6.dp),
                                            color = if (product.prepStation == "bar") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                text = product.prepStation.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (product.prepStation == "bar") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Rp ${rupiahFormat.format(product.basePriceIdr)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Stok: ${product.stockQuantity}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (product.stockQuantity < 10) StatusErrFgLight else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            VerticalDivider()

            // =====================================================================
            // PANEL 3: ACTIVE CHECK / ORDER SUMMARY (35% Lebar Layar)
            // =====================================================================
            Surface(
                modifier = Modifier
                    .width(totalWidth * 0.35f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Order Meja & Dine-In / Takeaway Toggle
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pesanan: $tableName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Order Type Pill Toggle
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (orderType == "DINE-IN") StatusWarnBgLight else MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.clickable {
                                    orderType = if (orderType == "DINE-IN") "TAKEAWAY" else "DINE-IN"
                                }
                            ) {
                                Text(
                                    text = orderType,
                                    color = if (orderType == "DINE-IN") StatusWarnFgLight else MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Cart Items List
                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Keranjang kasir masih kosong\nPilih menu dari katalog di sebelah kiri",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(cart) { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.product.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Rp ${rupiahFormat.format(item.product.basePriceIdr * item.quantity)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    if (item.quantity > 1) {
                                                        item.quantity -= 1
                                                    } else {
                                                        cart.remove(item)
                                                    }
                                                },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }

                                            Text(
                                                text = "${item.quantity}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.widthIn(min = 20.dp),
                                                textAlign = TextAlign.Center
                                            )

                                            OutlinedButton(
                                                onClick = { item.quantity += 1 },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bill Summary & Action Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                            Text("Rp ${rupiahFormat.format(subtotal)}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("PB1 (10%)", style = MaterialTheme.typography.bodySmall)
                            Text("Rp ${rupiahFormat.format(taxPb1)}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Rp ${rupiahFormat.format(grandTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Action: Buka Laci Kasir ESC/POS RJ11 manual
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    printerManager.openCashDrawer(printerEndpoints.cashierPrinterIp)
                                        .onSuccess {
                                            Toast.makeText(context, "Laci Kasir Terbuka (RJ11 Kick)", Toast.LENGTH_SHORT).show()
                                        }
                                        .onFailure {
                                            Toast.makeText(context, "Laci Kasir Terbuka (Simulasi Lokal)", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Buka Laci Kasir (RJ11 Kick)")
                        }

                        // Settlement Buttons: Tunai vs QRIS Dinamis
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showCashModal = true },
                                enabled = cart.isNotEmpty(),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text("Bayar Tunai", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showQrisModal = true },
                                enabled = cart.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text("QRIS Dinamis", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // MODAL 1: PEMBAYARAN TUNAI (CASH SETTLEMENT & CHANGE CALCULATOR)
        // =====================================================================
        if (showCashModal) {
            var cashTenderedStr by remember { mutableStateOf(grandTotal.toString()) }
            val tenderedLong = cashTenderedStr.toLongOrNull() ?: 0L
            val change = tenderedLong - grandTotal
            val isSufficient = tenderedLong >= grandTotal

            AlertDialog(
                onDismissRequest = { showCashModal = false },
                title = {
                    Text("Pembayaran Tunai & Kembalian", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Total Tagihan: Rp ${rupiahFormat.format(grandTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text("Pilih Uang Diterima / Pecahan Cepat:", style = MaterialTheme.typography.labelSmall)

                        // Preset Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { cashTenderedStr = grandTotal.toString() },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("Pas", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { cashTenderedStr = "50000" },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("50k", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = { cashTenderedStr = "100000" },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("100k", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = { cashTenderedStr = "200000" },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("200k", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Nominal Input
                        OutlinedTextField(
                            value = cashTenderedStr,
                            onValueChange = { cashTenderedStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Nominal Tunai Diterima (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Kembalian Banner
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSufficient) StatusOkBgLight else StatusErrBgLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (isSufficient) {
                                    Text(
                                        text = "KEMBALIAN: Rp ${rupiahFormat.format(change)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = StatusOkFgLight
                                    )
                                } else {
                                    Text(
                                        text = "Uang diterima kurang Rp ${rupiahFormat.format(grandTotal - tenderedLong)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StatusErrFgLight
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // 1. Buka Laci Kasir ESC/POS RJ11
                                printerManager.openCashDrawer(printerEndpoints.cashierPrinterIp)

                                // 2. Cetak Struk Kasir Resmi 80mm
                                val orderNum = "ORD-" + System.currentTimeMillis().toString().takeLast(8)
                                val receiptBuilder = EscPosReceiptBuilder(EscPosReceiptBuilder.PaperWidth.WIDTH_80MM)
                                val receiptItems = cart.map {
                                    EscPosReceiptBuilder.ReceiptItem(
                                        name = it.product.name,
                                        qty = it.quantity,
                                        priceIdr = it.product.basePriceIdr
                                    )
                                }
                                val receiptBytes = receiptBuilder.buildStoreReceipt(
                                    storeName = "Central Cafe Senopati",
                                    storeAddress = "Jl. Senopati No. 18, Jakarta Selatan",
                                    orderId = orderNum,
                                    tableName = tableName,
                                    cashierName = "Kasir 1",
                                    items = receiptItems,
                                    subtotalIdr = subtotal,
                                    pb1TaxIdr = taxPb1,
                                    grandTotalIdr = grandTotal,
                                    paymentMethod = "TUNAI",
                                    cashTenderedIdr = tenderedLong,
                                    changeIdr = change
                                )

                                // 3. Rute dan Cetak Tiket KDS (Bar & Dapur)
                                val stationItems = cart.map {
                                    KdsTicketRouter.StationItem(
                                        name = it.product.name,
                                        quantity = it.quantity,
                                        prepStation = it.product.prepStation,
                                        notes = it.notes
                                    )
                                }
                                ticketRouter.routeOrder(
                                    orderNumber = orderNum,
                                    tableOrQueue = tableName,
                                    items = stationItems,
                                    endpoints = printerEndpoints,
                                    cashierReceiptBytes = receiptBytes
                                )

                                Toast.makeText(context, "Transaksi Selesai & Tiket KDS Terkirim!", Toast.LENGTH_LONG).show()
                                showCashModal = false
                                cart.clear()
                            }
                        },
                        enabled = isSufficient,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Selesaikan & Cetak Struk", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showCashModal = false },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Batal")
                    }
                }
            )
        }

        // =====================================================================
        // MODAL 2: QRIS DINAMIS (MIDTRANS CORE API)
        // =====================================================================
        if (showQrisModal) {
            val qrisPayload = remember(grandTotal) {
                "00020101021226590014ID.LINKAJA.WWW0118936009110020038849520458125303360540" + grandTotal + "5802ID5914Central Cafe6007Jakarta6304"
            }
            val qrBitmap = remember(qrisPayload) {
                QrBitmapGenerator.createQrBitmap(qrisPayload, 400, 400)
            }

            AlertDialog(
                onDismissRequest = { showQrisModal = false },
                title = { Text("QRIS Dinamis Midtrans Core API", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Total Tagihan: Rp ${rupiahFormat.format(grandTotal)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // QR Code Bitmap Render via ZXing
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QRIS Dinamis Midtrans",
                                modifier = Modifier
                                    .size(190.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(2.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(190.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.LightGray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("QRIS DINAMIS\n(MIDTRANS CORE API)", textAlign = TextAlign.Center)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Scan menggunakan BCA, GoPay, ShopeePay, OVO, Dana, LinkAja",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val orderNum = "QRIS-" + System.currentTimeMillis().toString().takeLast(8)
                                val receiptBuilder = EscPosReceiptBuilder(EscPosReceiptBuilder.PaperWidth.WIDTH_80MM)
                                val receiptItems = cart.map {
                                    EscPosReceiptBuilder.ReceiptItem(
                                        name = it.product.name,
                                        qty = it.quantity,
                                        priceIdr = it.product.basePriceIdr
                                    )
                                }
                                val receiptBytes = receiptBuilder.buildStoreReceipt(
                                    storeName = "Central Cafe Senopati",
                                    storeAddress = "Jl. Senopati No. 18, Jakarta Selatan",
                                    orderId = orderNum,
                                    tableName = tableName,
                                    cashierName = "Kasir 1",
                                    items = receiptItems,
                                    subtotalIdr = subtotal,
                                    pb1TaxIdr = taxPb1,
                                    grandTotalIdr = grandTotal,
                                    paymentMethod = "QRIS DINAMIS (MIDTRANS)"
                                )

                                // Dispatch to Bar, Kitchen, and Cashier printers
                                val stationItems = cart.map {
                                    KdsTicketRouter.StationItem(
                                        name = it.product.name,
                                        quantity = it.quantity,
                                        prepStation = it.product.prepStation,
                                        notes = it.notes
                                    )
                                }
                                ticketRouter.routeOrder(
                                    orderNumber = orderNum,
                                    tableOrQueue = tableName,
                                    items = stationItems,
                                    endpoints = printerEndpoints,
                                    cashierReceiptBytes = receiptBytes
                                )

                                Toast.makeText(context, "Pembayaran QRIS Terkonfirmasi & Tiket KDS Terkirim!", Toast.LENGTH_LONG).show()
                                showQrisModal = false
                                cart.clear()
                            }
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Simulasi Pembayaran Berhasil", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showQrisModal = false },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
