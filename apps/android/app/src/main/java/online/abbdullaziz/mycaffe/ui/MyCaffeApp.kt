package online.abbdullaziz.mycaffe.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import online.abbdullaziz.mycaffe.hardware.EscPosTcpPrinter
import online.abbdullaziz.mycaffe.hardware.LanPrinterTarget
import online.abbdullaziz.mycaffe.hardware.PaperWidth
import online.abbdullaziz.mycaffe.network.HealthStatusResult
import online.abbdullaziz.mycaffe.network.MidtransCredentialSnapshot
import online.abbdullaziz.mycaffe.network.MikrotikVlanSnapshot
import online.abbdullaziz.mycaffe.network.MyCaffeApiClient
import online.abbdullaziz.mycaffe.network.PosTableItem
import online.abbdullaziz.mycaffe.network.StaffSessionState
import java.io.File

private enum class CompanionTab(val tag: String, val label: String, val badge: String) {
    KASIR_QR("tab_kasir_qr", "Kasir & QR", "POS"),
    MIDTRANS_WALLET("tab_midtrans_wallet", "Midtrans & Wallet", "PAY"),
    JARINGAN_PRINTER("tab_network_printer", "Jaringan & Printer", "LAN"),
    IZIN_STORAGE("tab_permissions_storage", "Izin & Storage", "SYS")
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MyCaffeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient = remember { MyCaffeApiClient() }
    val escPosPrinter = remember { EscPosTcpPrinter() }

    var darkTheme by rememberSaveable { mutableStateOf(false) }
    var activeTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeTab = CompanionTab.entries[activeTabIndex]

    var apiBaseUrl by rememberSaveable { mutableStateOf("http://10.0.2.2:8080") }
    var healthLoading by remember { mutableStateOf(false) }
    var healthResult by remember { mutableStateOf<HealthStatusResult?>(null) }
    var healthError by remember { mutableStateOf<String?>(null) }

    var storeSlugInput by rememberSaveable { mutableStateOf("kopi-nusantara") }
    var staffEmailInput by rememberSaveable { mutableStateOf("owner@kopinusantara.id") }
    var staffPinInput by rememberSaveable { mutableStateOf("") }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var staffSession by remember { mutableStateOf<StaffSessionState?>(null) }

    var tablesLoading by remember { mutableStateOf(false) }
    var tablesError by remember { mutableStateOf<String?>(null) }
    var tablesList by remember { mutableStateOf<List<PosTableItem>>(emptyList()) }

    var midtransLoading by remember { mutableStateOf(false) }
    var midtransError by remember { mutableStateOf<String?>(null) }
    var midtransSnapshot by remember { mutableStateOf<MidtransCredentialSnapshot?>(null) }

    var mikrotikSnapshot by remember { mutableStateOf<MikrotikVlanSnapshot?>(null) }
    var printerIp by rememberSaveable { mutableStateOf("192.168.10.50") }
    var printerPortText by rememberSaveable { mutableStateOf("9100") }
    var paperWidthMm by rememberSaveable { mutableIntStateOf(80) }
    var printerBusy by remember { mutableStateOf(false) }
    var printerOnline by remember { mutableStateOf(false) }
    var printerFeedback by remember { mutableStateOf("Siap menguji koneksi soket TCP:9100 pada jaringan LAN gerai.") }

    fun probeBackendHealth() {
        scope.launch {
            healthLoading = true
            healthError = null
            apiClient.checkHealthReady(apiBaseUrl)
                .onSuccess { res -> healthResult = res }
                .onFailure { err ->
                    healthResult = null
                    healthError = "Backend belum terjangkau di $apiBaseUrl (${err.message ?: "timeout"})."
                }
            healthLoading = false
        }
    }

    fun loadAuthenticatedStoreData(session: StaffSessionState) {
        scope.launch {
            tablesLoading = true
            tablesError = null
            apiClient.fetchTables(apiBaseUrl, session.accessToken)
                .onSuccess { tablesList = it }
                .onFailure { tablesError = it.message ?: "Gagal memuat meja QR" }
            tablesLoading = false

            midtransLoading = true
            midtransError = null
            apiClient.fetchMidtransSnapshot(apiBaseUrl, session.accessToken)
                .onSuccess { midtransSnapshot = it }
                .onFailure { midtransError = it.message ?: "Gagal memuat status Midtrans" }
            midtransLoading = false

            apiClient.fetchMikrotikSnapshot(apiBaseUrl, session.accessToken)
                .onSuccess { mikrotikSnapshot = it }
        }
    }

