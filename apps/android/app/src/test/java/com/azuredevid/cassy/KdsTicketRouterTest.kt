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
            items = items,
            deliveryMode = KdsTicketRouter.OrderDeliveryMode.DINE_IN,
            roundNumber = 1
        )

        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val text = String(bytes)
        assertTrue(text.contains("*** TIKET BAR / BARISTA ***"))
        assertTrue(text.contains("ORD-20261108-01"))
        assertTrue(text.contains("Meja T-04"))
        assertTrue(text.contains("DINE_IN"))
        assertTrue(text.contains("2x Classic Iced Latte"))
        assertTrue(text.contains("Oat Milk"))
        assertTrue(text.contains("Gula pisah"))
    }

    @Test
    fun `test buildKitchenTicket includes failover redirection watermark header`() {
        val router = KdsTicketRouter()
        val items = listOf(
            KdsTicketRouter.StationItem(
                name = "Spaghetti Carbonara",
                quantity = 1,
                prepStation = "hot_kitchen",
                notes = "Extra parmesan"
            )
        )

        val bytes = router.buildKitchenTicket(
            stationTitle = "DAPUR PANAS",
            orderNumber = "ORD-20261108-88",
            tableOrQueue = "Meja T-12",
            items = items,
            isRedirected = true,
            originalStation = "Dapur Panas (192.168.10.202)"
        )

        val text = String(bytes)
        assertTrue(text.contains("*** PENGALIHAN PRINTER ***"))
        assertTrue(text.contains("DARI STASIUN: Dapur Panas (192.168.10.202)"))
        assertTrue(text.contains("STATUS ASLI: OFFLINE / KERTAS HABIS"))
        assertTrue(text.contains("*** TIKET DAPUR PANAS ***"))
        assertTrue(text.contains("Spaghetti Carbonara"))
    }

    @Test
    fun `test buildKitchenTicket formats Dine-In round number correctly`() {
        val router = KdsTicketRouter()
        val items = listOf(
            KdsTicketRouter.StationItem(
                name = "Butter Croissant",
                quantity = 2,
                prepStation = "cold_pastry"
            )
        )

        val bytes = router.buildKitchenTicket(
            stationTitle = "PASTRY & DINGIN",
            orderNumber = "ORD-20261108-99",
            tableOrQueue = "Meja T-05",
            items = items,
            deliveryMode = KdsTicketRouter.OrderDeliveryMode.DINE_IN,
            roundNumber = 2
        )

        val text = String(bytes)
        assertTrue(text.contains("RONDE PESANAN: RONDE #2"))
        assertTrue(text.contains("PASTRY & DINGIN"))
        assertTrue(text.contains("Butter Croissant"))
    }
}
