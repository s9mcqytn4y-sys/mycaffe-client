package com.azuredevid.cassy.ui.shift

import com.azuredevid.cassy.domain.error.DomainError
import com.azuredevid.cassy.domain.error.DomainException
import com.azuredevid.cassy.domain.model.CashDrawer
import com.azuredevid.cassy.domain.model.CashMovement
import com.azuredevid.cassy.domain.model.CashMovementResult
import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.domain.model.ShiftCloseResult
import com.azuredevid.cassy.domain.model.ShiftOpenResult
import com.azuredevid.cassy.domain.model.ShiftStatus
import com.azuredevid.cassy.domain.model.XReport
import com.azuredevid.cassy.domain.model.ZReport
import com.azuredevid.cassy.domain.repository.PrinterConfig
import com.azuredevid.cassy.domain.repository.PrinterSettingsRepository
import com.azuredevid.cassy.domain.repository.ShiftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeShiftRepository : ShiftRepository {
        var drawersMock: List<CashDrawer> = listOf(
            CashDrawer(id = "drawer-1", storeId = "store-1", name = "Laci Utama", isActive = true)
        )
        var currentShiftMock: Shift? = null
        var shouldFail: Boolean = false
        var failCloseWithVarianceOverride: Boolean = false
        var lastOpenedFloat: Long = 0L
        var lastMovement: CashMovement? = null
        var lastCloseCountedCash: Long = 0L

        override suspend fun getDrawers(): Result<List<CashDrawer>> =
            if (shouldFail) Result.failure(Exception("Network error")) else Result.success(drawersMock)

        override suspend fun getCurrentShift(drawerId: String?): Result<Shift?> {
            if (shouldFail) return Result.failure(Exception("Network error"))
            return Result.success(currentShiftMock)
        }

        override suspend fun openShift(
            openingFloat: Long,
            drawerId: String?,
            notes: String
        ): Result<ShiftOpenResult> {
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
            return Result.success(ShiftOpenResult(shift = newShift, openDrawer = true))
        }

        override suspend fun recordCashMovement(
            movementType: CashMovementType,
            amount: Long,
            reason: String,
            shiftId: String?,
            drawerId: String?
        ): Result<CashMovementResult> {
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
            return Result.success(CashMovementResult(movement = movement, openDrawer = false))
        }

        override suspend fun closeShift(
            shiftId: String,
            countedCash: Long,
            ownerPin: String?,
            varianceReason: String?,
            notes: String?
        ): Result<ShiftCloseResult> {
            if (failCloseWithVarianceOverride && ownerPin.isNullOrBlank()) {
                return Result.failure(
                    DomainException(
                        DomainError.Validation(
                            message = "Selisih kas melebihi toleransi. Masukkan alasan dan PIN owner.",
                            code = "VARIANCE_EXCEEDS_TOLERANCE"
                        )
                    )
                )
            }
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
            return Result.success(ShiftCloseResult(report = report, openDrawer = true))
        }

        override suspend fun getXReport(shiftId: String): Result<XReport> = Result.success(
            XReport(
                shiftId = shiftId,
                storeId = "store-1",
                drawerId = "drawer-1",
                openedBy = "staff-1",
                openedAt = "2026-11-13T08:00:00Z",
                generatedAt = "2026-11-13T12:00:00Z",
                expectedRedacted = true,
                expectedCash = null
            )
        )

        override suspend fun getZReport(shiftId: String): Result<ZReport> = Result.success(
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

    private class FakePrinterSettingsRepository(
        initial: PrinterConfig = PrinterConfig()
    ) : PrinterSettingsRepository {
        private val flow = MutableStateFlow(initial)
        override val configFlow: Flow<PrinterConfig> = flow
        override suspend fun getConfig(): PrinterConfig = flow.value
        override suspend fun saveConfig(config: PrinterConfig) { flow.value = config }
        override suspend fun clearConfig() { flow.value = PrinterConfig() }
    }

    private lateinit var shiftRepository: FakeShiftRepository
    private lateinit var printerSettings: FakePrinterSettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        shiftRepository = FakeShiftRepository()
        printerSettings = FakePrinterSettingsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_whenShiftActive_setsShiftOpenAndDrawers() = runTest {
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

        val viewModel = ShiftViewModel(shiftRepository, printerSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isShiftOpen)
        assertEquals(1, state.drawers.size)
        assertEquals("drawer-1", state.selectedDrawerId)
        assertEquals("shift-001", state.currentShift?.id)
        assertEquals(450000L, state.currentShift?.expectedCash)
    }

    @Test
    fun initialLoad_whenNoActiveShift_setsShiftClosed() = runTest {
        shiftRepository.currentShiftMock = null

        val viewModel = ShiftViewModel(shiftRepository, printerSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isShiftOpen)
        assertNull(state.currentShift)
    }

    @Test
    fun submitOpenShift_success_opensShiftAndUpdatesState() = runTest {
        shiftRepository.currentShiftMock = null
        val viewModel = ShiftViewModel(shiftRepository, printerSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openShiftModal()
        testDispatcher.scheduler.advanceUntilIdle()
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
        val viewModel = ShiftViewModel(shiftRepository, printerSettings)
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
    fun closeShift_whenBlindCountRedacted_reliesOnServerVarianceOverride() = runTest {
        shiftRepository.currentShiftMock = Shift(
            id = "shift-001",
            storeId = "store-1",
            drawerId = "drawer-1",
            openedBy = "staff-1",
            openingFloat = 500000L,
            openedAt = "2026-11-13T07:00:00Z",
            status = ShiftStatus.OPEN,
            expectedCash = null
        )
        shiftRepository.failCloseWithVarianceOverride = true

        val viewModel = ShiftViewModel(shiftRepository, printerSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openCloseShiftModal()
        viewModel.onCountedCashChanged("350000")

        // Blind count: variance cannot be computed client-side when expectedCash is redacted
        assertNull(viewModel.uiState.value.calculatedVariance)
        assertFalse(viewModel.uiState.value.isSupervisorAuthRequired)

        // Server rejects close with VARIANCE_EXCEEDS_TOLERANCE -> VM opens override inputs
        viewModel.submitCloseShift()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSupervisorAuthRequired)

        // Supply reason + 6-digit PIN and retry -> succeeds
        viewModel.onVarianceReasonChanged("Kembalian kurang")
        viewModel.onSupervisorPinChanged("482913")
        viewModel.submitCloseShift()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShiftOpen)
        assertFalse(viewModel.uiState.value.showCloseShiftModal)
        assertNotNull(viewModel.uiState.value.lastZReport)
    }

    @Test
    fun closeShift_whenOwnerExpectedVisible_calculatesVarianceAndRequiresValidPin() = runTest {
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
        val viewModel = ShiftViewModel(shiftRepository, printerSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openCloseShiftModal()
        viewModel.onCountedCashChanged("400000")
        assertEquals(-100000L, viewModel.uiState.value.calculatedVariance)
        assertTrue(viewModel.uiState.value.isSupervisorAuthRequired)

        viewModel.submitCloseShift()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onVarianceReasonChanged("Uang kembalian tercecer")
        viewModel.onSupervisorPinChanged("482913")
        viewModel.submitCloseShift()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShiftOpen)
        assertNotNull(viewModel.uiState.value.lastZReport)
        assertEquals(400000L, shiftRepository.lastCloseCountedCash)
    }

    @Test
    fun openXReportModal_loadsRedactedReport() = runTest {
        shiftRepository.currentShiftMock = Shift(
            id = "shift-001",
            storeId = "store-1",
            drawerId = "drawer-1",
            openedBy = "staff-1",
            openingFloat = 500000L,
            openedAt = "2026-11-13T07:00:00Z",
            status = ShiftStatus.OPEN
        )
        val viewModel = ShiftViewModel(shiftRepository, printerSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openXReportModal()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showXReportModal)
        assertNotNull(viewModel.uiState.value.currentXReport)
        assertTrue(viewModel.uiState.value.currentXReport!!.expectedRedacted)
    }
}
