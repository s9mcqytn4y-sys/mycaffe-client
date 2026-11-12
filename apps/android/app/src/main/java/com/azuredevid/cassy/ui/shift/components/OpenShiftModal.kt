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
import com.azuredevid.cassy.ui.theme.CassyPandan
import com.azuredevid.cassy.ui.theme.LightBorder
import com.azuredevid.cassy.ui.theme.LightCanvas
import com.azuredevid.cassy.ui.theme.LightInk
import com.azuredevid.cassy.ui.theme.LightSecondaryInk
import com.azuredevid.cassy.ui.theme.LightSurface
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OpenShiftModal(
    openingFloatInput: String,
    notes: String,
    isOpening: Boolean,
    errorMessage: String? = null,
    onOpeningFloatChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rupiahFormatter = NumberFormat.getIntegerInstance(Locale("id", "ID"))
    val currentAmount = openingFloatInput.toLongOrNull() ?: 0L
    val presets = listOf(100_000L, 200_000L, 500_000L, 1_000_000L)

    Dialog(onDismissRequest = { if (!isOpening) onDismiss() }) {
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
                    text = stringResource(R.string.shift_open_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightInk
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.shift_open_subtitle),
                    fontSize = 14.sp,
                    color = LightSecondaryInk
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                            text = stringResource(R.string.shift_open_float_label),
                            fontSize = 12.sp,
                            color = LightSecondaryInk
                        )
                        Text(
                            text = "Rp " + rupiahFormatter.format(currentAmount),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = CassyPandan
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
                                .clickable { onOpeningFloatChanged(preset.toString()) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CassyPandan else LightCanvas,
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

                // Custom Input TextField
                OutlinedTextField(
                    value = openingFloatInput,
                    onValueChange = onOpeningFloatChanged,
                    label = { Text(stringResource(R.string.shift_open_custom_amount)) },
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

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChanged,
                    label = { Text(stringResource(R.string.shift_notes_label)) },
                    placeholder = { Text(stringResource(R.string.shift_open_notes_placeholder)) },
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

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isOpening,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_cancel), color = LightInk)
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !isOpening,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CassyPandan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isOpening) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.shift_btn_open),
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