    LaunchedEffect(Unit) {
        probeBackendHealth()
    }

    MyCaffeTheme(darkTheme = darkTheme) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
        ) {
            val isTabletWidth = maxWidth >= 640.dp
            val headerState = PosOrderSummaryUiState(
                storeName = staffSession?.storeName ?: "MyCaffe by AzureDevID",
                queueCode = if (staffSession != null) "#A-001" else "Mode Siaga",
                totalIdr = 57_200L,
                printerStatusLabel = if (printerOnline) "Printer LAN Aktif" else "Cek Printer 9100",
                isPrinterOnline = printerOnline
            )

            Scaffold(
                topBar = {
                    PosCashierHeaderBar(
                        state = headerState,
                        darkTheme = darkTheme,
                        onToggleTheme = { darkTheme = !darkTheme },
                        onQuickPrintNav = { activeTabIndex = CompanionTab.JARINGAN_PRINTER.ordinal }
                    )
                },
                bottomBar = {
                    if (!isTabletWidth) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            CompanionTab.entries.forEachIndexed { idx, tab ->
                                NavigationBarItem(
                                    selected = activeTabIndex == idx,
                                    onClick = { activeTabIndex = idx },
                                    modifier = Modifier
                                        .testTag(tab.tag)
                                        .semantics { contentDescription = tab.label },
                                    icon = {
                                        Text(
                                            text = tab.badge,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    label = { Text(text = tab.label) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isTabletWidth) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            CompanionTab.entries.forEachIndexed { idx, tab ->
                                NavigationRailItem(
                                    selected = activeTabIndex == idx,
                                    onClick = { activeTabIndex = idx },
                                    modifier = Modifier
                                        .padding(vertical = 6.dp)
                                        .testTag(tab.tag)
                                        .semantics { contentDescription = tab.label },
                                    icon = {
                                        Text(
                                            text = tab.badge,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    label = { Text(text = tab.label) }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (activeTab) {
                            CompanionTab.KASIR_QR -> KasirAndQrTab(
                                darkTheme = darkTheme,
                                apiBaseUrl = apiBaseUrl,
                                onApiBaseUrlChange = { apiBaseUrl = it },
                                healthLoading = healthLoading,
                                healthResult = healthResult,
                                healthError = healthError,
                                onProbeHealth = ::probeBackendHealth,
                                storeSlug = storeSlugInput,
                                onStoreSlugChange = { storeSlugInput = it },
                                email = staffEmailInput,
                                onEmailChange = { staffEmailInput = it },
                                pin = staffPinInput,
                                onPinChange = { if (it.length <= 8) staffPinInput = it.filter(Char::isDigit) },
                                authLoading = authLoading,
                                authError = authError,
                                session = staffSession,
                                onLogin = {
                                    if (staffPinInput.length < 6) {
                                        authError = "PIN staf wajib minimal 6 digit angka."
                                    } else {
                                        scope.launch {
                                            authLoading = true
                                            authError = null
                                            apiClient.loginStaff(apiBaseUrl, storeSlugInput, staffEmailInput, staffPinInput)
                                                .onSuccess { loggedIn ->
                                                    staffSession = loggedIn
                                                    staffPinInput = ""
                                                    loadAuthenticatedStoreData(loggedIn)
                                                }
                                                .onFailure { err ->
                                                    authError = err.message ?: "Autentikasi staf gagal"
                                                }
                                            authLoading = false
                                        }
                                    }
                                },
                                onLogout = {
                                    staffSession = null
                                    tablesList = emptyList()
                                    midtransSnapshot = null
                                },
                                tablesLoading = tablesLoading,
                                tablesError = tablesError,
                                tables = tablesList,
                                onRefreshTables = { staffSession?.let(::loadAuthenticatedStoreData) }
                            )

                            CompanionTab.MIDTRANS_WALLET -> MidtransAndWalletTab(
                                darkTheme = darkTheme,
                                session = staffSession,
                                loading = midtransLoading,
                                error = midtransError,
                                snapshot = midtransSnapshot,
                                onRefresh = { staffSession?.let(::loadAuthenticatedStoreData) }
                            )

                            CompanionTab.JARINGAN_PRINTER -> NetworkAndPrinterTab(
                                darkTheme = darkTheme,
                                mikrotik = mikrotikSnapshot,
                                printerIp = printerIp,
                                onPrinterIpChange = { printerIp = it },
                                printerPortText = printerPortText,
                                onPrinterPortChange = { printerPortText = it.filter(Char::isDigit) },
                                paperWidthMm = paperWidthMm,
                                onSelectPaperWidth = { paperWidthMm = it },
                                busy = printerBusy,
                                feedback = printerFeedback,
                                isOnline = printerOnline,
                                onProbeSocket = {
                                    val port = printerPortText.toIntOrNull() ?: 9100
                                    val target = LanPrinterTarget(
                                        host = printerIp,
                                        port = port,
                                        paperWidth = if (paperWidthMm == 58) PaperWidth.MM_58 else PaperWidth.MM_80
                                    )
                                    scope.launch {
                                        printerBusy = true
                                        escPosPrinter.probeTcpSocket(target)
                                            .onSuccess { ms ->
                                                printerOnline = true
                                                printerFeedback = "Soket TCP ${target.host}:${target.port} terbuka dalam ${ms}ms."
                                            }
                                            .onFailure { err ->
                                                printerOnline = false
                                                printerFeedback = "Soket TCP ${target.host}:${target.port} tidak merespons (${err.message ?: "timeout"})."
                                            }
                                        printerBusy = false
                                    }
                                },
                                onTestPrint = {
                                    val port = printerPortText.toIntOrNull() ?: 9100
                                    val width = if (paperWidthMm == 58) PaperWidth.MM_58 else PaperWidth.MM_80
                                    val target = LanPrinterTarget(printerIp, port, width)
                                    val payload = escPosPrinter.buildDiagnosticReceiptPayload(
                                        storeName = staffSession?.storeName ?: "Gerai Simulasi MyCaffe",
                                        paperWidth = width
                                    )
                                    scope.launch {
                                        printerBusy = true
                                        escPosPrinter.printRawLanTicket(target, payload)
                                            .onSuccess { ms ->
                                                printerOnline = true
                                                printerFeedback = "Struk uji ESC/POS (${width.mm}mm) terkirim ke ${target.host}:${target.port} dalam ${ms}ms."
                                            }
                                            .onFailure { err ->
                                                printerOnline = false
                                                printerFeedback = "Gagal mengirim struk ESC/POS ke ${target.host}:${target.port}: ${err.message ?: "timeout"}."
                                            }
                                        printerBusy = false
                                    }
                                },
                                onKickDrawer = {
                                    val port = printerPortText.toIntOrNull() ?: 9100
                                    val target = LanPrinterTarget(printerIp, port)
                                    scope.launch {
                                        printerBusy = true
                                        escPosPrinter.kickCashDrawerLan(target)
                                            .onSuccess { ms ->
                                                printerOnline = true
                                                printerFeedback = "Pulsa laci kasir RJ11 (Pin 2) terkirim dalam ${ms}ms."
                                            }
                                            .onFailure { err ->
                                                printerOnline = false
                                                printerFeedback = "Gagal membuka laci kasir via ${target.host}:${target.port}: ${err.message ?: "timeout"}."
                                            }
                                        printerBusy = false
                                    }
                                }
                            )

                            CompanionTab.IZIN_STORAGE -> PermissionsAndStorageTab(
                                context = context,
                                darkTheme = darkTheme,
                                session = staffSession,
                                kybStatus = midtransSnapshot?.kybStatus ?: "unverified"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            content()
        }
    }
}

@Composable
private fun KasirAndQrTab(
    darkTheme: Boolean,
    apiBaseUrl: String,
    onApiBaseUrlChange: (String) -> Unit,
    healthLoading: Boolean,
    healthResult: HealthStatusResult?,
    healthError: String?,
    onProbeHealth: () -> Unit,
    storeSlug: String,
    onStoreSlugChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    authLoading: Boolean,
    authError: String?,
    session: StaffSessionState?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    tablesLoading: Boolean,
    tablesError: String?,
    tables: List<PosTableItem>,
    onRefreshTables: () -> Unit
) {
    SectionCard(
        title = "Status Koneksi Backend Go RLS",
        subtitle = "Endpoint default emulator Android menggunakan http://10.0.2.2:8080 atau domain demo HTTPS."
    ) {
        OutlinedTextField(
            value = apiBaseUrl,
            onValueChange = onApiBaseUrlChange,
            label = { Text("URL Backend API") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_api_base_url")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                healthLoading -> SemanticBadge("Memeriksa /health/ready...", "warn", darkTheme)
                healthResult != null -> SemanticBadge(
                    label = "API Siap: ${healthResult.rawSummary}",
                    tone = if (healthResult.httpCode in 200..299) "ok" else "warn",
                    darkTheme = darkTheme
                )
                healthError != null -> SemanticBadge("API Offline / Belum Terhubung", "err", darkTheme)
                else -> SemanticBadge("Belum Diperiksa", "warn", darkTheme)
            }
            Button(
                onClick = onProbeHealth,
                enabled = !healthLoading,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_probe_health")
            ) {
                Text("Cek Koneksi API")
            }
        }
        if (healthError != null) {
            Text(
                text = healthError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    SectionCard(
        title = "Autentikasi Kasir & Staf Gerai",
        subtitle = "Masuk dengan slug gerai, email staf, dan PIN 6 digit untuk memuat meja QR secara langsung."
    ) {
        if (session == null) {
            OutlinedTextField(
                value = storeSlug,
                onValueChange = onStoreSlugChange,
                label = { Text("Slug Gerai") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_store_slug")
            )
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email Staf / Pemilik") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_staff_email")
            )
            OutlinedTextField(
                value = pin,
                onValueChange = onPinChange,
                label = { Text("PIN Kasir (6 digit)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_staff_pin")
            )
            if (authError != null) {
                SemanticBadge(label = authError, tone = "err", darkTheme = darkTheme)
            }
            Button(
                onClick = onLogin,
                enabled = !authLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_staff_login")
            ) {
                Text(if (authLoading) "Memverifikasi PIN..." else "Masuk Kasir")
            }
        } else {
            SemanticBadge(
                label = "Sesi Aktif: ${session.staffName} (${session.role}) @ ${session.storeSlug}",
                tone = "ok",
                darkTheme = darkTheme
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRefreshTables,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_refresh_tables")
                ) {
                    Text("Muat Ulang Meja QR")
                }
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_staff_logout")
                ) {
                    Text("Keluar Sesi")
                }
            }
        }
    }

    SectionCard(
        title = "Monitor Meja QR Pelanggan",
        subtitle = "Daftar meja aktif yang terisolasi per tenant melalui PostgreSQL Row-Level Security."
    ) {
        when {
            session == null -> {
                Text(
                    text = "Status Kosong: Silakan masuk ke sesi kasir di atas untuk menampilkan token QR meja gerai Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            tablesLoading -> {
                SemanticBadge("Memuat daftar meja QR dari server...", "warn", darkTheme)
            }
            tablesError != null -> {
                SemanticBadge("Gagal memuat meja: $tablesError", "err", darkTheme)
            }
            tables.isEmpty() -> {
                Text(
                    text = "Belum ada meja QR yang terdaftar pada gerai ini. Tambahkan meja melalui ruang kerja Web POS.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                tables.forEach { table ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Meja ${table.tableNumber} (${table.area}, ${table.capacity} kursi)",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Token QR: ${table.qrToken.ifBlank { "tersimpan-aman" }}",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SemanticBadge(
                                label = if (table.isActive) "Aktif" else "Nonaktif",
                                tone = if (table.isActive) "ok" else "warn",
                                darkTheme = darkTheme
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MidtransAndWalletTab(
    darkTheme: Boolean,
    session: StaffSessionState?,
    loading: Boolean,
    error: String?,
    snapshot: MidtransCredentialSnapshot?,
    onRefresh: () -> Unit
) {
    SectionCard(
        title = "Status Kredensial Midtrans Core API (BYOK)",
        subtitle = "Mode Sandbox diizinkan sejak gerai baru dibuat. Mode Production dikunci hingga verifikasi KYB disetujui."
    ) {
        when {
            session == null -> {
                SemanticBadge("Sesi kasir belum masuk (menampilkan aturan standar)", "warn", darkTheme)
                Text(
                    text = "Masuk pada tab Kasir & QR untuk memeriksa status Merchant ID Midtrans dan status KYB gerai secara langsung.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            loading -> SemanticBadge("Memuat konfigurasi Midtrans gerai...", "warn", darkTheme)
            error != null -> {
                SemanticBadge("Gagal memuat kredensial: $error", "err", darkTheme)
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Coba Muat Ulang")
                }
            }
            snapshot != null -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SemanticBadge(
                        label = "Lingkungan: ${snapshot.environment.uppercase()}",
                        tone = if (snapshot.environment == "production") "ok" else "warn",
                        darkTheme = darkTheme
                    )
                    SemanticBadge(
                        label = "KYB: ${snapshot.kybStatus.uppercase()}",
                        tone = if (snapshot.kybStatus == "verified") "ok" else "warn",
                        darkTheme = darkTheme
                    )
                }
                Text(
                    text = "Merchant ID: ${snapshot.merchantId}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "QRIS Dinamis Otomatis: ${if (snapshot.isEnabled) "Aktif" else "Nonaktif"} | Fallback QRIS Statis Kasir: ${if (snapshot.fallbackStaticQris) "Siaga" else "Mati"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_refresh_midtrans")
                ) {
                    Text("Segarkan Status Midtrans")
                }
            }
            else -> {
                Text(
                    text = "Belum ada kredensial BYOK Midtrans yang tersimpan untuk gerai ini.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    SectionCard(
        title = "Kesiapan QRIS Nasional & Google Pay Android",
        subtitle = "Transparansi metode pembayaran domestik Bank Indonesia dan dompet kartu internasional."
    ) {
        SemanticBadge("QRIS Dinamis & Statis: Siap Operasional IDR", "ok", darkTheme)
        Text(
            text = "Seluruh transaksi Rupiah dihitung dalam bilangan bulat (integer IDR) di server Go dan diverifikasi menggunakan tanda tangan SHA-512 Midtrans.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        SemanticBadge("Google Pay (Kartu Tokenisasi EMV): Siap di Chrome/Android", "warn", darkTheme)
        Text(
            text = "Google Pay untuk kartu berlogo Visa/Mastercard melalui jalur 3DS Midtrans bergantung pada dukungan bank penerbit kartu tamu dan aktivasi channel kartu merchant.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NetworkAndPrinterTab(
    darkTheme: Boolean,
    mikrotik: MikrotikVlanSnapshot?,
    printerIp: String,
    onPrinterIpChange: (String) -> Unit,
    printerPortText: String,
    onPrinterPortChange: (String) -> Unit,
    paperWidthMm: Int,
    onSelectPaperWidth: (Int) -> Unit,
    busy: Boolean,
    feedback: String,
    isOnline: Boolean,
    onProbeSocket: () -> Unit,
    onTestPrint: () -> Unit,
    onKickDrawer: () -> Unit
) {
    SectionCard(
        title = "Ringkasan Segmentasi VLAN MikroTik RouterOS v7",
        subtitle = "Memisahkan trafik kasir dan printer thermal dari Wi-Fi publik pengunjung kafe."
    ) {
        val model = mikrotik?.routerModel ?: "MikroTik hAP ax2 (Profil Standar)"
        val posCidr = mikrotik?.posVlanCidr ?: "192.168.10.0/24"
        val staffCidr = mikrotik?.staffVlanCidr ?: "192.168.20.0/24"
        val guestCidr = mikrotik?.guestVlanCidr ?: "192.168.30.0/24"
        val sftpPort = mikrotik?.sftpPort ?: 2222
        val winboxPort = mikrotik?.winboxPort ?: 8291

        Text(text = "Router: $model", fontWeight = FontWeight.SemiBold)
        Text(
            text = "VLAN 10 POS & Printer: $posCidr\nVLAN 20 Staf Backoffice: $staffCidr\nVLAN 30 Tamu Wi-Fi (Terisolasi): $guestCidr\nManajemen Aman: Winbox TCP $winboxPort & SFTP TCP $sftpPort (FTP plain-text dimatikan)",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    SectionCard(
        title = "Penguji Printer Thermal ESC/POS LAN (TCP:9100)",
        subtitle = "Koneksi soket langsung dari tablet Android ke printer kasir atau dapur tanpa kunci vendor."
    ) {
        OutlinedTextField(
            value = printerIp,
            onValueChange = onPrinterIpChange,
            label = { Text("Alamat IPv4 Printer LAN") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_printer_ip")
        )
        OutlinedTextField(
            value = printerPortText,
            onValueChange = onPrinterPortChange,
            label = { Text("Port TCP (standar 9100)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_printer_port")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onSelectPaperWidth(58) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_paper_58")
            ) {
                Text(if (paperWidthMm == 58) "[Aktif] Kertas 58mm" else "Kertas 58mm")
            }
            OutlinedButton(
                onClick = { onSelectPaperWidth(80) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_paper_80")
            ) {
                Text(if (paperWidthMm == 80) "[Aktif] Kertas 80mm" else "Kertas 80mm")
            }
        }
        SemanticBadge(
            label = feedback,
            tone = if (isOnline) "ok" else "warn",
            darkTheme = darkTheme
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onProbeSocket,
                enabled = !busy,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("btn_probe_printer")
            ) {
                Text("Tes Soket")
            }
            Button(
                onClick = onTestPrint,
                enabled = !busy,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("btn_test_print")
            ) {
                Text("Uji Cetak Struk")
            }
        }
        OutlinedButton(
            onClick = onKickDrawer,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("btn_kick_drawer")
        ) {
            Text("Buka Laci Kasir RJ11")
        }
    }
}

@Composable
private fun PermissionsAndStorageTab(
    context: Context,
    darkTheme: Boolean,
    session: StaffSessionState?,
    kybStatus: String
) {
    var notifGranted by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    var notifMessage by remember { mutableStateOf("") }
    var cacheBytes by remember { mutableLongStateOf(calculateFolderBytes(context.cacheDir) + calculateFolderBytes(context.filesDir)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        notifMessage = if (granted) {
            "Izin notifikasi Android diberikan untuk peringatan pesanan dapur."
        } else {
            "Izin notifikasi ditolak. Aktifkan melalui Pengaturan Aplikasi bila diperlukan."
        }
    }

    val lanConnected = remember { checkNetworkTransport(context) }

    SectionCard(
        title = "Izin Runtime Android & Jaringan Lokal",
        subtitle = "Pemeriksaan izin POST_NOTIFICATIONS serta konektivitas Wi-Fi/Ethernet tablet kasir."
    ) {
        SemanticBadge(
            label = if (notifGranted) "POST_NOTIFICATIONS: Diizinkan" else "POST_NOTIFICATIONS: Belum Diizinkan",
            tone = if (notifGranted) "ok" else "warn",
            darkTheme = darkTheme
        )
        SemanticBadge(
            label = lanConnected,
            tone = if (lanConnected.contains("Terhubung")) "ok" else "warn",
            darkTheme = darkTheme
        )
        if (notifMessage.isNotBlank()) {
            Text(
                text = notifMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_request_notif_perm")
                ) {
                    Text("Minta Izin Notifikasi")
                }
            }
            OutlinedButton(
                onClick = {
                    if (hasNotificationPermission(context)) {
                        sendLocalTestNotification(context)
                        notifMessage = "Notifikasi uji pesanan #A-001 dikirim ke panel sistem Android."
                    } else {
                        notifMessage = "Berikan izin POST_NOTIFICATIONS terlebih dahulu sebelum mengirim notifikasi uji."
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_send_test_notif")
            ) {
                Text("Kirim Notifikasi Uji")
            }
        }
    }

    SectionCard(
        title = "Penyimpanan Cache Lokal & Identitas Paket",
        subtitle = "Ruang penyimpanan direktori internal aplikasi serta informasi tata kelola akun."
    ) {
        Text(
            text = "ID Paket Resmi: online.abbdullaziz.mycaffe",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Penggunaan Cache & Berkas Lokal: ${cacheBytes / 1024} KB",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Sesi Tenant: ${session?.storeName ?: "Belum masuk"} | Status KYB: ${kybStatus.uppercase()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                cacheBytes = calculateFolderBytes(context.cacheDir) + calculateFolderBytes(context.filesDir)
            },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("btn_clear_local_cache")
        ) {
            Text("Bersihkan Cache Lokal")
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkNetworkTransport(context: Context): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "Jaringan: Tidak diketahui"
    val active = cm.activeNetwork ?: return "Jaringan: Terputus (Mode Offline Kasir)"
    val caps = cm.getNetworkCapabilities(active) ?: return "Jaringan: Terbatas"
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Jaringan: Terhubung via Ethernet LAN"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Jaringan: Terhubung via Wi-Fi Gerai"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Jaringan: Terhubung via Seluler Backup"
        else -> "Jaringan: Terhubung"
    }
}

private fun sendLocalTestNotification(context: Context) {
    val channelId = "mycaffe_kds_alerts"
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Peringatan Pesanan & Dapur MyCaffe",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Pesanan Meja T-01 Siap")
        .setContentText("Tiket #A-001 (Es Kopi Susu Gula Aren) siap diantar ke meja pelanggan.")
        .setAutoCancel(true)
        .build()
    nm.notify(1001, notification)
}

private fun calculateFolderBytes(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    var total = 0L
    dir.walkTopDown().forEach { file ->
        if (file.isFile) total += file.length()
    }
    return total
}

