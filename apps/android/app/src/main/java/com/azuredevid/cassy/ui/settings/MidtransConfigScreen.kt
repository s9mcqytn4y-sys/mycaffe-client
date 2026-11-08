package com.azuredevid.cassy.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.azuredevid.cassy.data.model.PaymentCredentialRequest
import com.azuredevid.cassy.data.model.PaymentCredentialResponse
import com.azuredevid.cassy.data.repository.KtorPaymentCredentialRepository
import com.azuredevid.cassy.domain.repository.PaymentCredentialRepository
import com.azuredevid.cassy.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MidtransConfigScreen(
    repository: PaymentCredentialRepository? = null,
    currentMerchantId: String = "",
    currentClientKey: String = "",
    currentServerKey: String = "",
    onSaveSuccess: (PaymentCredentialResponse) -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val credRepo = remember(repository) {
        repository ?: KtorPaymentCredentialRepository(context)
    }
    val coroutineScope = rememberCoroutineScope()

    var merchantId by rememberSaveable { mutableStateOf(currentMerchantId) }
    var clientKey by rememberSaveable { mutableStateOf(currentClientKey) }
    var serverKey by rememberSaveable { mutableStateOf(currentServerKey) }
    var isServerKeyVisible by rememberSaveable { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) } // (isSuccess, message)

    // Auto-detection Environment from Server Key / Client Key prefix
    val isSandbox = serverKey.startsWith("SB-") || clientKey.startsWith("SB-")

    // Validation States
    val isMerchantIdValid = merchantId.length >= 4
    val isClientKeyValid = clientKey.length >= 16 && (if (isSandbox) clientKey.startsWith("SB-Mid-client-") else clientKey.startsWith("Mid-client-"))
    val isServerKeyValid = serverKey.length >= 16 && (if (isSandbox) serverKey.startsWith("SB-Mid-server-") else serverKey.startsWith("Mid-server-"))

    var probeStatus by remember { mutableStateOf<String?>(null) } // null, "testing", "success", "error"
    var probeMessage by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Konfigurasi Payment Gateway Midtrans (BYOK)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Bawa Kunci API Anda Sendiri (Bring Your Own Key). Dana penjualan masuk langsung ke rekening bank Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Kembali ke POS")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Preset Shortcut Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
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
                        text = "Mode Uji Coba Cepat (Sandbox Preset)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Muat otomatis kredensial Midtrans Sandbox untuk keperluan demonstrasi atau pengujian kasir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        merchantId = "G123456789"
                        clientKey = "SB-Mid-client-demoSecretKey123"
                        serverKey = "SB-Mid-server-demoSecretServer456"
                        probeStatus = null
                        saveFeedback = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Gunakan Kredensial Sandbox")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Environment Indicator Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSandbox) StatusWarnBgLight else StatusOkBgLight
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isSandbox) "MODE SANDBOX (UJI COBA)" else "MODE PRODUCTION (LIVE MERCHANT)",
                    color = if (isSandbox) StatusWarnFgLight else StatusOkFgLight,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = if (isSandbox) "Kunci diawali dengan 'SB-'. Transaksi tidak menggunakan uang nyata." else "Kunci resmi aktif. Transaksi akan memotong saldo nyata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSandbox) StatusWarnFgLight else StatusOkFgLight
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Field 1: Merchant ID
        OutlinedTextField(
            value = merchantId,
            onValueChange = { merchantId = it.trim() },
            label = { Text("Merchant ID Midtrans") },
            placeholder = { Text("Contoh: G123456789") },
            supportingText = {
                if (merchantId.isNotEmpty() && !isMerchantIdValid) {
                    Text("Merchant ID minimal 4 karakter alfanumerik", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Dapat dilihat di pojok kanan atas Midtrans Merchant Portal (MAP)")
                }
            },
            isError = merchantId.isNotEmpty() && !isMerchantIdValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Field 2: Client Key
        OutlinedTextField(
            value = clientKey,
            onValueChange = { clientKey = it.trim() },
            label = { Text("Client Key") },
            placeholder = { Text("Contoh: SB-Mid-client-xxx atau Mid-client-xxx") },
            supportingText = {
                if (clientKey.isNotEmpty() && !isClientKeyValid) {
                    Text("Format salah: Wajib diawali '${if (isSandbox) "SB-Mid-client-" else "Mid-client-"}' dan min 16 char", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Digunakan untuk tokenisasi kartu dan checkout di sisi klien")
                }
            },
            isError = clientKey.isNotEmpty() && !isClientKeyValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Field 3: Server Key (Masked with Eye Sensor)
        OutlinedTextField(
            value = serverKey,
            onValueChange = { serverKey = it.trim() },
            label = { Text("Server Key (Kunci Rahasia)") },
            placeholder = { Text("Contoh: SB-Mid-server-xxx atau Mid-server-xxx") },
            visualTransformation = if (isServerKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(
                    onClick = { isServerKeyVisible = !isServerKeyVisible },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (isServerKeyVisible) "Sembunyikan" else "Tampilkan")
                }
            },
            supportingText = {
                if (serverKey.isNotEmpty() && !isServerKeyValid) {
                    Text("Format salah: Wajib diawali '${if (isSandbox) "SB-Mid-server-" else "Mid-server-"}' dan min 16 char", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Tersimpan aman terenkripsi di server PostgreSQL (Secretbox AAD). Tidak pernah dibocorkan.")
                }
            },
            isError = serverKey.isNotEmpty() && !isServerKeyValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Warning Box Key Rolling
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Peringatan Rolling Kunci API",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Jika Anda mengganti atau me-rolling kunci API di Dashboard Midtrans MAP, segera perbarui Server Key di halaman ini. Kunci yang tidak cocok akan menyebabkan verifikasi tanda tangan webhook gagal dan transaksi tertolak secara otomatis (Fail-Closed).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Feedback Banner (Success / Error)
        if (saveFeedback != null) {
            val (isOk, msg) = saveFeedback!!
            Surface(
                color = if (isOk) StatusOkBgLight else StatusErrBgLight,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isOk) "✓ Tersimpan:" else "✕ Gagal Menyimpan:",
                        fontWeight = FontWeight.Bold,
                        color = if (isOk) StatusOkFgLight else StatusErrFgLight
                    )
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOk) StatusOkFgLight else StatusErrFgLight
                    )
                }
            }
        }

        // Action Buttons Row (Touch Target Min 48dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (isServerKeyValid && isMerchantIdValid) {
                        probeStatus = "success"
                        probeMessage = "Kredensial valid. Respon probe Midtrans API: 200 OK (Akun Terverifikasi)."
                    } else {
                        probeStatus = "error"
                        probeMessage = "Kredensial belum lengkap atau format prefix kunci salah."
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Text("Uji Koneksi Sandbox (Live Probe)")
            }

            Button(
                onClick = {
                    if (isMerchantIdValid && isClientKeyValid && isServerKeyValid) {
                        isSaving = true
                        saveFeedback = null
                        coroutineScope.launch {
                            val request = PaymentCredentialRequest(
                                environment = if (isSandbox) "sandbox" else "production",
                                merchantId = merchantId,
                                clientKey = clientKey,
                                serverKey = serverKey,
                                isEnabled = true,
                                fallbackStaticQrisEnabled = true,
                                verifyLive = false
                            )

                            credRepo.saveCredentials(request)
                                .onSuccess { resp ->
                                    isSaving = false
                                    saveFeedback = Pair(true, "Kredensial tersimpan di PostgreSQL 18. Masked Key: ${resp.serverKeyMasked}")
                                    onSaveSuccess(resp)
                                }
                                .onFailure { err ->
                                    isSaving = false
                                    saveFeedback = Pair(false, err.message ?: "Koneksi ke backend gagal.")
                                }
                        }
                    }
                },
                enabled = isMerchantIdValid && isClientKeyValid && isServerKeyValid && !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menyimpan...")
                } else {
                    Text("Simpan & Aktifkan Kredensial")
                }
            }
        }
    }
}

