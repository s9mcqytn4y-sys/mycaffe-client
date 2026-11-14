package com.azuredevid.cassy.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class PrinterConfig(
    val cashierPrinterIp: String = "",
    val cashierPrinterPort: Int = 9100,
    val barPrinterIp: String = "",
    val kitchenPrinterIp: String = "",
    val pastryPrinterIp: String = "",
    val expediterPrinterIp: String = "",
    val paperWidthMm: Int = 80
) {
    val hasCashierPrinter: Boolean
        get() = cashierPrinterIp.isNotBlank()
}

interface PrinterSettingsRepository {
    val configFlow: Flow<PrinterConfig>
    suspend fun getConfig(): PrinterConfig
    suspend fun saveConfig(config: PrinterConfig)
    suspend fun clearConfig()
}
