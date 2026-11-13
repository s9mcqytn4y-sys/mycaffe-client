package com.azuredevid.cassy.ui.table

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.core.security.DataStoreSessionManager
import com.azuredevid.cassy.data.model.TableBoardEntry
import com.azuredevid.cassy.data.model.TableInput
import com.azuredevid.cassy.data.model.TableRecord
import com.azuredevid.cassy.data.model.TableSessionInfo
import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoredSession
import com.azuredevid.cassy.domain.repository.TableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TableManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class InMemorySessionDataStore(
        initial: StoredSession = StoredSession()
    ) : DataStore<StoredSession> {
        private val state = MutableStateFlow(initial)
        private val mutex = Mutex()
        override val data: Flow<StoredSession> = state
        override suspend fun updateData(transform: suspend (t: StoredSession) -> StoredSession): StoredSession =
            mutex.withLock {
                val updated = transform(state.value)
                state.value = updated
                updated
            }
    }

    private class FakeServerSettings : ServerSettingsRepository {
        override fun getBaseUrl(): String = "https://demo.abbdullaziz.online"
        override fun setBaseUrl(url: String) = Unit
    }

    private class FakeTableRepository : TableRepository {
        var rotateQrCalls = 0
        var createTableCalls = 0
        val tables = mutableListOf(
            TableBoardEntry(
                id = "t-1",
                tableNumber = "01",
                label = "Meja 01",
                section = "Indoor",
                capacity = 4,
                qrToken = "tok-1",
                status = "available"
            ),
            TableBoardEntry(
                id = "t-2",
                tableNumber = "02",
                label = "Meja 02",
                section = "Outdoor",
                capacity = 2,
                qrToken = "tok-2",
                status = "occupied",
                session = TableSessionInfo("s-2", "t-2", "open")
            )
        )

        override suspend fun listTables(section: String?): Result<List<TableBoardEntry>> =
            Result.success(tables.toList())

        override suspend fun createTable(input: TableInput): Result<TableRecord> {
            createTableCalls++
            val record = TableRecord(
                id = "t-3",
                tableNumber = input.tableNumber.orEmpty(),
                label = input.label.orEmpty(),
                section = input.section.orEmpty(),
                capacity = input.capacity ?: 2,
                qrToken = "tok-3"
            )
            tables.add(
                TableBoardEntry(
                    id = record.id,
                    tableNumber = record.tableNumber,
                    label = record.label,
                    section = record.section,
                    capacity = record.capacity,
                    qrToken = record.qrToken
                )
            )
            return Result.success(record)
        }

        override suspend fun updateTable(tableId: String, input: TableInput): Result<TableRecord> =
            Result.failure(UnsupportedOperationException())

        override suspend fun archiveTable(tableId: String): Result<Unit> = Result.success(Unit)

        override suspend fun rotateQr(tableId: String): Result<TableRecord> {
            rotateQrCalls++
            return Result.success(TableRecord(tableId, "01", qrToken = "tok-new"))
        }

        override suspend fun openSession(tableId: String): Result<TableSessionInfo> =
            Result.success(TableSessionInfo("s-new", tableId, "open"))

        override suspend fun closeSession(sessionId: String): Result<TableSessionInfo> =
            Result.success(TableSessionInfo(sessionId, "t-2", "closed"))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cashierRole_cannotManageCrud_andFilterSectionWorks() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-cashier",
            accessTokenExpiresAt = "2026-11-14T00:00:00Z",
            sessionExpiresAt = "2026-11-15T00:00:00Z",
            staff = StaffIdentity("u1", "Kasir", "kasir@cassy.id", "cashier", "s1", "kopi", "Kopi")
        )
        val repo = FakeTableRepository()
        val vm = TableManagementViewModel(
            repo,
            sessionManager,
            FakeServerSettings(),
            PrinterPort9100Manager(testDispatcher),
            testDispatcher
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.canManageTables)
        assertEquals(listOf("Indoor", "Outdoor"), vm.uiState.value.sections)

        vm.selectSection("Outdoor")
        assertEquals(1, vm.uiState.value.displayedTables.size)
        assertEquals("t-2", vm.uiState.value.displayedTables.single().id)

        vm.saveTable(null, TableInput(tableNumber = "03"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, repo.createTableCalls)
    }

    @Test
    fun ownerRole_blocksRotateQrWhenOccupied_andAllowsWhenAvailable() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-owner",
            accessTokenExpiresAt = "2026-11-14T00:00:00Z",
            sessionExpiresAt = "2026-11-15T00:00:00Z",
            staff = StaffIdentity("u2", "Owner", "owner@cassy.id", "owner", "s1", "kopi", "Kopi")
        )
        val repo = FakeTableRepository()
        val vm = TableManagementViewModel(
            repo,
            sessionManager,
            FakeServerSettings(),
            PrinterPort9100Manager(testDispatcher),
            testDispatcher
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.canManageTables)
        val occupied = vm.uiState.value.tables.first { it.status == "occupied" }
        vm.rotateQr(occupied)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, repo.rotateQrCalls)

        val available = vm.uiState.value.tables.first { it.status == "available" }
        vm.rotateQr(available)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repo.rotateQrCalls)
    }
}
