package com.azuredevid.cassy.ui.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.ui.settings.StoreSettingsUiEvent
import com.azuredevid.cassy.ui.settings.StoreSettingsUiState
import com.azuredevid.cassy.ui.theme.statusErrBg
import com.azuredevid.cassy.ui.theme.statusErrFg
import com.azuredevid.cassy.ui.theme.statusOkBg
import com.azuredevid.cassy.ui.theme.statusOkFg
import com.azuredevid.cassy.ui.theme.statusWarnBg
import com.azuredevid.cassy.ui.theme.statusWarnFg

@Composable
fun MidtransCredentialsTab(
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.midtrans_env_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    )
                    Text(
                        text = stringResource(
                            if (state.isSandbox) {
                                R.string.midtrans_env_sandbox
                            } else {
                                R.string.midtrans_env_production
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
                    )
                    val statusLabel = state.validationStatus.ifBlank {
                        stringResource(R.string.midtrans_status_unverified)
                    }
                    Text(
                        text = stringResource(R.string.midtrans_status_badge, statusLabel),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    state.lastVerifiedAt?.takeIf { it.isNotBlank() }?.let { timestamp ->
                        Text(
                            text = stringResource(R.string.midtrans_last_verified, timestamp),
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isSandbox) statusWarnBg() else statusOkBg())
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (state.isSandbox) {
                                R.string.midtrans_badge_sandbox
                            } else {
                                R.string.midtrans_badge_production
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (state.isSandbox) statusWarnFg() else statusOkFg(),
                            letterSpacing = 0.5.sp
                        )
                    )
                }
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.midtrans_merchant_id_label),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = state.merchantId,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateMerchantId(it)) },
                        placeholder = { Text(stringResource(R.string.midtrans_merchant_id_placeholder)) },
                        singleLine = true,
                        isError = state.merchantId.isNotEmpty() && !state.isMerchantIdValid,
                        supportingText = {
                            if (state.merchantId.isNotEmpty() && !state.isMerchantIdValid) {
                                Text(
                                    text = stringResource(R.string.midtrans_merchant_id_error),
                                    color = statusErrFg()
                                )
                            } else {
                                Text(stringResource(R.string.midtrans_merchant_id_help))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.midtrans_client_key_label),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    )
                    val clientPrefix = if (state.isSandbox) "SB-Mid-client-" else "Mid-client-"
                    OutlinedTextField(
                        value = state.clientKey,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateClientKey(it)) },
                        placeholder = { Text("${clientPrefix}xxx") },
                        singleLine = true,
                        isError = state.clientKey.isNotEmpty() && !state.isClientKeyValid,
                        supportingText = {
                            if (state.clientKey.isNotEmpty() && !state.isClientKeyValid) {
                                Text(
                                    text = stringResource(R.string.midtrans_client_key_error, clientPrefix),
                                    color = statusErrFg()
                                )
                            } else {
                                Text(stringResource(R.string.midtrans_client_key_help))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.midtrans_server_key_label),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    )
                    val serverPrefix = if (state.isSandbox) "SB-Mid-server-" else "Mid-server-"
                    OutlinedTextField(
                        value = state.serverKey,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateServerKey(it)) },
                        placeholder = {
                            Text(state.serverKeyMasked.ifEmpty { "${serverPrefix}xxx" })
                        },
                        visualTransformation = if (state.isServerKeyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = { onEvent(StoreSettingsUiEvent.ToggleServerKeyVisibility) },
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Text(
                                    stringResource(
                                        if (state.isServerKeyVisible) {
                                            R.string.midtrans_key_hide
                                        } else {
                                            R.string.midtrans_key_show
                                        }
                                    )
                                )
                            }
                        },
                        singleLine = true,
                        isError = state.serverKey.isNotEmpty() && !state.isServerKeyValid,
                        supportingText = {
                            if (state.serverKey.isNotEmpty() && !state.isServerKeyValid) {
                                Text(
                                    text = stringResource(R.string.midtrans_server_key_error, serverPrefix),
                                    color = statusErrFg()
                                )
                            } else {
                                Text(stringResource(R.string.midtrans_server_key_help))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = colors.outline, thickness = 1.dp)

                val fallbackSwitchLabel = stringResource(R.string.midtrans_fallback_qris_title)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fallbackSwitchLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                        )
                        Text(
                            text = stringResource(R.string.midtrans_fallback_qris_desc),
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = state.fallbackStaticQris,
                        onCheckedChange = { onEvent(StoreSettingsUiEvent.ToggleFallbackStaticQris(it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.primary),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics { contentDescription = fallbackSwitchLabel }
                    )
                }
            }
        }

        if (state.verificationMessage != null) {
            val isSuccess = state.verificationStatus == "success"
            val isTesting = state.verificationStatus == "testing"
            val boxBg = when {
                isSuccess -> statusOkBg()
                isTesting -> colors.surfaceVariant
                else -> statusErrBg()
            }
            val boxFg = when {
                isSuccess -> statusOkFg()
                isTesting -> colors.onSurface
                else -> statusErrFg()
            }
            Surface(
                color = boxBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.verificationMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = boxFg,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Surface(
            color = colors.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.midtrans_rolling_title),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                )
                Text(
                    text = stringResource(R.string.midtrans_rolling_body),
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
                )
            }
        }

        val formReady = state.isMerchantIdValid && state.isClientKeyValid && state.isServerKeyValid
        val busy = state.isSavingMidtrans || state.isVerifyingMidtrans

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { onEvent(StoreSettingsUiEvent.VerifyAndSaveMidtrans) },
                enabled = formReady && !busy,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.isVerifyingMidtrans) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.midtrans_btn_verify_live))
                }
            }

            Button(
                onClick = { onEvent(StoreSettingsUiEvent.SaveMidtransCredentials) },
                enabled = formReady && !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.isSavingMidtrans) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.midtrans_btn_save))
                }
            }
        }
    }
}
