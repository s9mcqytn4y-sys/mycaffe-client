package com.azuredevid.cassy.core.hardware

import com.azuredevid.cassy.domain.model.ShiftCashSummary
import com.azuredevid.cassy.domain.model.XReport
import com.azuredevid.cassy.domain.model.ZReport
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Generates thermal receipt ESC/POS byte payloads for 58mm/80mm printers
 * for Cashier Shift Close (Z-Report) and Mid-Shift Reading (X-Report).
 */
class ShiftReportThermalBuilder(
    private val paperWidth: EscPosReceiptBuilder.PaperWidth = EscPosReceiptBuilder.PaperWidth.WIDTH_80MM
) {
    private val rupiahFormat = NumberFormat.getIntegerInstance(Locale("id", "ID"))

    private fun formatRupiah(amount: Long): String {
        return "Rp " + rupiahFormat.format(amount)
    }

    /**
     * Builds ESC/POS byte payload for mid-shift X-Report.
     * Respects [XReport.expectedRedacted] by hiding expected cash for blind cashier reads.
     */
    fun buildXReportReceipt(
        storeName: String,
        report: XReport
    ): ByteArray {
        val builder = EscPosReceiptBuilder(paperWidth)

        builder.alignCenter()
        builder.title(storeName.ifBlank { "KASIR POS" })
        builder.bold(true)
        builder.text("LAPORAN BACAAN SHIFT (X-REPORT)")
        builder.bold(false)
        builder.doubleSeparator()

        val shortShiftId = if (report.shiftId.length > 8) report.shiftId.substring(0, 8).uppercase() else report.shiftId.uppercase()
        builder.row("ID Shift", "#$shortShiftId")
        if (report.drawerName.isNotBlank()) {
            builder.row("Laci Kasir", report.drawerName)
        }
        builder.row("Kasir Buka", report.openedByName.ifBlank { report.openedBy })
        builder.row("Waktu Buka", report.openedAt.ifBlank { "-" })
        if (report.generatedAt.isNotBlank()) {
            builder.row("Dicetak", report.generatedAt)
        }
        builder.separator()

        appendCashSummary(builder, report.summary)
        builder.separator()

        if (report.expectedRedacted || report.expectedCash == null) {
            builder.row("Ekspektasi Kas", "Disamarkan (Blind Count)")
        } else {
            builder.bold(true)
            builder.row("Ekspektasi Kas Sistem", formatRupiah(report.expectedCash))
            builder.bold(false)
        }

        if (report.unsettledOpenTabsCount > 0) {
            builder.separator()
            builder.bold(true)
            builder.text("TAGIHAN MASIH TERBUKA")
            builder.bold(false)
            builder.row("Jumlah Tagihan", "${report.unsettledOpenTabsCount} pesanan")
            builder.row("Estimasi Nilai", formatRupiah(report.unsettledOpenTabsTotalAmount))
        }

        builder.emptyLine(1)
        builder.alignCenter()
        builder.text("Bacaan sementara - Shift masih berjalan")
        builder.feedAndCut()
        return builder.build()
    }

    /**
     * Builds ESC/POS byte payload for end-of-shift Z-Report.
     */
    fun buildZReportReceipt(
        storeName: String,
        report: ZReport
    ): ByteArray {
        val builder = EscPosReceiptBuilder(paperWidth)

        builder.alignCenter()
        builder.title(storeName.ifBlank { "KASIR POS" })
        builder.bold(true)
        builder.text("LAPORAN PENUTUPAN SHIFT (Z-REPORT)")
        builder.bold(false)
        builder.doubleSeparator()

        val shortShiftId = if (report.shiftId.length > 8) report.shiftId.substring(0, 8).uppercase() else report.shiftId.uppercase()
        builder.row("ID Shift", "#$shortShiftId")
        if (report.drawerName.isNotBlank()) {
            builder.row("Laci Kasir", report.drawerName)
        }
        builder.row("Kasir Buka", report.openedByName.ifBlank { report.openedBy })
        builder.row("Waktu Buka", report.openedAt.ifBlank { "-" })
        if (!report.closedAt.isNullOrBlank()) {
            builder.row("Waktu Tutup", report.closedAt)
        }
        if (!report.closedByName.isNullOrBlank()) {
            builder.row("Kasir Tutup", report.closedByName)
        }
        if (!report.overrideByName.isNullOrBlank()) {
            builder.row("Persetujuan Owner", report.overrideByName)
        }
        builder.separator()

        appendCashSummary(builder, report.summary)
        builder.separator()

        builder.bold(true)
        builder.row("Ekspektasi Kas Sistem", formatRupiah(report.expectedCash))
        builder.row("Fisik Kas Dihitung", formatRupiah(report.countedCash))
        builder.bold(false)
        builder.doubleSeparator()

        val variance = report.variance
        val varianceStatus = when {
            variance == 0L -> "PAS (BALANCE)"
            variance > 0L -> "LEBIH (+${formatRupiah(variance)})"
            else -> "KURANG (-${formatRupiah(abs(variance))})"
        }
        builder.bold(true)
        builder.row("STATUS REKONSILIASI", varianceStatus)
        builder.bold(false)

        if (!report.varianceReason.isNullOrBlank()) {
            builder.text("Alasan Selisih: ${report.varianceReason}")
        }
        if (!report.notes.isNullOrBlank()) {
            builder.text("Catatan: ${report.notes}")
        }

        if (report.amended) {
            builder.separator()
            builder.bold(true)
            builder.text("PEMBARUAN OFFLINE TERLAMBAT")
            builder.bold(false)
            builder.row("Jumlah Pesanan", "${report.lateOfflineOrders} pesanan")
            builder.row("Nominal Masuk", formatRupiah(report.lateOfflineAmount))
        }

        if (report.unsettledOpenTabsCount > 0) {
            builder.separator()
            builder.bold(true)
            builder.text("PERHATIAN: TAGIHAN BELUM SELESAI")
            builder.bold(false)
            builder.row("Jumlah Tagihan Terbuka", "${report.unsettledOpenTabsCount} pesanan")
            builder.row("Estimasi Nilai Terbuka", formatRupiah(report.unsettledOpenTabsTotalAmount))
        }

        builder.separator()
        builder.emptyLine(1)
        if (paperWidth == EscPosReceiptBuilder.PaperWidth.WIDTH_80MM) {
            builder.row("Kasir Bertugas", "Supervisor / Manager")
            builder.emptyLine(3)
            builder.row("(....................)", "(....................)")
        } else {
            builder.text("Kasir: (....................)")
            builder.emptyLine(2)
            builder.text("Supervisor: (....................)")
        }

        builder.emptyLine(1)
        builder.alignCenter()
        builder.text("Dokumen sah penutupan shift - Cassy")
        builder.feedAndCut()

        return builder.build()
    }

    private fun appendCashSummary(builder: EscPosReceiptBuilder, summary: ShiftCashSummary) {
        builder.bold(true)
        builder.text("RINGKASAN MUTASI KAS LACI")
        builder.bold(false)
        builder.row("Modal Awal (Opening Float)", formatRupiah(summary.openingFloat))
        builder.row("(+) Penjualan Tunai", formatRupiah(summary.cashSales))
        if (summary.cashIns > 0) {
            builder.row("(+) Kas Masuk Operasional", formatRupiah(summary.cashIns))
        }
        if (summary.tips > 0) {
            builder.row("(+) Tips Tunai", formatRupiah(summary.tips))
        }
        if (summary.paidOuts > 0) {
            builder.row("(-) Kas Keluar (Paid Out)", formatRupiah(summary.paidOuts))
        }
        if (summary.cashRefunds > 0) {
            builder.row("(-) Pengembalian (Refund)", formatRupiah(summary.cashRefunds))
        }
        if (summary.staffMeals > 0) {
            builder.row("(-) Makan Staf (Staff Meal)", formatRupiah(summary.staffMeals))
        }
    }
}
