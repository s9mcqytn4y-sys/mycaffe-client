package com.azuredevid.cassy.ui.shift

import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import com.azuredevid.cassy.domain.model.*
import com.azuredevid.cassy.domain.repository.ShiftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeShiftRepository : ShiftRepository {
        var currentShiftMock: Shift? = null
        var shouldFail: Boolean = false
        var lastOpenedFloat: Long = 0L
        var lastMovement: CashMovement? = null
        var lastCloseCountedCash: Long = 0L

        override suspend fun getCurrentShift(): Result<Shift?> {
            if (shouldFail) return Result.failure(Exception("Network error"))
            return Result.success(currentShiftMock)
        }

        override suspend fun openShift(
            openingFloat: Long,
            drawerId: String?,
            notes: String
        ): Result<Shift> {
            if (shouldFail) return Result.failure(Exception("Gagal buka shift"))
            lastOpenedFloat = openingFloat
            val newShift = Shift(
                id = "shift-new-123",
                storeId = "store-1",
                drawerId = drawerId ?: "drawer-1",
                openedBy = "staff-1",
                openedByName = "Kasir Test",
                openingFloat = openingFloat,
                openedAt = "2026-11-13T08:00:00Z",
                status = ShiftStatus.OPEN,
                expectedCash = openingFloat,
                notes = notes
            )
            currentShiftMock = newShift
            return Result.success(newShift)
        }

        override suspend fun recordCashMovement(
            movementType: CashMovementType,
            amount: Long,
            reason: String,
            shiftId: String?,
            drawerId: String?
        ): Result<CashMovement> {
            if (shouldFail) return Result.failure(Exception("Gagal catat mutasi"))
            val movement = CashMovement(
                id = "mov-1",
                storeId = "store-1",
                shiftId = shiftId ?: "shift-new-123",
                movementType = movementType,
                amount = amount,
                actorId = "staff-1",
                reason = reason,
                createdAt = "2026-11-13T09:00:00Z"
            )
            lastMovement = movement
            return Result.success(movement)
        }

        override suspend fun closeShift(
            shiftId: String,
            countedCash: Long,
            ownerPin: String?,
            varianceReason: String?,
            notes: String?
        ): Result<ZReport> {
            if (shouldFail) return Result.failure(Exception("Gagal tutup shift"))
            lastCloseCountedCash = countedCash
            val report = ZReport(
                shiftId = shiftId,
                storeId = "store-1",
                drawerId = "drawer-1",
                openedBy = "staff-1",
                openedAt = "2026-11-13T08:00:00Z",
                closedAt = "2026-11-13T16:00:00Z",
                status = ShiftStatus.CLOSED,
                expectedCash = 500000L,
                countedCash = countedCash,
                variance = countedCash - 500000L,
                varianceReason = varianceReason,
                notes = notes
            )
            currentShiftMock = null
            return Result.success(report)
        }

        override suspend fun getZReport(shiftId: String): Result<ZReport> {
            return Result.success(
                ZReport(
                    shiftId = shiftId,
                    storeId = "store-1",
                    drawerId = "drawer-1",
                    openedBy = "staff-1",
                    openedAt = "2026-11-13T08:00:00Z",
                    status = ShiftStatus.CLOSED
                )
            )
        }
    }

    private class FakeServerSettings(private var pin: String = "1234") : ServerSettingsRepository {
        override fun isSupervisorPinRequired(): Boolean = true
        override fun verifySupervisorPin(pin: String): Boolean = pin.trim() == this.pin
        override fun setSupervisorPin(pin: String) { this.pin = pin }
        override fun getBaseUrl(): String = "http://10.0.2.2:8080"
        override fun setBaseUrl(url: String) {}
    }

    private lateinit var shiftRepository: FakeShiftRepository
    private lateinit var serverSettings: FakeServerSettings

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        shiftRepository = FakeShiftRepository()
        serverSettings = FakeServerSettings()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_whenShiftActive_setsShiftOpen() = runTest {
        shiftRepository.currentShiftMock = Shift(
            id = "shift-001",
            storeId = "store-1",
            drawerId = "drawer-1",
            openedBy = "staff-1",
            openedByName = "Budi Kasir",
            openingFloat = 300000L,
            openedAt = "2026-11-13T07:00:00Z",
            status = ShiftStatus.OPEN,
            expectedCash = 450000L
        )

        val viewModel = ShiftViewModel(shiftRepository, serverSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isShiftOpen)
        assertNotNull(state.currentShift)
        assertEquals("shift-001", state.currentShift?.id)
        assertEquals(450000L, state.currentShift?.expectedCash)
    }

    @Test
    fun initialLoad_whenNoActiveShift_setsShiftClosed() = runTest {
        shiftRepository.currentShiftMock = null

        val viewModel = ShiftViewModel(shiftRepository, serverSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isShiftOpen)
        assertNull(state.currentShift)
    }

    @Test
    fun submitOpenShift_success_opensShiftAndUpdatesState() = runTest {
        shiftRepository.currentShiftMock = null
        val viewModel = ShiftViewModel(shiftRepository, serverSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openShiftModal()
        viewModel.onOpeningFloatChanged("500000")
        viewModel.onOpenShiftNotesChanged("Shift Siang")

        viewModel.submitOpenShift()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isShiftOpen)
        assertFalse(state.showOpenShiftModal)
        assertEquals(500000L, shiftRepository.lastOpenedFloat)
        assertEquals("shift-new-123", state.currentShift?.id)
    }

    @Test
    fun recordCashMovement_validInput_submitsAndReloads() = runTest {
        shiftRepository.currentShiftMock = Shift(
            id = "shift-001",
            storeId = "store-1",
            drawerId = "drawer-1",
            openedBy = "staff-1",
            openingFloat = 200000L,
            openedAt = "2026-11-13T07:00:00Z",
            status = ShiftStatus.OPEN,
            expectedCash = 200000L
        )
        val viewModel = ShiftViewModel(shiftRepository, serverSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openCashMovementModal()
        viewModel.onMovementTypeChanged(CashMovementType.PAID_OUT)
        viewModel.onMovementAmountChanged("25000")
        viewModel.onMovementReasonChanged("Beli es batu")

        viewModel.submitCashMovement()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showCashMovementModal)
        assertNotNull(shiftRepository.lastMovement)
        assertEquals(25000L, shiftRepository.lastMovement?.amount)
        assertEquals(CashMovementType.PAID_OUT, shiftRepository.lastMovement?.movementType)
    }

    @Test
    fun closeShift_blindCashCount_varianceCalculation() = runTest {
        shiftRepository.currentShiftMock = Shift(
            id = "shift-001",
            storeId = "store-1",
            drawerId = "drawer-1",
            openedBy = "staff-1",
            openingFloat = 500000L,
            openedAt = "2026-11-13T07:00:00Z",
            status = ShiftStatus.OPEN,
            expectedCash = 500000L
        )
        val viewModel = ShiftViewModel(shiftRepository, serverSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openCloseShiftModal()

        // 1. Enter exact amount: variance = 0
        viewModel.onCountedCashChanged("500000")
        assertEquals(0L, viewModel.uiState.value.calculatedVariance)
        assertFalse(viewModel.uiState.value.isSupervisorAuthRequired)

        // 2. Small variance within Rp 50.000 (e.g. Rp 20.000): does not require supervisor
        viewModel.onCountedCashChanged("520000")
        assertEquals(20000L, viewModel.uiState.value.calculatedVariance)
        assertFalse(viewModel.uiState.value.isSupervisorAuthRequired)

        // 3. Large variance exceeding Rp 50.000 (e.g. short Rp 60.000): requires supervisor PIN & reason
        viewModel.onCountedCashChanged("440000")
        assertEquals(-60000L, viewModel.uiState.value.calculatedVariance)
        assertTrue(viewModel.uiState.value.isSupervisorAuthRequired)
    }

    @Test
    fun closeShift_whenVarianceExceedsTolerance_requiresValidSupervisorPin() = runTest {
        shiftRepository.currentShiftMock = Shift(
            id = "shift-001",
            storeId = "store-1",
            drawerId = "drawer-1",
            openedBy = "staff-1",
            openingFloat = 500000L,
            openedAt = "2026-11-13T07:00:00Z",
            status = ShiftStatus.OPEN,
            expectedCash = 500000L
        )
        val viewModel = ShiftViewModel(shiftRepository, serverSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openCloseShiftModal()
        viewModel.onCountedCashChanged("400000") // Short by 100k
        assertTrue(viewModel.uiState.value.isSupervisorAuthRequired)

        // Attempt submit without reason: blocked
        viewModel.submitCloseShift()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Alasan wajib diisi"))

        // Add reason but wrong PIN: blocked
        viewModel.onVarianceReasonChanged("Uang kembalian tercecer")
        viewModel.onSupervisorPinChanged("9999")
        viewModel.submitCloseShift()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("PIN Supervisor tidak valid"))

        // Add correct PIN: succeeds
        viewModel.onSupervisorPinChanged("1234")
        viewModel.submitCloseShift()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isShiftOpen)
        assertFalse(state.showCloseShiftModal)
        assertNotNull(state.lastZReport)
        assertEquals(400000L, shiftRepository.lastCloseCountedCash)
    }
}
