package com.azuredevid.cassy.data.repository

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.azuredevid.cassy.domain.repository.PrinterConfig
import com.azuredevid.cassy.domain.repository.PrinterSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object PrinterConfigSerializer : Serializer<PrinterConfig> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: PrinterConfig = PrinterConfig()

    override suspend fun readFrom(input: InputStream): PrinterConfig {
        val raw = input.readBytes()
        if (raw.isEmpty()) return defaultValue
        return try {
            json.decodeFromString(PrinterConfig.serializer(), raw.decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Format konfigurasi printer tidak valid.", e)
        }
    }

    override suspend fun writeTo(t: PrinterConfig, output: OutputStream) {
        output.write(json.encodeToString(PrinterConfig.serializer(), t).encodeToByteArray())
    }
}

class DataStorePrinterSettingsRepository(
    private val dataStore: DataStore<PrinterConfig>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PrinterSettingsRepository {

    constructor(
        context: Context,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        dataStore = DataStoreFactory.create(
            serializer = PrinterConfigSerializer,
            scope = CoroutineScope(ioDispatcher + SupervisorJob())
        ) {
            context.applicationContext.dataStoreFile("cassy_printer_config.json")
        },
        ioDispatcher = ioDispatcher
    )

    override val configFlow: Flow<PrinterConfig> = dataStore.data.map { raw ->
        raw.copy(
            cashierPrinterPort = raw.cashierPrinterPort.takeIf { it in 1..65535 } ?: 9100,
            paperWidthMm = if (raw.paperWidthMm == 58) 58 else 80
        )
    }

    override suspend fun getConfig(): PrinterConfig = withContext(ioDispatcher) {
        configFlow.first()
    }

    override suspend fun saveConfig(config: PrinterConfig) {
        withContext(ioDispatcher) {
            dataStore.updateData {
                PrinterConfig(
                    cashierPrinterIp = config.cashierPrinterIp.trim(),
                    cashierPrinterPort = config.cashierPrinterPort.coerceIn(1, 65535),
                    barPrinterIp = config.barPrinterIp.trim(),
                    kitchenPrinterIp = config.kitchenPrinterIp.trim(),
                    pastryPrinterIp = config.pastryPrinterIp.trim(),
                    expediterPrinterIp = config.expediterPrinterIp.trim(),
                    paperWidthMm = if (config.paperWidthMm == 58) 58 else 80
                )
            }
        }
    }

    override suspend fun clearConfig() {
        withContext(ioDispatcher) {
            dataStore.updateData { PrinterConfig() }
        }
    }
}
