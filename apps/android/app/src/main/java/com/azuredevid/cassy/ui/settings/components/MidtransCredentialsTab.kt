package com.azuredevid.cassy.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.ui.settings.StoreSettingsUiEvent
import com.azuredevid.cassy.ui.settings.StoreSettingsUiState
import com.azuredevid.cassy.ui.theme.LightAccent
import com.azuredevid.cassy.ui.theme.LightBorder
import com.azuredevid.cassy.ui.theme.LightInk
import com.azuredevid.cassy.ui.theme.LightMuted
import com.azuredevid.cassy.ui.theme.LightSecondaryInk
import com.azuredevid.cassy.ui.theme.LightSurface
import com.azuredevid.cassy.ui.theme.StatusErrBgLight
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import com.azuredevid.cassy.ui.theme.StatusOkBgLight
import com.azuredevid.cassy.ui.theme.StatusOkFgLight
import com.azuredevid.cassy.ui.theme.StatusWarnBgLight
import com.azuredevid.cassy.ui.theme.StatusWarnFgLight

@Composable
fun MidtransCredentialsTab(
    state: StoreSettingsUiState,
    onEvent: (StoreSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Card: Environment & Status Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Lingkungan Integrasi Midtrans",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightInk
                        )
                    )
                    Text(
                        text = if (state.isSandbox) {
                            "Mode Sandbox (Uji Coba Transaksi Simulasi)"
                        } else {
                            "Mode Produksi Live (Rekening Kafe Nyata)"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isSandbox) StatusWarnBgLight else StatusOkBgLight)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (state.isSandbox) "SANDBOX" else "PRODUCTION LIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (state.isSandbox) StatusWarnFgLight else StatusOkFgLight,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }

        // Form Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Merchant ID
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Merchant ID *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = LightInk)
                    )
                    OutlinedTextField(
                        value = state.merchantId,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateMerchantId(it)) },
                        placeholder = { Text("Contoh: G357841497") },
                        singleLine = true,
                        isError = state.merchantId.isNotEmpty() && !state.isMerchantIdValid,
                        supportingText = {
                            if (state.merchantId.isNotEmpty() && !state.isMerchantIdValid) {
                                Text("Merchant ID minimal 4 karakter.", color = StatusErrFgLight)
                            } else {
                                Text("Merchant ID resmi dari Dashboard Midtrans MAP")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightAccent,
                            unfocusedBorderColor = LightBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Client Key
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Client Key *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = LightInk)
                    )
                    OutlinedTextField(
                        value = state.clientKey,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateClientKey(it)) },
                        placeholder = { Text(if (state.isSandbox) "SB-Mid-client-xxx" else "Mid-client-xxx") },
                        singleLine = true,
                        isError = state.clientKey.isNotEmpty() && !state.isClientKeyValid,
                        supportingText = {
                            if (state.clientKey.isNotEmpty() && !state.isClientKeyValid) {
                                Text(
                                    "Format salah: Wajib diawali '${if (state.isSandbox) "SB-Mid-client-" else "Mid-client-"}' dan minimal 16 karakter.",
                                    color = StatusErrFgLight
                                )
                            } else {
                                Text("Kunci publik untuk checkout pembayaran mandiri tamu")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightAccent,
                            unfocusedBorderColor = LightBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 3. Server Key
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Server Key (Kunci Rahasia) *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = LightInk)
                    )
                    OutlinedTextField(
                        value = state.serverKey,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateServerKey(it)) },
                        placeholder = {
                            Text(
                                if (state.serverKeyMasked.isNotEmpty()) {
                                    state.serverKeyMasked
                                } else {
                                    if (state.isSandbox) "SB-Mid-server-xxx" else "Mid-server-xxx"
                                }
                            )
                        },
                        visualTransformation = if (state.isServerKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(
                                onClick = { onEvent(StoreSettingsUiEvent.ToggleServerKeyVisibility) },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(if (state.isServerKeyVisible) "Sembunyikan" else "Intip")
                            }
                        },
                        singleLine = true,
                        isError = state.serverKey.isNotEmpty() && !state.isServerKeyValid,
                        supportingText = {
                            if (state.serverKey.isNotEmpty() && !state.isServerKeyValid) {
                                Text(
                                    "Format salah: Wajib diawali '${if (state.isSandbox) "SB-Mid-server-" else "Mid-server-"}' dan minimal 16 karakter.",
                                    color = StatusErrFgLight
                                )
                            } else {
                                Text("Tersimpan aman terenkripsi di PostgreSQL (Secretbox AAD). Tidak dibocorkan ke log.")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightAccent,
                            unfocusedBorderColor = LightBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = LightBorder, thickness = 1.dp)

                // 4. Fallback Static QRIS Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fallback QRIS Statis & Pembayaran Kasir",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = LightInk
                            )
                        )
                        Text(
                            text = "Izinkan pelanggan memilih bayar di kasir jika koneksi gateway bermasalah",
                            style = MaterialTheme.typography.labelSmall.copy(color = LightSecondaryInk)
                        )
                    }
                    Switch(
                        checked = state.fallbackStaticQris,
                        onCheckedChange = { onEvent(StoreSettingsUiEvent.ToggleFallbackStaticQris(it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = LightAccent)
                    )
                }
            }
        }

        // Live Probe Status Feedback Box
        if (state.probeMessage != null) {
            val isSuccess = state.probeStatus == "success"
            Surface(
                color = if (isSuccess) StatusOkBgLight else StatusErrBgLight,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isSuccess) "Probe Sukses:" else "Probe Gagal:",
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) StatusOkFgLight else StatusErrFgLight
                    )
                    Text(
                        text = state.probeMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuccess) StatusOkFgLight else StatusErrFgLight
                    )
                }
            }
        }

        // Security Advice & Rolling Keys
        Surface(
            color = LightMuted.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Perhatian Rolling Kunci API",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = LightInk)
                )
                Text(
                    text = "Jika Anda mengganti kunci di Dashboard Midtrans MAP, segera perbarui Server Key di halaman ini. Kunci yang salah akan menyebabkan verifikasi tanda tangan webhook SHA-512 gagal dan transaksi tertolak otomatis.",
                    style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { onEvent(StoreSettingsUiEvent.RunLiveProbe) },
                enabled = !state.isSavingMidtrans && state.probeStatus != "testing",
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.probeStatus == "testing") {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Uji Live Probe API")
                }
            }

            Button(
                onClick = { onEvent(StoreSettingsUiEvent.SaveMidtransCredentials) },
                enabled = state.isMerchantIdValid && (state.serverKey.isNotBlank() || state.serverKeyMasked.isNotBlank()) && !state.isSavingMidtrans,
                colors = ButtonDefaults.buttonColors(containerColor = LightAccent),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.isSavingMidtrans) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Simpan Kredensial")
                }
            }
        }
    }
}

