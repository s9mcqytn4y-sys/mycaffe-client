package com.azuredevid.cassy.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.ui.scanner.CameraQrScannerView
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import com.azuredevid.cassy.ui.theme.StatusOkFgLight

@Composable
fun StorePairingScreen(
    onPairingSuccess: (storeSlug: String, pairingToken: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCameraActive by rememberSaveable { mutableStateOf(false) }
    var manualCode by rememberSaveable { mutableStateOf("") }
    var storeSlugInput by rememberSaveable { mutableStateOf("central-cafe") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    if (isCameraActive) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraQrScannerView(
                onQrDetected = { payload ->
                    isCameraActive = false
                    // Expect format: "cassy://pair?slug=central-cafe&token=..." or plain JSON/token
                    if (payload.contains("slug=") || payload.length >= 6) {
                        onPairingSuccess(storeSlugInput, payload)
                    } else {
                        errorMessage = "Format QR Code tidak valid untuk pairing Cassy POS."
                    }
                }
            )

            // Close Camera Button
            Button(
                onClick = { isCameraActive = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Tutup Kamera")
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cassy Monogram Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "C",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                    }
                }

                Text(
                    text = "Pairing Perangkat Tablet POS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Hubungkan tablet kasir ini ke gerai kafe Anda melalui pemindaian QR code atau masukkan kode pairing 6-digit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Primary Button: Pindai QR
                Button(
                    onClick = {
                        errorMessage = null
                        isCameraActive = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    Text("Pindai QR Pairing Gerai", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "ATAU MASUKKAN MANUAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                OutlinedTextField(
                    value = storeSlugInput,
                    onValueChange = { storeSlugInput = it },
                    label = { Text("Slug Gerai (Store Slug)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = manualCode,
                    onValueChange = {
                        if (it.length <= 6) {
                            manualCode = it.filter { char -> char.isLetterOrDigit() }.uppercase()
                        }
                    },
                    label = { Text("Kode Pairing (6-Karakter)") },
                    placeholder = { Text("Contoh: CP789X") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        if (manualCode.length == 6) {
                            onPairingSuccess(storeSlugInput, manualCode)
                        } else {
                            errorMessage = "Kode pairing harus terdiri dari 6 karakter alfanumerik."
                        }
                    },
                    enabled = manualCode.length == 6 && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Hubungkan dengan Kode", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

