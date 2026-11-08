package com.azuredevid.cassy.ui.pos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.ui.theme.StatusErrBgLight
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import com.azuredevid.cassy.ui.theme.StatusOkBgLight
import com.azuredevid.cassy.ui.theme.StatusOkFgLight

/**
 * Panel 1: Vertical Navigation Rail (10% screen width in Landscape POS).
 * Displays app monogram, category navigation, LAN printer status, and quick settings routes.
 */
@Composable
fun PosNavRail(
    railWidth: Dp,
    categories: List<Pair<String, String>>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    allProducts: List<CatalogProduct>,
    isPrinterOnline: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToPairing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(railWidth)
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
                // Cassy Monogram / App Title
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

                // Category Buttons
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
                                .clickable { onCategorySelected(catId) }
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

            // Bottom Actions: LAN Status Chip, Settings (BYOK) & Pairing
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
}

