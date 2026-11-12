package com.azuredevid.cassy.core.hardware

import com.azuredevid.cassy.domain.model.ZReport
import java.text.NumberFormat
import java.util.Locale

/**
 * Generates thermal receipt ESC/POS byte payloads for 80mm printers
 * for Cashier Shift Close (Z-Report) and Mid-Shift Reading (X-Report).
 * Adheres strictly to Zero Emoji, Zero Em Dash, and Integer Rupiah rules.
 */
class ShiftReportThermalBuilder(
    private val paperWidth: EscPosReceiptBuilder.PaperWidth = EscPosReceiptBuilder.PaperWidth.WIDTH_80MM
) {
    private val rupiahFormat = NumberFormat.getIntegerInstance(Locale("id", "ID"))

    private fun formatRupiah(amount: Long): String {
        return "Rp " + rupiahFormat.format(amount)
    }

    /**
     * Builds ESC/POS 80mm byte payload for end-of-shift Z-Report.
     */
    fun buildZReportReceipt(
        storeName: String,
        report: ZReport
    ): ByteArray {
        val builder = EscPosReceiptBuilder(paperWidth)

        builder.alignCenter()
        builder.title(storeName.ifBlank { "CASSY POS" })
        builder.bold(true)
        builder.text("LAPORAN PENUTUPAN SHIFT (Z-REPORT)")
        builder.bold(false)
        builder.doubleSeparator()

        // Shift metadata
        val shortShiftId = if (report.shiftId.length > 8) report.shiftId.substring(0, 8).uppercase() else report.shiftId.uppercase()
        builder.row("ID Shift", "#$shortShiftId")
        if (report.drawerName.isNotBlank()) {
            builder.row("Laci Kasir", report.drawerName)
        }
        val staffName = report.openedByName.ifBlank { report.openedBy }
        builder.row("Kasir Buka", staffName)
        builder.row("Waktu Buka", report.openedAt.ifBlank { "-" })
        if (!report.closedAt.isNullOrBlank()) {
            builder.row("Waktu Tutup", report.closedAt)
        }
        if (!report.closedByName.isNullOrBlank()) {
            builder.row("Kasir Tutup", report.closedByName)
        }
        builder.separator()

        // Cash flow summary
        builder.bold(true)
        builder.text("RINGKASAN MUTASI KAS LACI")
        builder.bold(false)
        builder.row("Modal Awal (Opening Float)", formatRupiah(report.summary.openingFloat))
        builder.row("(+) Penjualan Tunai", formatRupiah(report.summary.cashSales))
        if (report.summary.cashIns > 0) {
            builder.row("(+) Kas Masuk Operasional", formatRupiah(report.summary.cashIns))
        }
        if (report.summary.tips > 0) {
            builder.row("(+) Tips Tunai", formatRupiah(report.summary.tips))
        }
        if (report.summary.paidOuts > 0) {
            builder.row("(-) Kas Keluar (Paid Out)", formatRupiah(report.summary.paidOuts))
        }
        if (report.summary.cashRefunds > 0) {
            builder.row("(-) Pengembalian (Refund)", formatRupiah(report.summary.cashRefunds))
        }
        if (report.summary.staffMeals > 0) {
            builder.row("(-) Makan Staf (Staff Meal)", formatRupiah(report.summary.staffMeals))
        }
        builder.separator()

        // Expected vs Counted
        builder.bold(true)
        builder.row("Ekspektasi Kas Sistem", formatRupiah(report.expectedCash))
        builder.row("Fisik Kas Dihitung", formatRupiah(report.countedCash))
        builder.bold(false)
        builder.doubleSeparator()

        // Reconciliation Variance
        val variance = report.variance
        val varianceStatus = when {
            variance == 0L -> "PAS (BALANCE)"
            variance > 0L -> "LEBIH (+${formatRupiah(variance)})"
            else -> "KURANG (-${formatRupiah(Math.abs(variance))})"
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

        // Unsettled tabs warning
        if (report.unsettledOpenTabsCount > 0) {
            builder.separator()
            builder.bold(true)
            builder.text("PERHATIAN: TAGIHAN BELUM SELESAI")
            builder.bold(false)
            builder.row("Jumlah Tagihan Terbuka", "${report.unsettledOpenTabsCount} pesanan")
            builder.row("Estimasi Nilai Terbuka", formatRupiah(report.unsettledOpenTabsTotalAmount))
        }

        // Signatures
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
        builder.text("Dokumen sah penutupan shift - Cassy POS")
        builder.feedAndCut()

        return builder.build()
    }
}
