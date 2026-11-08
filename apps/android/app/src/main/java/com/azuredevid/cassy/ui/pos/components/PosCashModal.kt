package com.azuredevid.cassy.ui.pos.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.azuredevid.cassy.ui.theme.StatusErrBgLight
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import com.azuredevid.cassy.ui.theme.StatusOkBgLight
import com.azuredevid.cassy.ui.theme.StatusOkFgLight
import java.text.NumberFormat

/**
 * Cash settlement modal with preset chips (Pas, 50k, 100k, 200k),
 * manual tender input, and real-time change calculation.
 */
@Composable
fun PosCashModal(
    grandTotal: Long,
    rupiahFormat: NumberFormat,
    onDismiss: () -> Unit,
    onConfirmCashPayment: (tendered: Long, change: Long) -> Unit
) {
    var cashTenderedStr by remember { mutableStateOf(grandTotal.toString()) }
    val tenderedLong = cashTenderedStr.toLongOrNull() ?: 0L
    val change = tenderedLong - grandTotal
    val isSufficient = tenderedLong >= grandTotal

    AlertDialog(
        onDismissRequest = onDismiss,
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

                // Quick Cash Chips
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
                onClick = { onConfirmCashPayment(tenderedLong, change) },
                enabled = isSufficient,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Selesaikan & Cetak Struk", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Batal")
            }
        }
    )
}

