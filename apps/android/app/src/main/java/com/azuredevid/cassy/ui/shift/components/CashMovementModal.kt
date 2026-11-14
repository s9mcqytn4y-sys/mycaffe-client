package com.azuredevid.cassy.ui.shift.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.azuredevid.cassy.R
import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.ui.theme.statusErrFg
import com.azuredevid.cassy.ui.theme.statusWarnFg
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
    val colors = MaterialTheme.colorScheme
    val warnFg = statusWarnFg()

    Dialog(onDismissRequest = { if (!isRecording) onDismiss() }) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.shift_movement_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.shift_movement_subtitle),
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPaidOut = movementType == CashMovementType.PAID_OUT
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .selectable(
                                selected = isPaidOut,
                                role = Role.RadioButton,
                                onClick = { onMovementTypeChanged(CashMovementType.PAID_OUT) }
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPaidOut) warnFg else colors.background,
                        border = if (isPaidOut) null else BorderStroke(1.dp, colors.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.shift_movement_paid_out),
                                fontSize = 13.sp,
                                fontWeight = if (isPaidOut) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPaidOut) colors.surface else colors.onSurface
                            )
                        }
                    }

                    val isCashIn = movementType == CashMovementType.CASH_IN
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .selectable(
                                selected = isCashIn,
                                role = Role.RadioButton,
                                onClick = { onMovementTypeChanged(CashMovementType.CASH_IN) }
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCashIn) colors.primary else colors.background,
                        border = if (isCashIn) null else BorderStroke(1.dp, colors.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.shift_movement_cash_in),
                                fontSize = 13.sp,
                                fontWeight = if (isCashIn) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCashIn) colors.onPrimary else colors.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background, RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.shift_movement_amount_label),
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = "Rp " + rupiahFormatter.format(currentAmount),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (movementType.isDeduction) warnFg else colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = { onAmountChanged(preset.toString()) }
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) colors.onSurface else colors.background,
                            border = if (isSelected) null else BorderStroke(1.dp, colors.outline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${preset / 1000}rb",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.surface else colors.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChanged,
                    label = { Text(stringResource(R.string.shift_movement_custom_amount)) },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChanged,
                    label = { Text(stringResource(R.string.shift_movement_reason_label)) },
                    placeholder = { Text(stringResource(R.string.shift_movement_reason_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = statusErrFg(),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        Text(text = stringResource(R.string.btn_cancel), color = colors.onSurface)
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !isRecording,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isRecording) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.shift_btn_save_movement),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
