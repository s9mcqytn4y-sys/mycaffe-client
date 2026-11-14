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
import com.azuredevid.cassy.domain.model.CashDrawer
import com.azuredevid.cassy.ui.theme.statusErrFg
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OpenShiftModal(
    drawers: List<CashDrawer> = emptyList(),
    selectedDrawerId: String? = null,
    onDrawerSelected: (String) -> Unit = {},
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
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = { if (!isOpening) onDismiss() }) {
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
                    text = stringResource(R.string.shift_open_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.shift_open_subtitle),
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant
                )

                if (drawers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.shift_drawer_select_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        drawers.forEach { drawer ->
                            val isSelected = drawer.id == selectedDrawerId
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .selectable(
                                        selected = isSelected,
                                        role = Role.RadioButton,
                                        onClick = { onDrawerSelected(drawer.id) }
                                    ),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) colors.primaryContainer else colors.surfaceVariant,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colors.primary else colors.outline
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = drawer.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.primary else colors.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background, RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.shift_open_float_label),
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = "Rp " + rupiahFormatter.format(currentAmount),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
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
                                    onClick = { onOpeningFloatChanged(preset.toString()) }
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) colors.primary else colors.background,
                            border = if (isSelected) null else BorderStroke(1.dp, colors.outline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${preset / 1000}rb",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.onPrimary else colors.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = openingFloatInput,
                    onValueChange = onOpeningFloatChanged,
                    label = { Text(stringResource(R.string.shift_open_custom_amount)) },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChanged,
                    label = { Text(stringResource(R.string.shift_notes_label)) },
                    placeholder = { Text(stringResource(R.string.shift_open_notes_placeholder)) },
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

                Spacer(modifier = Modifier.height(22.dp))

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
                        Text(text = stringResource(R.string.btn_cancel), color = colors.onSurface)
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !isOpening,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isOpening) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.shift_btn_open),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
