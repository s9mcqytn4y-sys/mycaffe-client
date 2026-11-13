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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.ui.theme.StatusErrBgLight
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import com.azuredevid.cassy.ui.theme.StatusOkBgLight
import com.azuredevid.cassy.ui.theme.StatusOkFgLight

/**
 * Panel 1: Vertical Navigation Rail (10% screen width in Landscape POS).
 * Displays app monogram, category navigation, LAN printer status, and quick settings routes.
 * Accessible with WCAG 2.2 AA compliant semantics and touch targets.
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
    onNavigateToCatalog: () -> Unit = {},
    onNavigateToTables: () -> Unit = {},
    onNavigateToPairing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoDesc = stringResource(R.string.a11y_cassy_logo)

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
                // Cassy Monogram / App Title with A11y Semantics
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = logoDesc }
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
                    text = stringResource(R.string.app_name),
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
                val lanText = if (isPrinterOnline) stringResource(R.string.lan_status_ok) else stringResource(R.string.lan_status_off)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isPrinterOnline) StatusOkBgLight else StatusErrBgLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lanText,
                        color = if (isPrinterOnline) StatusOkFgLight else StatusErrFgLight,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToCatalog,
                        contentPadding = PaddingValues(2.dp),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.catalog_nav_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onNavigateToTables,
                        contentPadding = PaddingValues(2.dp),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.table_nav_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToSettings,
                        contentPadding = PaddingValues(2.dp),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.btn_byok_settings), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToPairing,
                        contentPadding = PaddingValues(2.dp),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.btn_pair_device), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
