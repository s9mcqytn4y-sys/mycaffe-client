package com.azuredevid.cassy.ui.pos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azuredevid.cassy.R
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import java.text.NumberFormat

/**
 * Panel 2: Product Catalog Grid (55% screen width in Landscape POS).
 * Displays search box, product cards with CDN WebP imagery, station badge, and stock count.
 * Accessible with WCAG 2.2 AA compliant labels and clear contrast.
 */
@Composable
fun PosCatalogGrid(
    gridWidth: Dp,
    displayedProducts: List<CatalogProduct>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoadingCatalog: Boolean,
    onProductSelected: (CatalogProduct) -> Unit,
    rupiahFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clearSearchDesc = stringResource(R.string.a11y_clear_search)

    Column(
        modifier = modifier
            .width(gridWidth)
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
                text = stringResource(R.string.catalog_title, displayedProducts.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.catalog_source_cdn),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // In-Field Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.semantics { contentDescription = clearSearchDesc }
                    ) {
                        Text("✕", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )

        if (isLoadingCatalog) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.catalog_loading), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else if (displayedProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.catalog_empty_search, searchQuery),
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
                            .clickable { onProductSelected(product) }
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

                                // Prep Station Badge (BAR vs HOT KITCHEN vs PASTRY vs RUNNER)
                                Surface(
                                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                                    color = when (product.prepStation.lowercase()) {
                                        "bar" -> MaterialTheme.colorScheme.tertiaryContainer
                                        "kitchen", "hot_kitchen" -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = product.prepStation.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = when (product.prepStation.lowercase()) {
                                            "bar" -> MaterialTheme.colorScheme.onTertiaryContainer
                                            "kitchen", "hot_kitchen" -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
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
                                    text = stringResource(R.string.stock_label, product.stockQuantity),
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
}
