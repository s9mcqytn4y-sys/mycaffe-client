package com.azuredevid.cassy.ui.shift.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.azuredevid.cassy.R
import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CashMovementModal(
    movementType: CashMovementType,
    amountInput: String,
    reason: String,
    isRecording: Boolean,
    errorMessage: String? = null,
    onMovementTypeChanged: (CashMovementType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onReasonChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rupiahFormatter = NumberFormat.getIntegerInstance(Locale("id", "ID"))
    val currentAmount = amountInput.toLongOrNull() ?: 0L
    val presets = listOf(10_000L, 20_000L, 50_000L, 100_000L)

    Dialog(onDismissRequest = { if (!isRecording) onDismiss() }) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = LightSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.shift_movement_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightInk
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.shift_movement_subtitle),
                    fontSize = 14.sp,
                    color = LightSecondaryInk
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Movement Type Toggle: Paid Out vs Cash In
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPaidOut = movementType == CashMovementType.PAID_OUT
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onMovementTypeChanged(CashMovementType.PAID_OUT) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPaidOut) StatusWarnFgLight else LightCanvas,
                        border = if (isPaidOut) null else androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.shift_movement_paid_out),
                                fontSize = 13.sp,
                                fontWeight = if (isPaidOut) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPaidOut) Color.White else LightInk
                            )
                        }
                    }

                    val isCashIn = movementType == CashMovementType.CASH_IN
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onMovementTypeChanged(CashMovementType.CASH_IN) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCashIn) CassyPandan else LightCanvas,
                        border = if (isCashIn) null else androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.shift_movement_cash_in),
                                fontSize = 13.sp,
                                fontWeight = if (isCashIn) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCashIn) Color.White else LightInk
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rupiah Display Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightCanvas, RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.shift_movement_amount_label),
                            fontSize = 12.sp,
                            color = LightSecondaryInk
                        )
                        Text(
                            text = "Rp " + rupiahFormatter.format(currentAmount),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (movementType.isDeduction) StatusWarnFgLight else CassyPandan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = currentAmount == preset
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onAmountChanged(preset.toString()) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CassyTinta else LightCanvas,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${preset / 1000}rb",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else LightInk
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Nominal TextField
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChanged,
                    label = { Text(stringResource(R.string.shift_movement_custom_amount)) },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CassyPandan,
                        focusedLabelColor = CassyPandan,
                        cursorColor = CassyPandan
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Reason TextField
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChanged,
                    label = { Text(stringResource(R.string.shift_movement_reason_label)) },
                    placeholder = { Text(stringResource(R.string.shift_movement_reason_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CassyPandan,
                        focusedLabelColor = CassyPandan,
                        cursorColor = CassyPandan
                    )
                )

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = StatusErrFgLight,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isRecording,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_cancel), color = LightInk)
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !isRecording,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CassyPandan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isRecording) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.shift_btn_save_movement),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
