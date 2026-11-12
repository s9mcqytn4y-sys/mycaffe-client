package com.azuredevid.cassy.ui.settings.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.azuredevid.cassy.core.util.ImageCompressor
import com.azuredevid.cassy.ui.settings.StoreSettingsUiEvent
import com.azuredevid.cassy.ui.settings.StoreSettingsUiState
import com.azuredevid.cassy.ui.theme.LightAccent
import com.azuredevid.cassy.ui.theme.LightBorder
import com.azuredevid.cassy.ui.theme.LightInk
import com.azuredevid.cassy.ui.theme.LightMuted
import com.azuredevid.cassy.ui.theme.LightSecondaryInk
import com.azuredevid.cassy.ui.theme.LightSurface

private fun parseHexColor(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return fallback
    return try {
        val colorInt = clean.toLong(16)
        if (clean.length == 6) Color(colorInt or 0xFF000000) else Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}

// Neutral ink shown for an outlet without its own colour (PRD v4 Q9).
private val NeutralInk = Color(0xFF17211E)

private val COLOR_PRESETS = listOf(
    Pair("Netral", ""),
    Pair("Charcoal", "#1F2937"),
    Pair("Forest", "#1B4D3E"),
    Pair("Amber", "#B45309"),
    Pair("Navy", "#1E3A8A")
)

@Composable
fun StoreBrandCmsTab(
    state: StoreSettingsUiState,
    onEvent: (StoreSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (rawBytes != null && rawBytes.isNotEmpty()) {
                    val compressed = ImageCompressor.compressBytes(rawBytes)
                    onEvent(StoreSettingsUiEvent.UploadBanner(compressed))
                }
            } catch (_: Exception) {
                // Handled gracefully in ViewModel effect
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left Column: CMS Configuration Form (60% width)
        Column(
            modifier = Modifier
                .weight(0.6f)
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
                        text = "Identitas Merek dan Tampilan WPA Tamu",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightInk
                        )
                    )
                    Text(
                        text = "Konfigurasi judul, banner sambutan, palet warna, dan opsi label brand yang tampil pada browser tamu.",
                        style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                    )
                }
            }

            // Hero Titles & Subtitles Card
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
                        text = "Teks Sambutan Header Tamu",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightInk
                        )
                    )

                    OutlinedTextField(
                        value = state.heroTitleId,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateHeroTitleId(it)) },
                        label = { Text("Judul Utama (Bahasa Indonesia)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightAccent,
                            unfocusedBorderColor = LightBorder
                        )
                    )

                    OutlinedTextField(
                        value = state.heroTitleEn,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateHeroTitleEn(it)) },
                        label = { Text("Main Title (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightAccent,
                            unfocusedBorderColor = LightBorder
                        )
                    )

                    OutlinedTextField(
                        value = state.heroSubtitleId,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateHeroSubtitleId(it)) },
                        label = { Text("Subjudul (Bahasa Indonesia)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightAccent,
                            unfocusedBorderColor = LightBorder
                        )
                    )

                    OutlinedTextField(
                        value = state.heroSubtitleEn,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateHeroSubtitleEn(it)) },
                        label = { Text("Subtitle (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightAccent,
                            unfocusedBorderColor = LightBorder
                        )
                    )
                }
            }

            // Colors & Palette Card
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
                        text = "Warna Aksen Merek (Design Tokens)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightInk
                        )
                    )

                    // Color Presets
                    Text(
                        text = "Pilihan Preset Cepat:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LightSecondaryInk,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        COLOR_PRESETS.forEach { (name, hex) ->
                            val presetColor = parseHexColor(hex, NeutralInk)
                            val isSelected = state.primaryHex.equals(hex, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LightMuted else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) LightAccent else LightBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onEvent(StoreSettingsUiEvent.UpdatePrimaryHex(hex))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(presetColor)
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall.copy(color = LightInk)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.primaryHex,
                            onValueChange = { onEvent(StoreSettingsUiEvent.UpdatePrimaryHex(it)) },
                            label = { Text("Warna Primer (HEX)") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(state.primaryHex, NeutralInk))
                                        .border(1.dp, LightBorder, CircleShape)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LightAccent,
                                unfocusedBorderColor = LightBorder
                            )
                        )

                        OutlinedTextField(
                            value = state.accentHex,
                            onValueChange = { onEvent(StoreSettingsUiEvent.UpdateAccentHex(it)) },
                            label = { Text("Warna Aksen (HEX)") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(state.accentHex, LightAccent))
                                        .border(1.dp, LightBorder, CircleShape)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LightAccent,
                                unfocusedBorderColor = LightBorder
                            )
                        )
                    }
                }
            }

            // Banner Image & White Label Card
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
                        text = "Banner dan Opsi White Label",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightInk
                        )
                    )

                    // Banner Upload Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.heroBannerUrl.isNotBlank()) {
                            AsyncImage(
                                model = state.heroBannerUrl,
                                contentDescription = "Pratinjau Banner Hero",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 120.dp, height = 64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, LightBorder, RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(width = 120.dp, height = 64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LightMuted)
                                    .border(1.dp, LightBorder, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tanpa Foto",
                                    style = MaterialTheme.typography.labelSmall.copy(color = LightSecondaryInk)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { bannerPickerLauncher.launch("image/*") },
                                enabled = !state.isUploadingBanner,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LightInk)
                            ) {
                                if (state.isUploadingBanner) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = LightInk
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mengunggah WebP...")
                                } else {
                                    Text("Pilih Banner Gerai")
                                }
                            }
                            Text(
                                text = "Format didukung: JPG, PNG, WebP (kompresi otomatis maks 300 KB)",
                                style = MaterialTheme.typography.labelSmall.copy(color = LightSecondaryInk)
                            )
                        }
                    }

                    HorizontalDivider(color = LightBorder)

                    // White-Label Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sembunyikan Label 'Didukung oleh Cassy'",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = LightInk
                                )
                            )
                            Text(
                                text = "Aktifkan mode White-Label eksklusif pada halaman pemesanan tamu.",
                                style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                            )
                        }
                        Switch(
                            checked = state.hidePoweredByBadge,
                            onCheckedChange = { onEvent(StoreSettingsUiEvent.ToggleWhiteLabel(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LightAccent
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Save CMS Button
            Button(
                onClick = { onEvent(StoreSettingsUiEvent.SaveStoreCms) },
                enabled = !state.isSavingCms,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightInk,
                    contentColor = Color.White
                )
            ) {
                if (state.isSavingCms) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menyimpan Tampilan WPA...")
                } else {
                    Text("Simpan Pengaturan Tampilan WPA")
                }
            }
        }

        // Right Column: Live WPA Mobile Preview Frame (40% width)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Pratinjau Langsung Tamu (WPA Mobile)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = LightInk
                )
            )

            // Simulated Smartphone Frame
            val primaryColor = remember(state.primaryHex) {
                parseHexColor(state.primaryHex, LightInk)
            }
            val accentColor = remember(state.accentHex) {
                parseHexColor(state.accentHex, LightAccent)
            }

            Surface(
                modifier = Modifier
                    .width(310.dp)
                    .height(560.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(4.dp, LightInk),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Mobile Status Bar Mock
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(primaryColor)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "10:30",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "4G",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = "100%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    // Mobile App Header Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(primaryColor)
                    ) {
                        if (state.heroBannerUrl.isNotBlank()) {
                            AsyncImage(
                                model = state.heroBannerUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            // Subtle gradient overlay for readability
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = state.heroTitleId.ifBlank { "Pesan & Bayar dari Meja" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.heroSubtitleId.ifBlank { "Pindai QR, pilih menu, lalu bayar dari meja" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = accentColor,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Category Bar Mock
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightMuted)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = primaryColor
                        ) {
                            Text(
                                text = "Kopi Spesialti",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
                        ) {
                            Text(
                                text = "Pastry & Roti",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = LightSecondaryInk,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Sample Product Mock Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LightMuted),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "MENU",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = LightSecondaryInk
                                        )
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Espresso Single Origin",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = LightInk
                                        )
                                    )
                                    Text(
                                        text = "Rp 28.000",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = accentColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Cart Floating Mock
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1 Sajian • Rp 28.000",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = "Lihat Keranjang",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    // White-Label / Powered By Footer Mock
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!state.hidePoweredByBadge) {
                            Text(
                                text = "Didukung oleh Cassy",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = LightSecondaryInk.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                            )
                        } else {
                            Text(
                                text = "Pemesanan Mandiri Meja",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = LightSecondaryInk.copy(alpha = 0.5f),
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

