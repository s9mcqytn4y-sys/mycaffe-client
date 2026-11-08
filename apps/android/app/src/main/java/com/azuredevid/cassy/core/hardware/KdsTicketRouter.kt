package com.azuredevid.cassy.core.hardware

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * KdsTicketRouter dispatches orders to multiple LAN thermal printers (Port 9100)
 * based on the prep station classification of each item (BAR vs KITCHEN).
 */
class KdsTicketRouter(
    private val printerManager: PrinterPort9100Manager = PrinterPort9100Manager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    data class PrinterEndpoints(
        val cashierPrinterIp: String = "192.168.10.200",
        val barPrinterIp: String = "192.168.10.201",
        val kitchenPrinterIp: String = "192.168.10.202",
        val port: Int = 9100
    )

    data class RoutingResult(
        val barItemsCount: Int,
        val kitchenItemsCount: Int,
        val barPrinted: Boolean,
        val kitchenPrinted: Boolean,
        val cashierReceiptPrinted: Boolean,
        val errors: List<String> = emptyList()
    )

    /**
     * Builds a kitchen ticket payload for a specific preparation station.
     */
    fun buildKitchenTicket(
        stationTitle: String,
        orderNumber: String,
        tableOrQueue: String,
        items: List<StationItem>,
        paperWidth: EscPosReceiptBuilder.PaperWidth = EscPosReceiptBuilder.PaperWidth.WIDTH_80MM
    ): ByteArray {
        val b = EscPosReceiptBuilder(paperWidth)
        b.bold(true)
        b.title("*** TIKET $stationTitle ***")
        b.bold(false)
        b.separator('=')
        b.row("No. Pesanan:", orderNumber)
        b.row("Meja / Antrean:", tableOrQueue)
        b.doubleSeparator()

        items.forEach { itm ->
            b.alignLeft()
            b.bold(true)
            b.row("${itm.quantity}x ${itm.name}", "")
            b.bold(false)
            if (itm.variant.isNotBlank()) {
                b.row("   Varian: ${itm.variant}", "")
            }
            itm.options.forEach { opt ->
                b.row("   + $opt", "")
            }
            if (itm.notes.isNotBlank()) {
                b.row("   * Catatan: ${itm.notes}", "")
            }
            b.separator('-')
        }

        b.emptyLine(1)
        b.feedAndCut()
        return b.build()
    }

    data class StationItem(
        val name: String,
        val quantity: Int,
        val prepStation: String,
        val variant: String = "",
        val options: List<String> = emptyList(),
        val notes: String = ""
    )

    /**
     * Routes and prints tickets to Bar and Kitchen printers concurrently.
     */
    suspend fun routeOrder(
        orderNumber: String,
        tableOrQueue: String,
        items: List<StationItem>,
        endpoints: PrinterEndpoints,
        cashierReceiptBytes: ByteArray? = null
    ): RoutingResult = withContext(ioDispatcher) {
        val errors = mutableListOf<String>()

        val barItems = items.filter { it.prepStation.equals("bar", ignoreCase = true) }
        val kitchenItems = items.filter { it.prepStation.equals("kitchen", ignoreCase = true) }

        var barSuccess = false
        if (barItems.isNotEmpty() && endpoints.barPrinterIp.isNotBlank()) {
            val barBytes = buildKitchenTicket("BAR / BARISTA", orderNumber, tableOrQueue, barItems)
            printerManager.printRawBytes(endpoints.barPrinterIp, barBytes, endpoints.port)
                .onSuccess { barSuccess = true }
                .onFailure { errors.add("Gagal cetak tiket Bar (${endpoints.barPrinterIp}): ${it.message}") }
        }

        var kitchenSuccess = false
        if (kitchenItems.isNotEmpty() && endpoints.kitchenPrinterIp.isNotBlank()) {
            val kitchenBytes = buildKitchenTicket("DAPUR PANAS", orderNumber, tableOrQueue, kitchenItems)
            printerManager.printRawBytes(endpoints.kitchenPrinterIp, kitchenBytes, endpoints.port)
                .onSuccess { kitchenSuccess = true }
                .onFailure { errors.add("Gagal cetak tiket Dapur (${endpoints.kitchenPrinterIp}): ${it.message}") }
        }

        var cashierSuccess = false
        if (cashierReceiptBytes != null && endpoints.cashierPrinterIp.isNotBlank()) {
            printerManager.printRawBytes(endpoints.cashierPrinterIp, cashierReceiptBytes, endpoints.port)
                .onSuccess { cashierSuccess = true }
                .onFailure { errors.add("Gagal cetak struk Kasir (${endpoints.cashierPrinterIp}): ${it.message}") }
        }

        RoutingResult(
            barItemsCount = barItems.size,
            kitchenItemsCount = kitchenItems.size,
            barPrinted = barSuccess,
            kitchenPrinted = kitchenSuccess,
            cashierReceiptPrinted = cashierSuccess,
            errors = errors
        )
    }
}

