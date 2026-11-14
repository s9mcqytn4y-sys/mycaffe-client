package com.azuredevid.cassy.ui.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.domain.model.QRServiceMode
import com.azuredevid.cassy.ui.settings.StoreSettingsUiEvent
import com.azuredevid.cassy.ui.settings.StoreSettingsUiState
import com.azuredevid.cassy.ui.theme.statusWarnBg
import com.azuredevid.cassy.ui.theme.statusWarnFg

@Composable
fun StoreServiceModeTab(
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
                    text = stringResource(R.string.service_section_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                )
                Text(
                    text = stringResource(R.string.service_section_desc),
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
                Text(
                    text = stringResource(R.string.service_qr_mode_title),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                )
                Text(
                    text = stringResource(R.string.service_qr_mode_desc),
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ServiceModeOptionCard(
                        title = stringResource(R.string.service_mode_table_title),
                        description = stringResource(R.string.service_mode_table_desc),
                        selected = state.qrServiceMode == QRServiceMode.TABLE,
                        onSelect = { onEvent(StoreSettingsUiEvent.SetServiceMode(QRServiceMode.TABLE)) },
                        modifier = Modifier.weight(1f)
                    )

                    ServiceModeOptionCard(
                        title = stringResource(R.string.service_mode_pickup_title),
                        description = stringResource(R.string.service_mode_pickup_desc),
                        selected = state.qrServiceMode == QRServiceMode.PICKUP,
                        onSelect = { onEvent(StoreSettingsUiEvent.SetServiceMode(QRServiceMode.PICKUP)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val pauseTitle = stringResource(R.string.service_pause_title)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (state.qrOrderingPaused) statusWarnBg() else colors.surface,
            border = BorderStroke(
                1.dp,
                if (state.qrOrderingPaused) statusWarnFg() else colors.outline
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pauseTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (state.qrOrderingPaused) statusWarnFg() else colors.onSurface
                        )
                    )
                    Text(
                        text = stringResource(
                            if (state.qrOrderingPaused) {
                                R.string.service_pause_active_desc
                            } else {
                                R.string.service_pause_idle_desc
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (state.qrOrderingPaused) statusWarnFg() else colors.onSurfaceVariant
                        )
                    )
                }

                Switch(
                    checked = state.qrOrderingPaused,
                    onCheckedChange = { onEvent(StoreSettingsUiEvent.ToggleEmergencyQrPause(it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onPrimary,
                        checkedTrackColor = statusWarnFg()
                    ),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = pauseTitle }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.service_table_limit_title),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.service_table_limit_label),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                        )
                        Text(
                            text = if (state.tableTimeLimitMinutes == 0) {
                                stringResource(R.string.service_table_limit_unlimited)
                            } else {
                                stringResource(R.string.service_table_limit_minutes, state.tableTimeLimitMinutes)
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val newLimit = (state.tableTimeLimitMinutes - 15).coerceAtLeast(0)
                                onEvent(StoreSettingsUiEvent.UpdateTableTimeLimit(newLimit))
                            },
                            enabled = state.tableTimeLimitMinutes > 0,
                            modifier = Modifier.size(width = 48.dp, height = 48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-15", style = MaterialTheme.typography.labelSmall)
                        }

                        Text(
                            text = stringResource(R.string.service_table_limit_chip, state.tableTimeLimitMinutes),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            ),
                            modifier = Modifier.width(64.dp),
                            textAlign = TextAlign.Center
                        )

                        OutlinedButton(
                            onClick = {
                                val newLimit = (state.tableTimeLimitMinutes + 15).coerceAtMost(240)
                                onEvent(StoreSettingsUiEvent.UpdateTableTimeLimit(newLimit))
                            },
                            enabled = state.tableTimeLimitMinutes < 240,
                            modifier = Modifier.size(width = 48.dp, height = 48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+15", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                HorizontalDivider(color = colors.outline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.service_table_warning_label),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                        )
                        Text(
                            text = stringResource(
                                R.string.service_table_warning_desc,
                                state.tableTimerWarningMinutes
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.service_table_warning_value,
                            state.tableTimerWarningMinutes
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    )
                }
            }
        }

        Button(
            onClick = { onEvent(StoreSettingsUiEvent.SaveStoreSettings) },
            enabled = !state.isSavingSettings,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            )
        ) {
            if (state.isSavingSettings) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colors.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.service_btn_saving))
            } else {
                Text(stringResource(R.string.service_btn_save))
            }
        }
    }
}

@Composable
private fun ServiceModeOptionCard(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.surfaceVariant else colors.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) colors.primary else colors.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (selected) colors.primary else Color.Transparent)
                        .border(2.dp, if (selected) colors.primary else colors.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.onPrimary)
                        )
                    }
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
            )
        }
    }
}
