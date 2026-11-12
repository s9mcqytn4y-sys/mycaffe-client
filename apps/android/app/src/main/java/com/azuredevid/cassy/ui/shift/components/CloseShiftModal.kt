package com.azuredevid.cassy.ui.shift.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.azuredevid.cassy.R
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

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

    Dialog(onDismissRequest = { if (!isClosing) onDismiss() }) {
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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.shift_close_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightInk
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.shift_close_subtitle),
                    fontSize = 14.sp,
                    color = LightSecondaryInk
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Input Fisik Uang Kas (Blind Cash Count)
                OutlinedTextField(
                    value = countedCashInput,
                    onValueChange = onCountedCashChanged,
                    label = { Text(stringResource(R.string.shift_counted_cash_label)) },
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

                // Variance evaluation preview if user has inputted counted cash
                if (countedAmount != null && calculatedVariance != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val isBalance = calculatedVariance == 0L
                    val isOver = calculatedVariance > 0L
                    val bgBox = if (isBalance) StatusOkBgLight else if (isSupervisorAuthRequired) StatusWarnBgLight else LightCanvas
                    val textCol = if (isBalance) StatusOkFgLight else if (isSupervisorAuthRequired) StatusWarnFgLight else LightInk

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
                                color = LightSecondaryInk
                            )
                            val varianceStr = when {
                                isBalance -> stringResource(R.string.shift_variance_balance)
                                isOver -> stringResource(R.string.shift_variance_over, rupiahFormatter.format(calculatedVariance))
                                else -> stringResource(R.string.shift_variance_short, rupiahFormatter.format(Math.abs(calculatedVariance)))
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

                // Supervisor Gate if |variance| > 50.000
                if (isSupervisorAuthRequired) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusWarnBgLight, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.shift_supervisor_gate_warning),
                            fontSize = 12.sp,
                            color = StatusWarnFgLight,
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CassyKunyit,
                            focusedLabelColor = CassyKunyit,
                            cursorColor = CassyKunyit
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = supervisorPinInput,
                        onValueChange = onSupervisorPinChanged,
                        label = { Text(stringResource(R.string.shift_supervisor_pin_label)) },
                        placeholder = { Text("****") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CassyKunyit,
                            focusedLabelColor = CassyKunyit,
                            cursorColor = CassyKunyit
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChanged,
                    label = { Text(stringResource(R.string.shift_notes_label)) },
                    placeholder = { Text(stringResource(R.string.shift_close_notes_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CassyPandan,
                        focusedLabelColor = CassyPandan,
                        cursorColor = CassyPandan
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Print Z-Report Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = printReceiptOnClose,
                        onCheckedChange = onPrintReceiptToggled,
                        colors = CheckboxDefaults.colors(checkedColor = CassyPandan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.shift_print_zreport_checkbox),
                        fontSize = 14.sp,
                        color = LightInk
                    )
                }

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
                        enabled = !isClosing,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_cancel), color = LightInk)
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !isClosing,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CassyTinta),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isClosing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.shift_btn_close),
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
