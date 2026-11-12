package com.azuredevid.cassy.core.hardware

import com.azuredevid.cassy.domain.model.ShiftCashSummary
import com.azuredevid.cassy.domain.model.ShiftStatus
import com.azuredevid.cassy.domain.model.ZReport
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class ShiftReportThermalBuilderTest {

    @Test
    fun buildZReportReceipt_generatesValidEscPosBytes() {
        val builder = ShiftReportThermalBuilder(EscPosReceiptBuilder.PaperWidth.WIDTH_80MM)

        val sampleReport = ZReport(
            shiftId = "shift-abc-12345678",
            storeId = "store-central-1",
            drawerId = "drawer-main",
            drawerName = "Laci Utama Kasir 1",
            openedBy = "staff-01",
            openedByName = "Ahmad Pratama",
            openedAt = "13/11/2026 08:00:00",
            closedBy = "staff-01",
            closedByName = "Ahmad Pratama",
            closedAt = "13/11/2026 16:30:00",
            status = ShiftStatus.CLOSED,
            summary = ShiftCashSummary(
                openingFloat = 500000L,
                cashSales = 1250000L,
                paidOuts = 50000L,
                tips = 20000L
            ),
            expectedCash = 1720000L,
            countedCash = 1720000L,
            variance = 0L,
            unsettledOpenTabsCount = 1,
            unsettledOpenTabsTotalAmount = 45000L
        )

        val bytes = builder.buildZReportReceipt(
            storeName = "Central Cafe Senopati",
            report = sampleReport
        )

        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val receiptText = String(bytes, Charset.forName("GBK"))

        // Assert crucial thermal receipt contents
        assertTrue(receiptText.contains("Central Cafe Senopati"))
        assertTrue(receiptText.contains("LAPORAN PENUTUPAN SHIFT (Z-REPORT)"))
        assertTrue(receiptText.contains("ID Shift"))
        assertTrue(receiptText.contains("Laci Kasir"))
        assertTrue(receiptText.contains("Ahmad Pratama"))
        assertTrue(receiptText.contains("Modal Awal (Opening Float)"))
        assertTrue(receiptText.contains("Penjualan Tunai"))
        assertTrue(receiptText.contains("Ekspektasi Kas Sistem"))
        assertTrue(receiptText.contains("Fisik Kas Dihitung"))
        assertTrue(receiptText.contains("PAS (BALANCE)"))
        assertTrue(receiptText.contains("PERHATIAN: TAGIHAN BELUM SELESAI"))
        assertTrue(receiptText.contains("Kasir Bertugas"))
        assertTrue(receiptText.contains("Supervisor / Manager"))

        // Verify partial cut command GS V (0x1D, 0x56) is present at the end
        val hasCutCommand = bytes.indices.any { i ->
            i < bytes.size - 1 && bytes[i] == 0x1D.toByte() && bytes[i + 1] == 0x56.toByte()
        }
        assertTrue(hasCutCommand)
    }

    @Test
    fun buildZReportReceipt_withNegativeVariance_showsShortWarning() {
        val builder = ShiftReportThermalBuilder(EscPosReceiptBuilder.PaperWidth.WIDTH_80MM)

        val sampleReport = ZReport(
            shiftId = "shift-xyz-98765432",
            storeId = "store-central-1",
            drawerId = "drawer-main",
            drawerName = "Laci 1",
            openedBy = "staff-02",
            openedByName = "Siti Kasir",
            openedAt = "13/11/2026 08:00:00",
            closedBy = "staff-02",
            closedByName = "Siti Kasir",
            closedAt = "13/11/2026 16:30:00",
            status = ShiftStatus.CLOSED,
            summary = ShiftCashSummary(
                openingFloat = 200000L,
                cashSales = 500000L
            ),
            expectedCash = 700000L,
            countedCash = 640000L,
            variance = -60000L,
            varianceReason = "Kembalian berlebih di pesanan #04"
        )

        val bytes = builder.buildZReportReceipt(
            storeName = "Kopi Kunyit Jakarta",
            report = sampleReport
        )

        val receiptText = String(bytes, Charset.forName("GBK"))
        assertTrue(receiptText.contains("KURANG (-Rp 60.000)"))
        assertTrue(receiptText.contains("Kembalian berlebih di pesanan #04"))
    }
}
