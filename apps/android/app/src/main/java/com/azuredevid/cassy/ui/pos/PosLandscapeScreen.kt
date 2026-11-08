package com.azuredevid.cassy.ui.pos

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.PaymentChannelRegistry
import com.azuredevid.cassy.data.repository.KtorCatalogRepository
import com.azuredevid.cassy.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

data class CartItem(
    val product: CatalogProduct,
    var quantity: Int = 1,
    var notes: String = ""
)

@Composable
fun PosLandscapeScreen(
    catalogRepository: com.azuredevid.cassy.domain.repository.CatalogRepository? = null,
    onOpenDrawerKick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(catalogRepository) {
        catalogRepository ?: KtorCatalogRepository(context)
    }

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
    val cart = remember { mutableStateListOf<CartItem>() }
    var showQrisModal by remember { mutableStateOf(false) }
    var isPrinterOnline by remember { mutableStateOf(true) }

    // Categories
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

    val displayedProducts = remember(selectedCategoryId, allProducts) {
        if (selectedCategoryId == "all") {
            allProducts
        } else {
            allProducts.filter { it.categoryId == selectedCategoryId }
        }
    }

    // Calculations
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        // Category Buttons (Icons / Short text)
                        categories.forEach { (catId, label) ->
                            val isSelected = selectedCategoryId == catId
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategoryId = catId }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Bottom Navigation: Hardware Status & Settings
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
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToSettings,
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("BYOK", style = MaterialTheme.typography.labelSmall)
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
                // Header Bar & Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Katalog Menu (${displayedProducts.size} Produk)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cloudflare R2 CDN Aktif",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
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
                                // AsyncImage WebP dari CDN R2
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                )

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
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Stok: ${product.stockQuantity}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                    // Header Order Meja
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pesanan Meja: T-04",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusWarnBgLight
                            ) {
                                Text(
                                    text = "DINE-IN",
                                    color = StatusWarnFgLight,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Cart List
                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Keranjang masih kosong\nPilih menu dari katalog",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Text("-")
                                            }

                                            Text(
                                                text = "${item.quantity}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )

                                            OutlinedButton(
                                                onClick = { item.quantity += 1 },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Text("+")
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
                            Text("Rp ${rupiahFormat.format(subtotal)}", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("PB1 (10%)", style = MaterialTheme.typography.bodySmall)
                            Text("Rp ${rupiahFormat.format(taxPb1)}", style = MaterialTheme.typography.bodySmall)
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
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Action: Buka Laci Kasir ESC/POS
                        OutlinedButton(
                            onClick = onOpenDrawerKick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buka Laci Kasir (ESC/POS RJ11)")
                        }

                        // Payment Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onOpenDrawerKick() // Cash settlement kicks drawer
                                    cart.clear()
                                },
                                enabled = cart.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Bayar Tunai")
                            }

                            Button(
                                onClick = { showQrisModal = true },
                                enabled = cart.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("QRIS Dinamis")
                            }
                        }
                    }
                }
            }
        }

        // Modal QRIS Dinamis
        if (showQrisModal) {
            AlertDialog(
                onDismissRequest = { showQrisModal = false },
                title = { Text("QRIS Dinamis Midtrans Core API") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Total Tagihan: Rp ${rupiahFormat.format(grandTotal)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Tampilkan QR Placeholder
                        Surface(
                            modifier = Modifier.size(180.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("QR CODE DINAMIS\n(MIDTRANS CORE API)", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scan menggunakan BCA, GoPay, ShopeePay, OVO, Dana, LinkAja",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showQrisModal = false
                            cart.clear()
                        }
                    ) {
                        Text("Simulasi Pembayaran Berhasil")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showQrisModal = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

