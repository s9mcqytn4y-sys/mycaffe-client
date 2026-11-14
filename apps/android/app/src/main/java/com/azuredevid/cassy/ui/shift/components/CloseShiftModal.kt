package com.azuredevid.cassy.ui.shift.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.azuredevid.cassy.R
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.ui.theme.statusErrFg
import com.azuredevid.cassy.ui.theme.statusOkBg
import com.azuredevid.cassy.ui.theme.statusOkFg
import com.azuredevid.cassy.ui.theme.statusWarnBg
import com.azuredevid.cassy.ui.theme.statusWarnFg
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun CloseShiftModal(
    currentShift: Shift?,
    countedCashInput: String,
    varianceReason: String,
    notes: String,
    supervisorPinInput: String,
    calculatedVariance: Long?,
    isSupervisorAuthRequired: Boolean,
    printReceiptOnClose: Boolean,
    isClosing: Boolean,
    errorMessage: String? = null,
    onCountedCashChanged: (String) -> Unit,
    onVarianceReasonChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSupervisorPinChanged: (String) -> Unit,
    onPrintReceiptToggled: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rupiahFormatter = NumberFormat.getIntegerInstance(Locale("id", "ID"))
    val countedAmount = countedCashInput.toLongOrNull()
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = { if (!isClosing) onDismiss() }) {
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
                    text = stringResource(R.string.shift_close_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.shift_close_subtitle),
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = countedCashInput,
                    onValueChange = onCountedCashChanged,
                    label = { Text(stringResource(R.string.shift_counted_cash_label)) },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (currentShift?.expectedCash == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.shift_blind_count_hint),
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )
                }

                if (countedAmount != null && calculatedVariance != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val isBalance = calculatedVariance == 0L
                    val isOver = calculatedVariance > 0L
                    val bgBox = when {
                        isBalance -> statusOkBg()
                        isSupervisorAuthRequired -> statusWarnBg()
                        else -> colors.background
                    }
                    val textCol = when {
                        isBalance -> statusOkFg()
                        isSupervisorAuthRequired -> statusWarnFg()
                        else -> colors.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgBox, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.shift_variance_status_label),
                                fontSize = 12.sp,
                                color = colors.onSurfaceVariant
                            )
                            val varianceStr = when {
                                isBalance -> stringResource(R.string.shift_variance_balance)
                                isOver -> stringResource(R.string.shift_variance_over, rupiahFormatter.format(calculatedVariance))
                                else -> stringResource(R.string.shift_variance_short, rupiahFormatter.format(abs(calculatedVariance)))
                            }
                            Text(
                                text = varianceStr,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textCol
                            )
                        }
                    }
                }

                if (isSupervisorAuthRequired) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(statusWarnBg(), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.shift_supervisor_gate_warning),
                            fontSize = 12.sp,
                            color = statusWarnFg(),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = varianceReason,
                        onValueChange = onVarianceReasonChanged,
                        label = { Text(stringResource(R.string.shift_variance_reason_label)) },
                        placeholder = { Text(stringResource(R.string.shift_variance_reason_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = supervisorPinInput,
                        onValueChange = onSupervisorPinChanged,
                        label = { Text(stringResource(R.string.shift_supervisor_pin_label)) },
                        placeholder = { Text("******") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChanged,
                    label = { Text(stringResource(R.string.shift_notes_label)) },
                    placeholder = { Text(stringResource(R.string.shift_close_notes_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .toggleable(
                            value = printReceiptOnClose,
                            role = Role.Checkbox,
                            onValueChange = onPrintReceiptToggled
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = printReceiptOnClose,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.shift_print_zreport_checkbox),
                        fontSize = 14.sp,
                        color = colors.onSurface
                    )
                }

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
                        enabled = !isClosing,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_cancel), color = colors.onSurface)
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !isClosing,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.onSurface,
                            contentColor = colors.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isClosing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.surface,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.shift_btn_close),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
