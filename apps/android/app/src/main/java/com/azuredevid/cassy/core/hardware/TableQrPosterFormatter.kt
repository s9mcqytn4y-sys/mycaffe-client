package com.azuredevid.cassy.core.hardware

import android.graphics.Bitmap
import android.graphics.Color
import com.azuredevid.cassy.core.util.QrBitmapGenerator
import java.io.ByteArrayOutputStream

/**
 * Formats an 80mm ESC/POS thermal table QR poster with store name, table label, and GS v 0 raster QR.
 */
object TableQrPosterFormatter {

    private val ESC_INIT = byteArrayOf(0x1B, 0x40)
    private val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val DOUBLE_SIZE = byteArrayOf(0x1D, 0x21, 0x11)
    private val NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00)
    private val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x42, 0x00)

    fun buildEscPosPoster(
        storeName: String,
        tableLabel: String,
        section: String,
        qrPayloadUrl: String
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(ESC_INIT)
        out.write(ALIGN_CENTER)

        out.write(BOLD_ON)
        out.write(DOUBLE_SIZE)
        out.write(storeName.uppercase().toByteArray(Charsets.UTF_8))
        out.write("\n".toByteArray(Charsets.UTF_8))
        out.write(NORMAL_SIZE)
        out.write("\n".toByteArray(Charsets.UTF_8))

        out.write(DOUBLE_SIZE)
        out.write(tableLabel.toByteArray(Charsets.UTF_8))
        out.write("\n".toByteArray(Charsets.UTF_8))
        out.write(NORMAL_SIZE)
        out.write(BOLD_OFF)

        if (section.isNotBlank()) {
            out.write("Area: $section\n".toByteArray(Charsets.UTF_8))
        }
        out.write("\n".toByteArray(Charsets.UTF_8))

        val qrBitmap = QrBitmapGenerator.createQrBitmap(qrPayloadUrl, width = 320, height = 320)
        if (qrBitmap != null) {
            out.write(bitmapToEscPosRaster(qrBitmap))
            out.write("\n".toByteArray(Charsets.UTF_8))
        }

        out.write("Pindai QR untuk lihat menu & pesan\n".toByteArray(Charsets.UTF_8))
        out.write("Powered by Cassy\n\n\n".toByteArray(Charsets.UTF_8))
        out.write(CUT_PAPER)
        return out.toByteArray()
    }

    private fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8
        val out = ByteArrayOutputStream()
        // GS v 0 m xL xH yL yH
        out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
        out.write(bytesPerRow and 0xFF)
        out.write((bytesPerRow shr 8) and 0xFF)
        out.write(height and 0xFF)
        out.write((height shr 8) and 0xFF)

        for (y in 0 until height) {
            for (xByte in 0 until bytesPerRow) {
                var slice = 0
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val isDark = Color.red(pixel) < 128
                        if (isDark) {
                            slice = slice or (1 shl (7 - bit))
                        }
                    }
                }
                out.write(slice)
            }
        }
        return out.toByteArray()
    }
}
