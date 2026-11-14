package com.azuredevid.cassy.ui.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.ui.settings.StoreSettingsUiEvent
import com.azuredevid.cassy.ui.settings.StoreSettingsUiState
import com.azuredevid.cassy.ui.theme.statusErrBg
import com.azuredevid.cassy.ui.theme.statusErrFg
import com.azuredevid.cassy.ui.theme.statusOkBg
import com.azuredevid.cassy.ui.theme.statusOkFg

@Composable
fun PrinterSettingsTab(
    state: StoreSettingsUiState,
    onEvent: (StoreSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.printer_tab_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                )
                Text(
                    text = stringResource(R.string.printer_tab_desc),
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.cashierPrinterIp,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateCashierPrinterIp(it)) },
                        label = { Text(stringResource(R.string.printer_cashier_ip_label)) },
                        placeholder = { Text(stringResource(R.string.printer_ip_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.weight(0.72f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )

                    OutlinedTextField(
                        value = state.cashierPrinterPort,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateCashierPrinterPort(it)) },
                        label = { Text(stringResource(R.string.printer_port_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.28f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.kitchenPrinterIp,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateKitchenPrinterIp(it)) },
                        label = { Text(stringResource(R.string.printer_kitchen_ip_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )

                    OutlinedTextField(
                        value = state.barPrinterIp,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateBarPrinterIp(it)) },
                        label = { Text(stringResource(R.string.printer_bar_ip_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.pastryPrinterIp,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdatePastryPrinterIp(it)) },
                        label = { Text(stringResource(R.string.printer_pastry_ip_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )

                    OutlinedTextField(
                        value = state.expediterPrinterIp,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateExpediterPrinterIp(it)) },
                        label = { Text(stringResource(R.string.printer_expediter_ip_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.printer_paper_width_label),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = state.paperWidthMm == 58,
                        onClick = { onEvent(StoreSettingsUiEvent.UpdatePaperWidthMm(58)) },
                        label = { Text(stringResource(R.string.printer_paper_58)) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                    FilterChip(
                        selected = state.paperWidthMm == 80,
                        onClick = { onEvent(StoreSettingsUiEvent.UpdatePaperWidthMm(80)) },
                        label = { Text(stringResource(R.string.printer_paper_80)) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }
        }

        if (state.printerTestMessage != null) {
            val bg = when (state.printerTestOk) {
                true -> statusOkBg()
                false -> statusErrBg()
                null -> colors.surfaceVariant
            }
            val fg = when (state.printerTestOk) {
                true -> statusOkFg()
                false -> statusErrFg()
                null -> colors.onSurface
            }
            Surface(
                color = bg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.printerTestMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = fg,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { onEvent(StoreSettingsUiEvent.TestCashierPrinter) },
                enabled = !state.isTestingPrinter && !state.isSavingPrinter,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.isTestingPrinter) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.printer_btn_test))
                }
            }

            Button(
                onClick = { onEvent(StoreSettingsUiEvent.SavePrinterConfig) },
                enabled = !state.isSavingPrinter && !state.isTestingPrinter,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.isSavingPrinter) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.printer_btn_saving))
                } else {
                    Text(stringResource(R.string.printer_btn_save))
                }
            }
        }
    }
}
