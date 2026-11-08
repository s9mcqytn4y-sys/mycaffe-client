package com.azuredevid.cassy.ui.pos.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.core.util.QrBitmapGenerator
import kotlinx.coroutines.delay
import java.text.NumberFormat

/**
 * Dynamic QRIS modal with live ZXing QR render, 15-minute countdown timer,
 * hybrid status sync (WebSocket/Polling status check), and Force Sync action.
 * Enforces Cart Lock while active.
 * Supports dual-language i18n and WCAG 2.2 AA accessibility.
 */
@Composable
fun PosQrisModal(
    grandTotal: Long,
    qrisPayload: String,
    isSyncing: Boolean,
    rupiahFormat: NumberFormat,
    onForceSyncPayment: () -> Unit,
    onCancelOrderAndReleaseLock: () -> Unit,
    onConfirmPaymentSuccess: () -> Unit
) {
    val qrBitmap = remember(qrisPayload) {
        QrBitmapGenerator.createQrBitmap(qrisPayload, 400, 400)
    }

    // 15 Minutes Countdown Timer
    var secondsRemaining by remember { mutableStateOf(900) } // 15 mins * 60
    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = remember(secondsRemaining) {
        String.format("%02d:%02d", minutes, seconds)
    }

    AlertDialog(
        onDismissRequest = onCancelOrderAndReleaseLock,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.modal_qris_title), fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.qris_expiry_format, timeFormatted),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.modal_cash_total, "Rp ${rupiahFormat.format(grandTotal)}"),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // QR Code Bitmap Render via ZXing
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.modal_qris_title),
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(200.dp),
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
                    text = stringResource(R.string.qris_payment_instructions),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status & Force Sync Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isSyncing) stringResource(R.string.qris_syncing_status) else stringResource(R.string.qris_waiting_status),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onForceSyncPayment,
                    enabled = !isSyncing,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.btn_force_sync), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPaymentSuccess,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.btn_confirm_paid), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancelOrderAndReleaseLock,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.btn_cancel_qris))
            }
        }
    )
}
