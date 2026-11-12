package com.azuredevid.cassy.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.domain.model.QRServiceMode
import com.azuredevid.cassy.ui.settings.StoreSettingsUiEvent
import com.azuredevid.cassy.ui.settings.StoreSettingsUiState
import com.azuredevid.cassy.ui.theme.LightAccent
import com.azuredevid.cassy.ui.theme.LightBorder
import com.azuredevid.cassy.ui.theme.LightInk
import com.azuredevid.cassy.ui.theme.LightMuted
import com.azuredevid.cassy.ui.theme.LightSecondaryInk
import com.azuredevid.cassy.ui.theme.LightSurface
import com.azuredevid.cassy.ui.theme.StatusWarnBgLight
import com.azuredevid.cassy.ui.theme.StatusWarnFgLight

@Composable
fun StoreServiceModeTab(
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
        // Section Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Mode Layanan dan Kebijakan Operasional Gerai (US4)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightInk
                    )
                )
                Text(
                    text = "Konfigurasi alur penyajian pesanan QR tamu, pembatasan durasi penggunaan meja, serta penghentian darurat pemesanan.",
                    style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                )
            }
        }

        // Service Mode (US4): Bar Pickup vs Table Delivery
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Metode Penyajian Pesanan Meja (QR Service Mode)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightInk
                    )
                )
                Text(
                    text = "Pilih bagaimana staf memproses dan menyerahkan pesanan yang masuk melalui QR kode tamu.",
                    style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Antar ke Meja (TABLE)
                    val isTableSelected = state.qrServiceMode == QRServiceMode.TABLE
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onEvent(StoreSettingsUiEvent.SetServiceMode(QRServiceMode.TABLE))
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isTableSelected) LightMuted else LightSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isTableSelected) 2.dp else 1.dp,
                            if (isTableSelected) LightAccent else LightBorder
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
                                    text = "Antar ke Meja",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = LightInk
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isTableSelected) LightAccent else Color.Transparent)
                                        .border(2.dp, if (isTableSelected) LightAccent else LightBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isTableSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Pramusaji mengantarkan sajian langsung ke nomor meja tamu saat pesanan berstatus siap.",
                                style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                            )
                        }
                    }

                    // Option 2: Ambil Mandiri di Bar (PICKUP)
                    val isPickupSelected = state.qrServiceMode == QRServiceMode.PICKUP
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onEvent(StoreSettingsUiEvent.SetServiceMode(QRServiceMode.PICKUP))
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPickupSelected) LightMuted else LightSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isPickupSelected) 2.dp else 1.dp,
                            if (isPickupSelected) LightAccent else LightBorder
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
                                    text = "Ambil Mandiri di Bar",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = LightInk
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isPickupSelected) LightAccent else Color.Transparent)
                                        .border(2.dp, if (isPickupSelected) LightAccent else LightBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPickupSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Tamu mengambil mandiri pesanan di counter barista saat layar status atau notifikasi berbunyi siap.",
                                style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                            )
                        }
                    }
                }
            }
        }

        // Emergency QR Pause Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (state.qrOrderingPaused) StatusWarnBgLight else LightSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (state.qrOrderingPaused) StatusWarnFgLight else LightBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jeda Pemesanan Mandiri QR Darurat",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (state.qrOrderingPaused) StatusWarnFgLight else LightInk
                            )
                        )
                        Text(
                            text = if (state.qrOrderingPaused) {
                                "Pemesanan QR sedang dihentikan. Tamu diminta memesan langsung ke meja kasir."
                            } else {
                                "Hentikan sementara pemesanan dari seluruh kode QR meja ketika antrean dapur sedang padat."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (state.qrOrderingPaused) StatusWarnFgLight else LightSecondaryInk
                            )
                        )
                    }

                    Switch(
                        checked = state.qrOrderingPaused,
                        onCheckedChange = { onEvent(StoreSettingsUiEvent.ToggleEmergencyQrPause(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StatusWarnFgLight
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Table Duration Limit & Warning Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Batas Waktu Penggunaan Meja & Peringatan Otomatis",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightInk
                    )
                )

                // Limit Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Batas Durasi Meja",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = LightInk
                            )
                        )
                        Text(
                            text = if (state.tableTimeLimitMinutes == 0) {
                                "Tanpa batas waktu (bebas durasi)"
                            } else {
                                "${state.tableTimeLimitMinutes} menit per sesi meja"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
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
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("-15", style = MaterialTheme.typography.labelSmall)
                        }

                        Text(
                            text = "${state.tableTimeLimitMinutes} mnt",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LightInk
                            ),
                            modifier = Modifier.width(60.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        OutlinedButton(
                            onClick = {
                                val newLimit = (state.tableTimeLimitMinutes + 15).coerceAtMost(240)
                                onEvent(StoreSettingsUiEvent.UpdateTableTimeLimit(newLimit))
                            },
                            enabled = state.tableTimeLimitMinutes < 240,
                            modifier = Modifier.size(width = 48.dp, height = 48.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("+15", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                HorizontalDivider(color = LightBorder)

                // Warning indicator info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Peringatan Menjelang Batas Waktu",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = LightInk
                            )
                        )
                        Text(
                            text = "Kartu meja di denah kasir akan menyala kuning ${state.tableTimerWarningMinutes} menit sebelum waktu habis.",
                            style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                        )
                    }
                    Text(
                        text = "${state.tableTimerWarningMinutes} menit",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightInk
                        )
                    )
                }
            }
        }

        // Save Settings Button
        Button(
            onClick = { onEvent(StoreSettingsUiEvent.SaveStoreSettings) },
            enabled = !state.isSavingSettings,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LightInk,
                contentColor = Color.White
            )
        ) {
            if (state.isSavingSettings) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Menyimpan Kebijakan Operasional...")
            } else {
                Text("Simpan Kebijakan Operasional Gerai")
            }
        }
    }
}

