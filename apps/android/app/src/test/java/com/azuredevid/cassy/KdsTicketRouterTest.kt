package com.azuredevid.cassy

import com.azuredevid.cassy.core.hardware.KdsTicketRouter
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KdsTicketRouterTest {

    @Test
    fun `test buildKitchenTicket generates correct station payload with notes and options`() {
        val router = KdsTicketRouter()
        val items = listOf(
            KdsTicketRouter.StationItem(
                name = "Classic Iced Latte",
                quantity = 2,
                prepStation = "bar",
                variant = "Large",
                options = listOf("Oat Milk", "Less Ice"),
                notes = "Gula pisah"
            )
        )

        val bytes = router.buildKitchenTicket(
            stationTitle = "BAR / BARISTA",
            orderNumber = "ORD-20261108-01",
            tableOrQueue = "Meja T-04",
            items = items
        )

        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val text = String(bytes)
        assertTrue(text.contains("*** TIKET BAR / BARISTA ***"))
        assertTrue(text.contains("ORD-20261108-01"))
        assertTrue(text.contains("Meja T-04"))
        assertTrue(text.contains("2x Classic Iced Latte"))
        assertTrue(text.contains("Oat Milk"))
        assertTrue(text.contains("Gula pisah"))
    }
}

