package online.abbdullaziz.mycaffe.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class HealthStatusResult(
    val httpCode: Int,
    val statusLabel: String,
    val latencyMs: Long,
    val rawSummary: String
)

data class StaffSessionState(
    val accessToken: String,
    val staffName: String,
    val role: String,
    val storeId: String,
    val storeSlug: String,
    val storeName: String
)

data class PosTableItem(
    val id: String,
    val tableNumber: String,
    val capacity: Int,
    val area: String,
    val qrToken: String,
    val isActive: Boolean
)

data class MidtransCredentialSnapshot(
    val environment: String,
    val merchantId: String,
    val isEnabled: Boolean,
    val fallbackStaticQris: Boolean,
    val kybStatus: String
)

data class MikrotikVlanSnapshot(
    val routerModel: String,
    val posVlanCidr: String,
    val staffVlanCidr: String,
    val guestVlanCidr: String,
    val sftpPort: Int,
    val winboxPort: Int
)

class MyCaffeApiClient {

    private fun normalizeBase(baseUrl: String): String =
        baseUrl.trim().trimEnd('/')

    suspend fun checkHealthReady(baseUrl: String): Result<HealthStatusResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val startNs = System.nanoTime()
                val conn = (URL("${normalizeBase(baseUrl)}/health/ready").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3_500
                    readTimeout = 3_500
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val code = conn.responseCode
                    val body = readBody(conn, code)
                    val latency = (System.nanoTime() - startNs) / 1_000_000L
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val status = json?.optString("status")?.takeIf { it.isNotBlank() }
                        ?: if (code in 200..299) "ready" else "degraded"
                    HealthStatusResult(
                        httpCode = code,
                        statusLabel = status,
                        latencyMs = latency,
                        rawSummary = "HTTP $code ($status) dalam ${latency}ms"
                    )
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun loginStaff(
        baseUrl: String,
        storeSlug: String,
        email: String,
        pin: String
    ): Result<StaffSessionState> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().apply {
                    put("store_slug", storeSlug.trim().lowercase())
                    put("email", email.trim().lowercase())
                    put("pin", pin.trim())
                }
                val (code, body) = executeJson(
                    url = "${normalizeBase(baseUrl)}/api/v1/auth/staff/login",
                    method = "POST",
                    token = null,
                    body = payload.toString()
                )
                if (code !in 200..299) {
                    throw IllegalStateException(extractErrorMessage(code, body))
                }
                val root = JSONObject(body)
                val staff = root.optJSONObject("staff") ?: JSONObject()
                val store = staff.optJSONObject("store") ?: JSONObject()
                StaffSessionState(
                    accessToken = root.getString("access_token"),
                    staffName = staff.optString("full_name", email),
                    role = staff.optString("role", "cashier"),
                    storeId = store.optString("id", ""),
                    storeSlug = store.optString("slug", storeSlug),
                    storeName = store.optString("name", storeSlug)
                )
            }
        }

    suspend fun fetchTables(baseUrl: String, token: String): Result<List<PosTableItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (code, body) = executeJson(
                    url = "${normalizeBase(baseUrl)}/api/v1/pos/tables",
                    method = "GET",
                    token = token,
                    body = null
                )
                if (code !in 200..299) {
                    throw IllegalStateException(extractErrorMessage(code, body))
                }
                val root = JSONObject(body)
                val arr = root.optJSONArray("tables") ?: JSONArray()
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        add(
                            PosTableItem(
                                id = item.optString("id", "tbl-$i"),
                                tableNumber = item.optString("table_number", "T-${i + 1}"),
                                capacity = item.optInt("capacity", 4),
                                area = item.optString("area", "Indoor"),
                                qrToken = item.optString("qr_token", ""),
                                isActive = item.optBoolean("is_active", true)
                            )
                        )
                    }
                }
            }
        }

    suspend fun fetchMidtransSnapshot(baseUrl: String, token: String): Result<MidtransCredentialSnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (credCode, credBody) = executeJson(
                    url = "${normalizeBase(baseUrl)}/api/v1/store/payment-credentials",
                    method = "GET",
                    token = token,
                    body = null
                )
                if (credCode !in 200..299) {
                    throw IllegalStateException(extractErrorMessage(credCode, credBody))
                }
                val (kybCode, kybBody) = executeJson(
                    url = "${normalizeBase(baseUrl)}/api/v1/store/kyb",
                    method = "GET",
                    token = token,
                    body = null
                )
                val credRoot = JSONObject(credBody)
                val credObj = credRoot.optJSONObject("credentials") ?: credRoot
                val kybObj = if (kybCode in 200..299) {
                    val kr = JSONObject(kybBody)
                    kr.optJSONObject("kyb") ?: kr
                } else {
                    JSONObject()
                }
                MidtransCredentialSnapshot(
                    environment = credObj.optString("environment", "sandbox"),
                    merchantId = credObj.optString("merchant_id", "Belum diatur"),
                    isEnabled = credObj.optBoolean("is_enabled", false),
                    fallbackStaticQris = credObj.optBoolean("fallback_static_qris_enabled", true),
                    kybStatus = kybObj.optString("kyb_status", "unverified")
                )
            }
        }

    suspend fun fetchMikrotikSnapshot(baseUrl: String, token: String): Result<MikrotikVlanSnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (code, body) = executeJson(
                    url = "${normalizeBase(baseUrl)}/api/v1/store/network/mikrotik",
                    method = "GET",
                    token = token,
                    body = null
                )
                if (code !in 200..299) {
                    throw IllegalStateException(extractErrorMessage(code, body))
                }
                val root = JSONObject(body)
                val prof = root.optJSONObject("profile") ?: root
                MikrotikVlanSnapshot(
                    routerModel = prof.optString("router_model", "hAP ax2 (RouterOS v7)"),
                    posVlanCidr = prof.optString("pos_vlan_cidr", "192.168.10.0/24"),
                    staffVlanCidr = prof.optString("staff_vlan_cidr", "192.168.20.0/24"),
                    guestVlanCidr = prof.optString("guest_vlan_cidr", "192.168.30.0/24"),
                    sftpPort = prof.optInt("sftp_port", 2222),
                    winboxPort = prof.optInt("winbox_port", 8291)
                )
            }
        }

    private fun executeJson(
        url: String,
        method: String,
        token: String?,
        body: String?
    ): Pair<Int, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 4_000
            readTimeout = 4_000
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            if (body != null) {
                conn.outputStream.use { out ->
                    out.write(body.toByteArray(StandardCharsets.UTF_8))
                }
            }
            val code = conn.responseCode
            code to readBody(conn, code)
        } finally {
            conn.disconnect()
        }
    }

    private fun readBody(conn: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..399) conn.inputStream else conn.errorStream
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
    }

    private fun extractErrorMessage(code: Int, body: String): String {
        val json = runCatching { JSONObject(body) }.getOrNull()
        val msg = json?.optString("error")?.takeIf { it.isNotBlank() }
            ?: json?.optString("message")?.takeIf { it.isNotBlank() }
        return if (msg != null) "HTTP $code: $msg" else "HTTP $code gagal memproses permintaan"
    }
}

