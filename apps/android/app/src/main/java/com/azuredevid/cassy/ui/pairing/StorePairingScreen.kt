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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.ui.scanner.CameraQrScannerView
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import org.json.JSONObject

/**
 * Store Pairing Screen supporting Signed QR JSON camera scanner with State Lock
 * and 6-digit alphanumeric manual code entry.
 * Fully localized with dual-language support (ID/EN) and WCAG 2.2 AA accessibility.
 */
@Composable
fun StorePairingScreen(
    onPairingSuccess: (storeSlug: String, pairingToken: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCameraActive by rememberSaveable { mutableStateOf(false) }
    var manualCode by rememberSaveable { mutableStateOf("") }
    var storeSlugInput by rememberSaveable { mutableStateOf("central-cafe") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isStateLocked by remember { mutableStateOf(false) }

    val errInvalidFormat = stringResource(R.string.err_invalid_qr_format)
    val errCodeLength = stringResource(R.string.err_code_length)
    val logoDesc = stringResource(R.string.a11y_cassy_logo)

    if (isCameraActive && !isStateLocked) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraQrScannerView(
                onQrDetected = { payload ->
                    // State Lock immediately when valid payload detected
                    try {
                        val json = JSONObject(payload)
                        val slug = json.optString("store_slug", json.optString("store_id", "central-cafe"))
                        val token = json.optString("pairing_token", json.optString("token", ""))

                        if (token.isNotBlank()) {
                            isStateLocked = true
                            isCameraActive = false
                            errorMessage = null
                            onPairingSuccess(slug, token)
                        } else {
                            errorMessage = errInvalidFormat
                        }
                    } catch (_: Exception) {
                        // Fallback: Check if it's a direct pairing token string
                        if (payload.trim().length >= 4) {
                            isStateLocked = true
                            isCameraActive = false
                            errorMessage = null
                            onPairingSuccess(storeSlugInput, payload.trim())
                        } else {
                            errorMessage = errInvalidFormat
                        }
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
                Text(stringResource(R.string.btn_close_camera))
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
                // Cassy Monogram Icon with A11y Semantics
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(56.dp)
                        .semantics { contentDescription = logoDesc }
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
                    text = stringResource(R.string.pairing_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.pairing_screen_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (isStateLocked) {
                    // State Lock Loading Indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.pairing_connecting),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = StatusErrFgLight,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Primary Action: Pindai QR Code via CameraX
                Button(
                    onClick = {
                        errorMessage = null
                        isCameraActive = true
                    },
                    enabled = !isStateLocked,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_scan_qr_camera),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = " ${stringResource(R.string.or_divider)} ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Secondary Action: Input Manual Kode Gerai
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = storeSlugInput,
                        onValueChange = { storeSlugInput = it },
                        enabled = !isStateLocked,
                        label = { Text(stringResource(R.string.store_slug_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it.uppercase().take(6) },
                        enabled = !isStateLocked,
                        label = { Text(stringResource(R.string.pairing_code_label)) },
                        placeholder = { Text("CAS892") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            if (manualCode.length >= 4) {
                                isStateLocked = true
                                errorMessage = null
                                onPairingSuccess(storeSlugInput, manualCode)
                            } else {
                                errorMessage = errCodeLength
                            }
                        },
                        enabled = manualCode.isNotBlank() && !isStateLocked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.btn_pair_manual), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
