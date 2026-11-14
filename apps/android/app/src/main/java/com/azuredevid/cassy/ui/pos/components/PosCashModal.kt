package com.azuredevid.cassy.ui.pos.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.ui.theme.statusErrBg
import com.azuredevid.cassy.ui.theme.statusErrFg
import com.azuredevid.cassy.ui.theme.statusOkBg
import com.azuredevid.cassy.ui.theme.statusOkFg
import java.text.NumberFormat

/**
 * Cash settlement modal with preset chips, manual tender input, and server-backed submit state.
 */
@Composable
fun PosCashModal(
    grandTotal: Long,
    rupiahFormat: NumberFormat,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirmCashPayment: (tendered: Long, change: Long) -> Unit
) {
    var cashTenderedStr by remember { mutableStateOf(grandTotal.toString()) }
    val tenderedLong = cashTenderedStr.toLongOrNull() ?: 0L
    val change = tenderedLong - grandTotal
    val isSufficient = tenderedLong >= grandTotal

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(stringResource(R.string.modal_cash_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.modal_cash_total, "Rp ${rupiahFormat.format(grandTotal)}"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(stringResource(R.string.modal_cash_quick_prompt), style = MaterialTheme.typography.labelSmall)

                // Quick Cash Chips (Min Height 48dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { cashTenderedStr = grandTotal.toString() },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text(stringResource(R.string.chip_exact_cash), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { cashTenderedStr = "50000" },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("50k", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { cashTenderedStr = "100000" },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("100k", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { cashTenderedStr = "200000" },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("200k", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Nominal Input
                OutlinedTextField(
                    value = cashTenderedStr,
                    onValueChange = { cashTenderedStr = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.input_cash_tendered_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Kembalian Banner
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSufficient) statusOkBg() else statusErrBg(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (isSufficient) {
                            Text(
                                text = stringResource(R.string.change_label, "Rp ${rupiahFormat.format(change)}"),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                color = statusOkFg()
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.insufficient_cash, "Rp ${rupiahFormat.format(grandTotal - tenderedLong)}"),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = statusErrFg()
                            )
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusErrFg(),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmCashPayment(tenderedLong, change) },
                enabled = isSufficient && !isSubmitting,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.btn_complete_and_print), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
