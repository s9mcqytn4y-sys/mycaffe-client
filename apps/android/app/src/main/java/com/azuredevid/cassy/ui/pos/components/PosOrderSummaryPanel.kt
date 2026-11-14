package com.azuredevid.cassy.ui.pos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.data.model.OrderQuote
import com.azuredevid.cassy.ui.pos.CartItem
import com.azuredevid.cassy.ui.theme.statusErrBg
import com.azuredevid.cassy.ui.theme.statusErrFg
import com.azuredevid.cassy.ui.theme.statusWarnBg
import com.azuredevid.cassy.ui.theme.statusWarnFg
import java.text.NumberFormat

/**
 * Panel 3: Active Order Summary & Settlement Panel (35% screen width).
 * Accessible with WCAG 2.2 AA compliant labels, 48dp touch targets, editable table name, and QRIS action.
 */
@Composable
fun PosOrderSummaryPanel(
    panelWidth: Dp,
    tableName: String,
    orderType: String,
    onTableNameChanged: (String) -> Unit = {},
    onToggleOrderType: () -> Unit,
    cart: List<CartItem>,
    isCartLocked: Boolean,
    isShiftOpen: Boolean = true,
    isSubmittingOrder: Boolean = false,
    onIncreaseQuantity: (CartItem) -> Unit,
    onDecreaseQuantity: (CartItem) -> Unit,
    quote: OrderQuote?,
    quoteError: String? = null,
    rupiahFormat: NumberFormat,
    onOpenDrawerKick: () -> Unit,
    onSendToKitchen: () -> Unit,
    onOpenCashModal: () -> Unit,
    onOpenQrisModal: () -> Unit,
    failoverWarning: String?,
    onReprintStations: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTableDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .width(panelWidth)
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
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headingLabel = if (orderType == "TAKEAWAY") {
                        stringResource(R.string.order_heading_takeaway)
                    } else {
                        val displayTable = tableName.ifBlank { stringResource(R.string.order_table_unspecified) }
                        stringResource(R.string.order_heading, displayTable)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable(enabled = orderType == "DINE-IN" && !isCartLocked) {
                                showTableDialog = true
                            }
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = headingLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCartLocked) {
                            Text(
                                text = stringResource(R.string.cart_locked_notice),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    val typeText = if (orderType == "DINE-IN") {
                        stringResource(R.string.order_type_dine_in)
                    } else {
                        stringResource(R.string.order_type_takeaway)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (orderType == "DINE-IN") statusWarnBg() else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable(enabled = !isCartLocked) { onToggleOrderType() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = typeText,
                                color = if (orderType == "DINE-IN") statusWarnFg() else MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!failoverWarning.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusErrBg(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = failoverWarning,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusErrFg(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onReprintStations,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    stringResource(R.string.btn_reprint_stations),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.cart_empty_title),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.cart_empty_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cart, key = { it.product.id }) { item ->
                            val decreaseDesc = stringResource(R.string.a11y_decrease_qty, item.product.name)
                            val increaseDesc = stringResource(R.string.a11y_increase_qty, item.product.name)

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
                                        onClick = { onDecreaseQuantity(item) },
                                        enabled = !isCartLocked,
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .semantics { contentDescription = decreaseDesc }
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }

                                    Text(
                                        text = "${item.quantity}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.widthIn(min = 24.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    OutlinedButton(
                                        onClick = { onIncreaseQuantity(item) },
                                        enabled = !isCartLocked,
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .semantics { contentDescription = increaseDesc }
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HorizontalDivider()

                if (quote == null) {
                    if (!quoteError.isNullOrBlank()) {
                        Text(
                            text = quoteError,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusErrFg()
                        )
                    } else if (cart.isNotEmpty()) {
                        Text(stringResource(R.string.total_from_server), style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.subtotal_label), style = MaterialTheme.typography.bodySmall)
                        Text("Rp ${rupiahFormat.format(quote.subtotalAmount)}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }

                    if (quote.serviceChargeAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.service_label), style = MaterialTheme.typography.bodySmall)
                            Text("Rp ${rupiahFormat.format(quote.serviceChargeAmount)}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.tax_pb1_label), style = MaterialTheme.typography.bodySmall)
                        Text("Rp ${rupiahFormat.format(quote.taxAmount)}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.grand_total_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Rp ${rupiahFormat.format(quote.totalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenDrawerKick,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(stringResource(R.string.btn_kick_drawer), style = MaterialTheme.typography.labelSmall)
                    }

                    if (orderType == "DINE-IN") {
                        FilledTonalButton(
                            onClick = onSendToKitchen,
                            enabled = cart.isNotEmpty() && !isCartLocked,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(stringResource(R.string.btn_send_to_kitchen), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenCashModal,
                        enabled = quote != null && cart.isNotEmpty() && !isCartLocked,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.btn_pay_cash), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onOpenQrisModal,
                        enabled = isShiftOpen && quote != null && cart.isNotEmpty() && !isCartLocked && !isSubmittingOrder,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.btn_pay_qris), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showTableDialog) {
        var draft by remember(tableName) { mutableStateOf(tableName) }
        AlertDialog(
            onDismissRequest = { showTableDialog = false },
            title = { Text(stringResource(R.string.order_table_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(stringResource(R.string.order_table_input_label)) },
                    placeholder = { Text(stringResource(R.string.order_table_input_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onTableNameChanged(draft)
                        showTableDialog = false
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.btn_apply))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showTableDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
