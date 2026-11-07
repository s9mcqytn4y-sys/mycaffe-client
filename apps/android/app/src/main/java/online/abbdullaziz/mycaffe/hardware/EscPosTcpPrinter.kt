package online.abbdullaziz.mycaffe.hardware

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.Locale

enum class PaperWidth(val mm: Int, val columns: Int) {
    MM_58(58, 32),
    MM_80(80, 48)
}

data class LanPrinterTarget(
    val host: String,
    val port: Int = 9100,
    val paperWidth: PaperWidth = PaperWidth.MM_80,
    val connectTimeoutMs: Int = 3_000
)

/**
 * Open-Standard ESC/POS & RJ11 Cash Drawer Adapter for MyCaffe Android POS / KDS Companion.
 * Official Package: online.abbdullaziz.mycaffe.
 */
class EscPosTcpPrinter(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private val CMD_INIT = byteArrayOf(0x1B, 0x40)
        private val CMD_CUT_PARTIAL = byteArrayOf(0x1D, 0x56, 0x42, 0x00)
        // ESC p m t1 t2 (RJ11 Cash Drawer Kick on Pin 2: 50ms ON, 250ms OFF)
        private val CMD_KICK_DRAWER_PIN2 = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
    }

    suspend fun probeTcpSocket(target: LanPrinterTarget): Result<Long> =
        withContext(ioDispatcher) {
            runCatching {
                val startNs = System.nanoTime()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(target.host.trim(), target.port), target.connectTimeoutMs)
                }
                (System.nanoTime() - startNs) / 1_000_000L
            }
        }

    suspend fun printRawLanTicket(target: LanPrinterTarget, payload: ByteArray): Result<Long> =
        withContext(ioDispatcher) {
            runCatching {
                val startNs = System.nanoTime()
                withTimeout(target.connectTimeoutMs.toLong() + 2_000L) {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(target.host.trim(), target.port), target.connectTimeoutMs)
                        socket.soTimeout = target.connectTimeoutMs
                        writePacket(socket.getOutputStream(), payload, cutPaper = true)
                    }
                }
                (System.nanoTime() - startNs) / 1_000_000L
            }
        }

    suspend fun kickCashDrawerLan(target: LanPrinterTarget): Result<Long> =
        withContext(ioDispatcher) {
            runCatching {
                val startNs = System.nanoTime()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(target.host.trim(), target.port), target.connectTimeoutMs)
                    val out = socket.getOutputStream()
                    out.write(CMD_INIT)
                    out.write(CMD_KICK_DRAWER_PIN2)
                    out.flush()
                }
                (System.nanoTime() - startNs) / 1_000_000L
            }
        }

    fun buildDiagnosticReceiptPayload(storeName: String, paperWidth: PaperWidth): ByteArray {
        val idr = NumberFormat.getIntegerInstance(Locale("id", "ID"))
        val divider = "-".repeat(paperWidth.columns)
        val text = buildString {
            appendLine("MYCAFFE BY AZUREDEVID")
            appendLine("Gerai: $storeName")
            appendLine("Paket: online.abbdullaziz.mycaffe")
            appendLine(divider)
            appendLine("1x Es Kopi Susu Aren   Rp ${idr.format(28_000L)}")
            appendLine("1x Croissant Butter    Rp ${idr.format(24_000L)}")
            appendLine(divider)
            appendLine("Subtotal               Rp ${idr.format(52_000L)}")
            appendLine("PB1 (10%)              Rp ${idr.format(5_200L)}")
            appendLine("Total                  Rp ${idr.format(57_200L)}")
            appendLine(divider)
            appendLine("Uji Cetak LAN TCP:9100 Berhasil")
            appendLine()
            appendLine()
        }
        return text.toByteArray(StandardCharsets.UTF_8)
    }

    private fun writePacket(out: OutputStream, payload: ByteArray, cutPaper: Boolean) {
        out.write(CMD_INIT)
        out.write(payload)
        if (cutPaper) {
            out.write(CMD_CUT_PARTIAL)
        }
        out.flush()
    }
}

typealias EscPosHardwareBridge = EscPosTcpPrinter

