package com.azuredevid.cassy.core.hardware

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.azuredevid.cassy.core.util.QrBitmapGenerator
import java.io.FileOutputStream

/**
 * Generates an A6 white-label table tent-card PDF using the outlet's brand color and store name.
 */
object TablePdfPosterGenerator {

    // A6 in PostScript points (105mm x 148mm ~ 298 x 420 pt)
    private const val A6_WIDTH_PT = 298
    private const val A6_HEIGHT_PT = 420

    fun printTablePosterPdf(
        context: Context,
        storeName: String,
        brandHex: String?,
        tableLabel: String,
        section: String,
        qrPayloadUrl: String
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val accentColor = parseColorOrDefault(brandHex, Color.parseColor("#1F5F52"))
        val jobName = "Poster QR - $tableLabel"

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("poster_${tableLabel.replace(" ", "_")}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val doc = PdfDocument()
                try {
                    val pageInfo = PdfDocument.PageInfo.Builder(A6_WIDTH_PT, A6_HEIGHT_PT, 1).create()
                    val page = doc.startPage(pageInfo)
                    drawPosterPage(page.canvas, storeName, accentColor, tableLabel, section, qrPayloadUrl)
                    doc.finishPage(page)

                    FileOutputStream(destination.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    doc.close()
                }
            }
        }

        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
            .build()
        printManager.print(jobName, adapter, attrs)
    }

    private fun drawPosterPage(
        canvas: Canvas,
        storeName: String,
        accentColor: Int,
        tableLabel: String,
        section: String,
        qrPayloadUrl: String
    ) {
        canvas.drawColor(Color.parseColor("#F7F5F0"))

        val headerPaint = Paint().apply { color = accentColor }
        canvas.drawRect(0f, 0f, A6_WIDTH_PT.toFloat(), 68f, headerPaint)

        val storePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(storeName, A6_WIDTH_PT / 2f, 42f, storePaint)

        val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#17211E")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(tableLabel, A6_WIDTH_PT / 2f, 110f, tablePaint)

        if (section.isNotBlank()) {
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4A5954")
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(section, A6_WIDTH_PT / 2f, 130f, sectionPaint)
        }

        val qrBmp = QrBitmapGenerator.createQrBitmap(qrPayloadUrl, width = 360, height = 360)
        if (qrBmp != null) {
            val left = (A6_WIDTH_PT - 180) / 2f
            val top = 150f
            val cardPaint = Paint().apply { color = Color.WHITE }
            canvas.drawRoundRect(left - 10f, top - 10f, left + 190f, top + 190f, 12f, 12f, cardPaint)
            canvas.drawBitmap(qrBmp, null, android.graphics.RectF(left, top, left + 180f, top + 180f), null)
        }

        val ctaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#17211E")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Pindai QR untuk pesan & bayar", A6_WIDTH_PT / 2f, 376f, ctaPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7A75")
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Powered by Cassy", A6_WIDTH_PT / 2f, 400f, subPaint)
    }

    private fun parseColorOrDefault(hex: String?, fallback: Int): Int {
        if (hex.isNullOrBlank()) return fallback
        return try {
            Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (_: Exception) {
            fallback
        }
    }
}
