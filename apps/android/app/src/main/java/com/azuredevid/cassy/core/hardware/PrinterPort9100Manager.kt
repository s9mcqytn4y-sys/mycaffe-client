package com.azuredevid.cassy.core.hardware

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PrinterPort9100Manager implements non-blocking TCP socket communication with LAN thermal printers (Port 9100).
 * Handles ESC/POS raw bytes transmission and Cash Drawer Kick Pulse (0x1B, 0x70, 0x00, 0x19, 0xFA).
 */
class PrinterPort9100Manager(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // ESC p m t1 t2 -> ESC/POS drawer kick pulse standard
    private val drawerKickCommand = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())

    /**
     * Kicks open the cash drawer connected via RJ11 to the thermal receipt printer.
     */
    suspend fun openCashDrawer(printerIp: String, port: Int = 9100): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            val selector = SelectorManager(ioDispatcher)
            val socket = aSocket(selector).tcp().connect(printerIp, port) {
                socketTimeout = 3000
            }
            try {
                val writeChannel = socket.openWriteChannel(autoFlush = true)
                writeChannel.writeFully(drawerKickCommand)
                true
            } finally {
                socket.close()
                selector.close()
            }
        }
    }

    /**
     * Sends raw ESC/POS formatted byte array directly to the kitchen or cashier printer.
     */
    suspend fun printRawBytes(printerIp: String, payload: ByteArray, port: Int = 9100): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            val selector = SelectorManager(ioDispatcher)
            val socket = aSocket(selector).tcp().connect(printerIp, port) {
                socketTimeout = 5000
            }
            try {
                val writeChannel = socket.openWriteChannel(autoFlush = true)
                writeChannel.writeFully(payload)
                true
            } finally {
                socket.close()
                selector.close()
            }
        }
    }
}
