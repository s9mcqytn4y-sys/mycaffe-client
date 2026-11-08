package com.azuredevid.cassy.core.hardware

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EscPosReceiptBuilder generates compliant ESC/POS binary payloads for 58mm (32 chars)
 * and 80mm (48 chars) thermal receipt printers on TCP port 9100.
 */
class EscPosReceiptBuilder(
    val paperWidth: PaperWidth = PaperWidth.WIDTH_80MM,
    private val charset: Charset = Charset.forName("GBK") // or UTF-8 / CP437 based on printer ROM
) {
    enum class PaperWidth(val columns: Int) {
        WIDTH_58MM(32),
        WIDTH_80MM(48)
    }

    private val outputStream = ByteArrayOutputStream()
    private val rupiahFormat = NumberFormat.getIntegerInstance(Locale("id", "ID"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))

    // ESC/POS Command Constants
    companion object {
        val ESC_INIT = byteArrayOf(0x1B, 0x40) // ESC @
        val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
        val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
        val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // ESC a 2
        val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1
        val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // ESC E 0
        val GS_TEXT_DOUBLE = byteArrayOf(0x1D, 0x21, 0x11) // GS ! 0x11 (2x width & 2x height)
        val GS_TEXT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00) // GS ! 0x00
        val FEED_AND_CUT = byteArrayOf(0x1B, 0x64, 0x03, 0x1D, 0x56, 0x41, 0x00) // Feed 3 lines & Partial Cut
        val DRAWER_KICK = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()) // RJ11 pulse
    }

    init {
        outputStream.write(ESC_INIT)
    }

    fun kickDrawer(): EscPosReceiptBuilder {
        outputStream.write(DRAWER_KICK)
        return this
    }

    fun alignCenter(): EscPosReceiptBuilder {
        outputStream.write(ESC_ALIGN_CENTER)
        return this
    }

    fun alignLeft(): EscPosReceiptBuilder {
        outputStream.write(ESC_ALIGN_LEFT)
        return this
    }

    fun alignRight(): EscPosReceiptBuilder {
        outputStream.write(ESC_ALIGN_RIGHT)
        return this
    }

    fun bold(enable: Boolean): EscPosReceiptBuilder {
        outputStream.write(if (enable) ESC_BOLD_ON else ESC_BOLD_OFF)
        return this
    }

    fun title(text: String): EscPosReceiptBuilder {
        alignCenter()
        outputStream.write(GS_TEXT_DOUBLE)
        outputStream.write(ESC_BOLD_ON)
        writeLine(text)
        outputStream.write(GS_TEXT_NORMAL)
        outputStream.write(ESC_BOLD_OFF)
        return this
    }

    fun text(text: String): EscPosReceiptBuilder {
        writeLine(text)
        return this
    }

    fun separator(char: Char = '-'): EscPosReceiptBuilder {
        alignLeft()
        val line = char.toString().repeat(paperWidth.columns)
        writeLine(line)
        return this
    }

    fun doubleSeparator(): EscPosReceiptBuilder {
        return separator('=')
    }

    /**
     * Formats two-column text with left and right alignment.
     * Truncates or wraps left column if sum of lengths exceeds total columns.
     */
    fun row(left: String, right: String): EscPosReceiptBuilder {
        alignLeft()
        val maxCols = paperWidth.columns
        val rightLen = right.length
        val maxLeftLen = maxCols - rightLen - 1

        val leftTrimmed = if (left.length > maxLeftLen) {
            left.substring(0, maxLeftLen.coerceAtLeast(0))
        } else {
            left
        }

        val spacesCount = (maxCols - leftTrimmed.length - rightLen).coerceAtLeast(1)
        val formatted = leftTrimmed + " ".repeat(spacesCount) + right
        writeLine(formatted)
        return this
    }

    /**
     * Prints an itemized row: "Product Name", "Qty x Price", "Total Price"
     */
    fun item(name: String, qty: Int, priceIdr: Long): EscPosReceiptBuilder {
        alignLeft()
        val totalIdr = qty * priceIdr
        val totalStr = "Rp " + rupiahFormat.format(totalIdr)
        
        // Print Product Name on first line
        writeLine(name)
        
        // Print "Qty x Price" on left, "Total" on right
        val detailLeft = "  ${qty}x @ Rp ${rupiahFormat.format(priceIdr)}"
        row(detailLeft, totalStr)
        return this
    }

    fun emptyLine(lines: Int = 1): EscPosReceiptBuilder {
        repeat(lines) {
            outputStream.write("\n".toByteArray(charset))
        }
        return this
    }

    fun feedAndCut(): EscPosReceiptBuilder {
        outputStream.write(FEED_AND_CUT)
        return this
    }

    private fun writeLine(text: String) {
        outputStream.write(text.toByteArray(charset))
        outputStream.write("\n".toByteArray(charset))
    }

    fun build(): ByteArray {
        return outputStream.toByteArray()
    }

    /**
     * Helper to build a complete customer receipt for MyCaffe / Cassy POS.
     */
    fun buildStoreReceipt(
        storeName: String,
        storeAddress: String,
        orderId: String,
        tableName: String,
        cashierName: String,
        items: List<ReceiptItem>,
        subtotalIdr: Long,
        pb1TaxIdr: Long,
        grandTotalIdr: Long,
        paymentMethod: String,
        cashTenderedIdr: Long? = null,
        changeIdr: Long? = null,
        footerNotes: String = "Terima kasih atas kunjungan Anda!\nWiFi Pass: cassycaffe2026"
    ): ByteArray {
        title(storeName)
        alignCenter()
        text(storeAddress)
        separator()
        
        alignLeft()
        row("No. Pesanan:", orderId.takeLast(10).uppercase())
        row("Meja / Tipe:", tableName)
        row("Kasir:", cashierName)
        row("Waktu:", dateFormat.format(Date()))
        doubleSeparator()

        items.forEach { item ->
            item(item.name, item.qty, item.priceIdr)
        }

        separator()
        row("Subtotal:", "Rp " + rupiahFormat.format(subtotalIdr))
        row("PB1 (10%):", "Rp " + rupiahFormat.format(pb1TaxIdr))
        bold(true)
        row("TOTAL TAGIHAN:", "Rp " + rupiahFormat.format(grandTotalIdr))
        bold(false)
        separator()

        row("Metode Bayar:", paymentMethod)
        if (cashTenderedIdr != null && cashTenderedIdr > 0) {
            row("Tunai Diterima:", "Rp " + rupiahFormat.format(cashTenderedIdr))
            row("Kembalian:", "Rp " + rupiahFormat.format(changeIdr ?: 0))
        }
        separator()

        alignCenter()
        text(footerNotes)
        emptyLine(1)
        feedAndCut()

        return build()
    }

    data class ReceiptItem(
        val name: String,
        val qty: Int,
        val priceIdr: Long
    )
}
