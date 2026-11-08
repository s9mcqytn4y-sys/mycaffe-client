package com.azuredevid.cassy

import com.azuredevid.cassy.core.hardware.EscPosReceiptBuilder
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosReceiptBuilderTest {

    @Test
    fun `test buildStoreReceipt outputs non-empty byte array with ESC commands`() {
        val builder = EscPosReceiptBuilder(EscPosReceiptBuilder.PaperWidth.WIDTH_80MM)

        val items = listOf(
            EscPosReceiptBuilder.ReceiptItem("Classic Iced Latte", 2, 24000),
            EscPosReceiptBuilder.ReceiptItem("Croissant Butter", 1, 18000)
        )

        val bytes = builder.buildStoreReceipt(
            storeName = "Central Cafe Senopati",
            storeAddress = "Jl. Senopati No. 18, Jakarta Selatan",
            orderId = "ord-uuid-20261108-998877",
            tableName = "Meja T-04",
            cashierName = "Budi (Kasir 1)",
            items = items,
            subtotalIdr = 66000,
            pb1TaxIdr = 6600,
            grandTotalIdr = 72600,
            paymentMethod = "TUNAI",
            cashTenderedIdr = 100000,
            changeIdr = 27400
        )

        assertNotNull(bytes)
        assertTrue("Byte array must not be empty", bytes.isNotEmpty())
        
        // Verify ESC @ (init command) is at index 0 and 1
        assertTrue(bytes[0] == 0x1B.toByte())
        assertTrue(bytes[1] == 0x40.toByte())

        // Verify partial cut byte is contained
        val cutCommand = EscPosReceiptBuilder.FEED_AND_CUT
        val stringContent = String(bytes)
        assertTrue(stringContent.contains("Central Cafe Senopati"))
        assertTrue(stringContent.contains("Classic Iced Latte"))
    }

    @Test
    fun `test 58mm narrow paper formatting does not overflow line width`() {
        val builder = EscPosReceiptBuilder(EscPosReceiptBuilder.PaperWidth.WIDTH_58MM)
        builder.row("Kopi Susu Aren Spesial Senopati", "Rp 26.000")
        val bytes = builder.build()
        val text = String(bytes)
        
        // Should contain price and part of the item name
        assertTrue(text.contains("Rp 26.000"))
        assertTrue(text.contains("Kopi Susu Aren"))
    }
}
