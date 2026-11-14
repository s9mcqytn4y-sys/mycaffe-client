package com.azuredevid.cassy.ui.pos

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.core.security.DataStoreSessionManager
import com.azuredevid.cassy.data.model.CatalogCategory
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.CatalogSnapshot
import com.azuredevid.cassy.data.model.CatalogVariant
import com.azuredevid.cassy.data.model.OrderQuote
import com.azuredevid.cassy.data.model.OrderQuoteRequest
import com.azuredevid.cassy.data.model.ProductUpsertInput
import com.azuredevid.cassy.data.model.VariantUpsertInput
import com.azuredevid.cassy.domain.model.CreatePosOrderInput
import com.azuredevid.cassy.domain.model.InitiatePosPaymentInput
import com.azuredevid.cassy.domain.model.InitiatePosPaymentResult
import com.azuredevid.cassy.domain.model.PosOrder
import com.azuredevid.cassy.domain.model.PosOrderDetail
import com.azuredevid.cassy.domain.model.PosPaymentAttempt
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoredSession
import com.azuredevid.cassy.domain.repository.CatalogRepository
import com.azuredevid.cassy.domain.repository.NoStaffSessionException
import com.azuredevid.cassy.domain.repository.OrderQuoteRepository
import com.azuredevid.cassy.domain.repository.PosOrderRepository
import com.azuredevid.cassy.domain.repository.PrinterConfig
import com.azuredevid.cassy.domain.repository.PrinterSettingsRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelTest {

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

    private class FakePrinterSettingsRepository(
        initial: PrinterConfig = PrinterConfig()
    ) : PrinterSettingsRepository {
        private val flow = MutableStateFlow(initial)
        override val configFlow: Flow<PrinterConfig> = flow
        override suspend fun getConfig(): PrinterConfig = flow.value
        override suspend fun saveConfig(config: PrinterConfig) { flow.value = config }
        override suspend fun clearConfig() { flow.value = PrinterConfig() }
    }

    private val coffeeCategory = CatalogCategory("cat-1", "Kopi", 1, true)
    private val latte = CatalogProduct("p-1", "Kopi Susu", "cat-1", "Kopi", "", 28000L, "", "bar")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class StubCatalogRepository(
        private val onGetStaffCatalog: () -> Result<CatalogSnapshot>
    ) : CatalogRepository {
        override suspend fun getCatalog(storeSlug: String): Result<List<CatalogProduct>> =
            Result.failure(UnsupportedOperationException())
        override suspend fun getStaffCatalog(): Result<CatalogSnapshot> = onGetStaffCatalog()
        override suspend fun createCategory(name: String, displayOrder: Int, isActive: Boolean): Result<CatalogCategory> =
            Result.failure(UnsupportedOperationException())
        override suspend fun updateCategory(id: String, name: String, displayOrder: Int, isActive: Boolean): Result<CatalogCategory> =
            Result.failure(UnsupportedOperationException())
        override suspend fun deleteCategory(id: String): Result<Unit> = Result.failure(UnsupportedOperationException())
        override suspend fun createProduct(input: ProductUpsertInput): Result<CatalogProduct> =
            Result.failure(UnsupportedOperationException())
        override suspend fun updateProduct(id: String, input: ProductUpsertInput): Result<CatalogProduct> =
            Result.failure(UnsupportedOperationException())
        override suspend fun archiveProduct(id: String): Result<Unit> = Result.failure(UnsupportedOperationException())
        override suspend fun setProductAvailability(id: String, isAvailable: Boolean): Result<Unit> =
            Result.failure(UnsupportedOperationException())
        override suspend fun createVariant(productId: String, input: VariantUpsertInput): Result<CatalogVariant> =
            Result.failure(UnsupportedOperationException())
        override suspend fun updateVariant(variantId: String, input: VariantUpsertInput): Result<CatalogVariant> =
            Result.failure(UnsupportedOperationException())
        override suspend fun archiveVariant(variantId: String): Result<Unit> = Result.failure(UnsupportedOperationException())
        override suspend fun uploadProductImage(productId: String, webpBytes: ByteArray): Result<String> =
            Result.failure(UnsupportedOperationException())
    }

    private class FakePosOrderRepository : PosOrderRepository {
        var createdOrderInput: CreatePosOrderInput? = null
        var initiatedPaymentInput: InitiatePosPaymentInput? = null
        var nextPaymentResult: InitiatePosPaymentResult = InitiatePosPaymentResult(
            isIdempotentReplay = false,
            openDrawer = false,
            payment = PosPaymentAttempt(
                id = "pay-1",
                providerOrderId = "CASSY-001",
                method = "cash",
                status = "settled",
                amount = 30800L,
                cashTenderedAmount = 50000L,
                changeGivenAmount = 19200L
            ),
            order = PosOrder(
                id = "ord-1",
                orderNumber = "ORD-2026-001",
                status = "confirmed",
                paymentStatus = "paid",
                totalAmount = 30800L,
                paidAmount = 30800L
            )
        )
        var orderDetailToReturn: PosOrderDetail = PosOrderDetail(
            order = PosOrder(id = "ord-1", orderNumber = "ORD-2026-001", paymentStatus = "pending")
        )

        override suspend fun createOrder(input: CreatePosOrderInput): Result<PosOrder> {
            createdOrderInput = input
            return Result.success(
                PosOrder(
                    id = "ord-1",
                    orderNumber = "ORD-2026-001",
                    tableNumber = input.tableNumber.orEmpty(),
                    orderType = input.orderType,
                    paymentStatus = "unpaid",
                    totalAmount = input.expectedTotal ?: 30800L
                )
            )
        }

        override suspend fun initiatePayment(input: InitiatePosPaymentInput): Result<InitiatePosPaymentResult> {
            initiatedPaymentInput = input
            return Result.success(nextPaymentResult)
        }

        override suspend fun getOrder(orderId: String): Result<PosOrderDetail> =
            Result.success(orderDetailToReturn)
    }

    @Test
    fun withoutStaffSession_failsClosedAndDoesNotLoadCatalog() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        var catalogCalled = false
        val catalogRepo = StubCatalogRepository {
            catalogCalled = true
            Result.success(CatalogSnapshot(listOf(coffeeCategory), listOf(latte)))
        }
        val quoteRepo = object : OrderQuoteRepository {
            override suspend fun quote(request: OrderQuoteRequest): Result<OrderQuote> =
                Result.failure(NoStaffSessionException())
        }

        val vm = PosViewModel(
            catalogRepo,
            quoteRepo,
            FakePosOrderRepository(),
            FakePrinterSettingsRepository(),
            sessionManager
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(catalogCalled)
        assertNotNull(vm.uiState.value.catalogError)
        assertTrue(vm.uiState.value.allProducts.isEmpty())
    }

    @Test
    fun addProductToCart_requestsServerQuoteAndSupportsEditableTableAndCashSettlement() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-1",
            accessTokenExpiresAt = "2026-11-13T21:00:00Z",
            sessionExpiresAt = "2026-11-14T21:00:00Z",
            staff = StaffIdentity("u1", "Kasir Budi", "budi@kopi.id", "cashier", "s1", "kopi-senja", "Kopi Senja")
        )
        val catalogRepo = StubCatalogRepository {
            Result.success(CatalogSnapshot(listOf(coffeeCategory), listOf(latte)))
        }
        val quoteRepo = object : OrderQuoteRepository {
            override suspend fun quote(request: OrderQuoteRequest): Result<OrderQuote> {
                val qty = request.items.single().quantity
                return Result.success(
                    OrderQuote(
                        subtotalAmount = 28000L * qty,
                        taxAmount = 2800L * qty,
                        totalAmount = 30800L * qty
                    )
                )
            }
        }
        val orderRepo = FakePosOrderRepository()

        val vm = PosViewModel(
            catalogRepo,
            quoteRepo,
            orderRepo,
            FakePrinterSettingsRepository(),
            sessionManager
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Kopi Senja", vm.uiState.value.storeName)
        assertEquals("Kasir Budi", vm.uiState.value.cashierName)
        assertEquals(1, vm.uiState.value.categories.size)
        assertEquals("", vm.uiState.value.tableName)

        vm.updateTableName("Meja 07")
        assertEquals("Meja 07", vm.uiState.value.tableName)

        vm.addProductToCart(latte)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.uiState.value.cart.single().quantity)
        assertEquals(30800L, vm.uiState.value.quote?.totalAmount)

        vm.submitCashPayment(tenderedAmount = 50000L, drawerId = "drawer-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Meja 07", orderRepo.createdOrderInput?.tableNumber)
        assertEquals("cash", orderRepo.initiatedPaymentInput?.method)
        assertEquals(50000L, orderRepo.initiatedPaymentInput?.cashTenderedAmount)
        assertTrue(vm.uiState.value.cart.isEmpty())
        assertNull(vm.uiState.value.quote)
    }

    @Test
    fun qrisCheckout_initiatesQrisModalWithoutFakeComplete() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-1",
            accessTokenExpiresAt = "2026-11-13T21:00:00Z",
            sessionExpiresAt = "2026-11-14T21:00:00Z",
            staff = StaffIdentity("u1", "Kasir Budi", "budi@kopi.id", "cashier", "s1", "kopi-senja", "Kopi Senja")
        )
        val catalogRepo = StubCatalogRepository {
            Result.success(CatalogSnapshot(listOf(coffeeCategory), listOf(latte)))
        }
        val quoteRepo = object : OrderQuoteRepository {
            override suspend fun quote(request: OrderQuoteRequest): Result<OrderQuote> = Result.success(
                OrderQuote(subtotalAmount = 28000L, taxAmount = 2800L, totalAmount = 30800L)
            )
        }
        val orderRepo = FakePosOrderRepository().apply {
            nextPaymentResult = InitiatePosPaymentResult(
                payment = PosPaymentAttempt(
                    id = "pay-qris",
                    providerOrderId = "CASSY-QRIS-1",
                    method = "qris",
                    status = "pending",
                    amount = 30800L,
                    qrString = "000201010212..."
                ),
                order = PosOrder(
                    id = "ord-1",
                    orderNumber = "ORD-QRIS-1",
                    paymentStatus = "pending",
                    totalAmount = 30800L
                )
            )
        }

        val vm = PosViewModel(
            catalogRepo,
            quoteRepo,
            orderRepo,
            FakePrinterSettingsRepository(),
            sessionManager
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addProductToCart(latte)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.startQrisPayment("drawer-1")
        testDispatcher.scheduler.runCurrent()

        assertNotNull(vm.uiState.value.activeQrisPayment)
        assertEquals("000201010212...", vm.uiState.value.activeQrisPayment?.qrString)
        assertTrue(vm.uiState.value.isCartLocked)

        // Manual refresh checks backend getOrder: if still pending, cart remains intact
        vm.refreshQrisStatus()
        testDispatcher.scheduler.runCurrent()
        assertNotNull(vm.uiState.value.activeQrisPayment)

        // When backend reports settled/paid, settlement completes and clears cart
        orderRepo.orderDetailToReturn = PosOrderDetail(
            order = PosOrder(id = "ord-1", orderNumber = "ORD-QRIS-1", paymentStatus = "paid", totalAmount = 30800L)
        )
        vm.refreshQrisStatus()
        testDispatcher.scheduler.runCurrent()
        assertNull(vm.uiState.value.activeQrisPayment)
        assertTrue(vm.uiState.value.cart.isEmpty())
    }
}
