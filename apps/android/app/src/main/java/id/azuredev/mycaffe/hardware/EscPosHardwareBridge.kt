package id.azuredev.mycaffe.hardware

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Open-Standard ESC/POS & RJ11 Cash Drawer Adapter for MyCaffe Android POS / KDS Companion.
 * Supports 58mm (32 cols) and 80mm (48 cols) LAN TCP:9100 thermal printers and Bluetooth SPP streams
 * without locking tenants into proprietary POS hardware.
 */
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

class EscPosHardwareBridge(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        // ESC @ (Initialize printer)
        private val CMD_INIT = byteArrayOf(0x1B, 0x40)
        // GS V B 0 (Partial cut with feed)
        private val CMD_CUT_PARTIAL = byteArrayOf(0x1D, 0x56, 0x42, 0x00)
        // ESC p m t1 t2 (RJ11 Cash Drawer Kick on Pin 2: 50ms ON, 250ms OFF)
        private val CMD_KICK_DRAWER_PIN2 = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
    }

    suspend fun printRawLanTicket(target: LanPrinterTarget, payload: ByteArray): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                withTimeout(target.connectTimeoutMs.toLong() + 2_000L) {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(target.host, target.port), target.connectTimeoutMs)
                        socket.soTimeout = target.connectTimeoutMs
                        writePacket(socket.getOutputStream(), payload, cutPaper = true)
                    }
                }
            }
        }

    suspend fun kickCashDrawerLan(target: LanPrinterTarget): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(target.host, target.port), target.connectTimeoutMs)
                    val out = socket.getOutputStream()
                    out.write(CMD_INIT)
                    out.write(CMD_KICK_DRAWER_PIN2)
                    out.flush()
                }
            }
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
