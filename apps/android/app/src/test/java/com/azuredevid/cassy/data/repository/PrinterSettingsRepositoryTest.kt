package com.azuredevid.cassy.data.repository

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.domain.repository.PrinterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class PrinterSettingsRepositoryTest {

    private class InMemoryPrinterDataStore(
        initial: PrinterConfig = PrinterConfig()
    ) : DataStore<PrinterConfig> {
        private val state = MutableStateFlow(initial)
        private val mutex = Mutex()
        override val data: Flow<PrinterConfig> = state
        override suspend fun updateData(transform: suspend (t: PrinterConfig) -> PrinterConfig): PrinterConfig =
            mutex.withLock {
                val updated = transform(state.value)
                state.value = updated
                updated
            }
    }

    @Test
    fun emptyDataStore_returnsUnconfiguredEmptyDefaults() = runTest {
        val repo = DataStorePrinterSettingsRepository(
            dataStore = InMemoryPrinterDataStore(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val config = repo.getConfig()
        assertEquals("", config.cashierPrinterIp)
        assertEquals(9100, config.cashierPrinterPort)
        assertFalse(config.hasCashierPrinter)
        assertEquals(80, config.paperWidthMm)
    }

    @Test
    fun saveAndClearConfig_persistsNormalizesAndResetsPrinterStations() = runTest {
        val repo = DataStorePrinterSettingsRepository(
            dataStore = InMemoryPrinterDataStore(),
            ioDispatcher = Dispatchers.Unconfined
        )

        repo.saveConfig(
            PrinterConfig(
                cashierPrinterIp = "  192.168.10.220  ",
                cashierPrinterPort = 9101,
                barPrinterIp = "192.168.10.221",
                kitchenPrinterIp = "192.168.10.222",
                pastryPrinterIp = "",
                expediterPrinterIp = "",
                paperWidthMm = 58
            )
        )

        val saved = repo.getConfig()
        assertTrue(saved.hasCashierPrinter)
        assertEquals("192.168.10.220", saved.cashierPrinterIp)
        assertEquals(9101, saved.cashierPrinterPort)
        assertEquals("192.168.10.221", saved.barPrinterIp)
        assertEquals("192.168.10.222", saved.kitchenPrinterIp)
        assertEquals(58, saved.paperWidthMm)

        repo.clearConfig()
        val cleared = repo.getConfig()
        assertFalse(cleared.hasCashierPrinter)
        assertEquals(80, cleared.paperWidthMm)
    }

    @Test
    fun serializer_roundTripsPrinterConfig() = runTest {
        val original = PrinterConfig(
            cashierPrinterIp = "192.168.10.220",
            cashierPrinterPort = 9100,
            barPrinterIp = "192.168.10.221",
            kitchenPrinterIp = "192.168.10.222",
            pastryPrinterIp = "192.168.10.223",
            expediterPrinterIp = "192.168.10.224",
            paperWidthMm = 58
        )
        val out = ByteArrayOutputStream()
        PrinterConfigSerializer.writeTo(original, out)
        val decoded = PrinterConfigSerializer.readFrom(ByteArrayInputStream(out.toByteArray()))
        assertEquals(original, decoded)
    }
}
