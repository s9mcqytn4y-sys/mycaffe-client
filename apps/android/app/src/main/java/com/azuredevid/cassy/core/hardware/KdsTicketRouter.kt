package com.azuredevid.cassy.core.hardware

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dynamic KDS Ticket Router supporting arbitrary preparation stations
 * (Barista, Hot Kitchen, Cold Kitchen/Pastry, Expediter/Runner) with automatic
 * socket failover and redirect watermarking.
 */
class KdsTicketRouter(
    private val printerManager: PrinterPort9100Manager = PrinterPort9100Manager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    enum class OrderDeliveryMode {
        DINE_IN,
        TAKEAWAY
    }

    data class StationPrinterConfig(
        val stationId: String,
        val displayName: String,
        val primaryIp: String,
        val fallbackIp: String? = null,
        val port: Int = 9100
    )

    data class StationItem(
        val name: String,
        val quantity: Int,
        val prepStation: String,
        val variant: String = "",
        val options: List<String> = emptyList(),
        val notes: String = ""
    )

    data class StationPrintStatus(
        val stationId: String,
        val stationName: String,
        val targetIp: String,
        val isPrinted: Boolean,
        val isRedirected: Boolean,
        val itemsCount: Int,
        val errorMessage: String? = null
    )

    data class RoutingResult(
        val stationStatuses: List<StationPrintStatus>,
        val cashierReceiptPrinted: Boolean,
        val errors: List<String> = emptyList(),
        val hasAnyFailover: Boolean = false
    ) {
        val allSucceeded: Boolean
            get() = errors.isEmpty() && stationStatuses.all { it.isPrinted }
    }

    /**
     * Builds ESC/POS bytes for a kitchen station ticket with optional round and failover headers.
     */
    fun buildKitchenTicket(
        stationTitle: String,
        orderNumber: String,
        tableOrQueue: String,
        items: List<StationItem>,
        deliveryMode: OrderDeliveryMode = OrderDeliveryMode.DINE_IN,
        roundNumber: Int = 1,
        isRedirected: Boolean = false,
        originalStation: String = "",
        paperWidth: EscPosReceiptBuilder.PaperWidth = EscPosReceiptBuilder.PaperWidth.WIDTH_80MM
    ): ByteArray {
        val b = EscPosReceiptBuilder(paperWidth)

        // Failover Watermark Header
        if (isRedirected && originalStation.isNotBlank()) {
            b.bold(true)
            b.title("*** PENGALIHAN PRINTER ***")
            b.text("DARI STASIUN: $originalStation")
            b.text("STATUS ASLI: OFFLINE / KERTAS HABIS")
            b.separator('!')
            b.bold(false)
        }

        b.bold(true)
        b.title("*** TIKET $stationTitle ***")
        b.bold(false)
        b.separator('=')
        b.row("No. Pesanan:", orderNumber)
        b.row("Tipe Layanan:", deliveryMode.name)
        b.row("Meja / Antrean:", tableOrQueue)
        if (deliveryMode == OrderDeliveryMode.DINE_IN && roundNumber > 1) {
            b.bold(true)
            b.text("RONDE PESANAN: RONDE #$roundNumber")
            b.bold(false)
        }
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
                b.bold(true)
                b.row("   * Catatan: ${itm.notes}", "")
                b.bold(false)
            }
            b.separator('-')
        }

        b.emptyLine(1)
        b.feedAndCut()
        return b.build()
    }

    /**
     * Routes and prints items to configured stations dynamically with automatic fallback redirection.
     */
    suspend fun routeOrderDynamic(
        orderNumber: String,
        tableOrQueue: String,
        items: List<StationItem>,
        stations: List<StationPrinterConfig>,
        cashierPrinterIp: String = "",
        cashierReceiptBytes: ByteArray? = null,
        deliveryMode: OrderDeliveryMode = OrderDeliveryMode.DINE_IN,
        roundNumber: Int = 1
    ): RoutingResult = withContext(ioDispatcher) {
        val errors = mutableListOf<String>()
        val statuses = mutableListOf<StationPrintStatus>()
        var anyFailover = false

        // Group items by normalized prepStation (e.g. "bar", "kitchen", "pastry", "runner")
        val itemsByStation = items.groupBy { it.prepStation.trim().lowercase() }
        val stationConfigMap = stations.associateBy { it.stationId.trim().lowercase() }

        // Find fallback station for unmapped items (or use cashier printer as safety net)
        val defaultSafetyStation = stationConfigMap["runner"] ?: stations.firstOrNull()

        for ((stationKey, stationItems) in itemsByStation) {
            val config = stationConfigMap[stationKey] ?: defaultSafetyStation
            val stationName = config?.displayName ?: stationKey.uppercase()

            if (config == null || config.primaryIp.isBlank()) {
                statuses.add(
                    StationPrintStatus(
                        stationId = stationKey,
                        stationName = stationName,
                        targetIp = "",
                        isPrinted = false,
                        isRedirected = false,
                        itemsCount = stationItems.size,
                        errorMessage = "Tidak ada konfigurasi IP printer untuk stasiun '$stationKey'"
                    )
                )
                errors.add("Printer stasiun '$stationKey' belum dikonfigurasi.")
                continue
            }

            // 1. Attempt print to Primary Printer
            val ticketBytes = buildKitchenTicket(
                stationTitle = config.displayName,
                orderNumber = orderNumber,
                tableOrQueue = tableOrQueue,
                items = stationItems,
                deliveryMode = deliveryMode,
                roundNumber = roundNumber,
                isRedirected = false
            )

            val primaryResult = printerManager.printRawBytes(config.primaryIp, ticketBytes, config.port)
            if (primaryResult.isSuccess) {
                statuses.add(
                    StationPrintStatus(
                        stationId = config.stationId,
                        stationName = config.displayName,
                        targetIp = config.primaryIp,
                        isPrinted = true,
                        isRedirected = false,
                        itemsCount = stationItems.size
                    )
                )
            } else {
                // 2. Primary Failed -> Attempt Fallback Redirection
                val fallbackIp = config.fallbackIp?.takeIf { it.isNotBlank() } ?: cashierPrinterIp.takeIf { it.isNotBlank() }
                if (!fallbackIp.isNullOrBlank() && fallbackIp != config.primaryIp) {
                    val redirectedBytes = buildKitchenTicket(
                        stationTitle = config.displayName,
                        orderNumber = orderNumber,
                        tableOrQueue = tableOrQueue,
                        items = stationItems,
                        deliveryMode = deliveryMode,
                        roundNumber = roundNumber,
                        isRedirected = true,
                        originalStation = "${config.displayName} (${config.primaryIp})"
                    )

                    val fallbackResult = printerManager.printRawBytes(fallbackIp, redirectedBytes, config.port)
                    if (fallbackResult.isSuccess) {
                        anyFailover = true
                        statuses.add(
                            StationPrintStatus(
                                stationId = config.stationId,
                                stationName = config.displayName,
                                targetIp = fallbackIp,
                                isPrinted = true,
                                isRedirected = true,
                                itemsCount = stationItems.size,
                                errorMessage = "Dialihkan ke $fallbackIp karena printer utama offline"
                            )
                        )
                    } else {
                        errors.add("Gagal cetak stasiun '${config.displayName}' di IP utama (${config.primaryIp}) dan fallback ($fallbackIp)")
                        statuses.add(
                            StationPrintStatus(
                                stationId = config.stationId,
                                stationName = config.displayName,
                                targetIp = config.primaryIp,
                                isPrinted = false,
                                isRedirected = false,
                                itemsCount = stationItems.size,
                                errorMessage = primaryResult.exceptionOrNull()?.message
                            )
                        )
                    }
                } else {
                    errors.add("Gagal cetak stasiun '${config.displayName}' (${config.primaryIp}): ${primaryResult.exceptionOrNull()?.message}")
                    statuses.add(
                        StationPrintStatus(
                            stationId = config.stationId,
                            stationName = config.displayName,
                            targetIp = config.primaryIp,
                            isPrinted = false,
                            isRedirected = false,
                            itemsCount = stationItems.size,
                            errorMessage = primaryResult.exceptionOrNull()?.message
                        )
                    )
                }
            }
        }

        // 3. Print Cashier Receipt if present
        var cashierSuccess = false
        if (cashierReceiptBytes != null && cashierPrinterIp.isNotBlank()) {
            printerManager.printRawBytes(cashierPrinterIp, cashierReceiptBytes)
                .onSuccess { cashierSuccess = true }
                .onFailure { errors.add("Gagal cetak struk kasir ($cashierPrinterIp): ${it.message}") }
        }

        RoutingResult(
            stationStatuses = statuses,
            cashierReceiptPrinted = cashierSuccess,
            errors = errors,
            hasAnyFailover = anyFailover
        )
    }
}
