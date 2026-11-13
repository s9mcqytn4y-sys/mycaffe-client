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
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoredSession
import com.azuredevid.cassy.domain.repository.CatalogRepository
import com.azuredevid.cassy.domain.repository.NoStaffSessionException
import com.azuredevid.cassy.domain.repository.OrderQuoteRepository
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

    private val latte = CatalogProduct("p-1", "Kopi Susu", "signature", "Signature", "", 28000L, "", "bar")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class StubCatalogRepository(
        private val onGetCatalog: (String) -> Result<List<CatalogProduct>>
    ) : CatalogRepository {
        override suspend fun getCatalog(storeSlug: String): Result<List<CatalogProduct>> = onGetCatalog(storeSlug)
        override suspend fun getStaffCatalog(): Result<CatalogSnapshot> = Result.failure(UnsupportedOperationException())
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

    @Test
    fun withoutStaffSession_failsClosedAndDoesNotLoadCatalog() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        var catalogCalled = false
        val catalogRepo = StubCatalogRepository {
            catalogCalled = true
            Result.success(listOf(latte))
        }
        val quoteRepo = object : OrderQuoteRepository {
            override suspend fun quote(request: OrderQuoteRequest): Result<OrderQuote> =
                Result.failure(NoStaffSessionException())
        }

        val vm = PosViewModel(catalogRepo, quoteRepo, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!catalogCalled)
        assertNotNull(vm.uiState.value.catalogError)
        assertTrue(vm.uiState.value.allProducts.isEmpty())
    }

    @Test
    fun addProductToCart_requestsServerQuoteAndStoresImmutableCart() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-1",
            accessTokenExpiresAt = "2026-11-13T21:00:00Z",
            sessionExpiresAt = "2026-11-14T21:00:00Z",
            staff = StaffIdentity("u1", "Kasir Budi", "budi@kopi.id", "cashier", "s1", "kopi-senja", "Kopi Senja")
        )
        val catalogRepo = StubCatalogRepository { storeSlug ->
            assertEquals("kopi-senja", storeSlug)
            Result.success(listOf(latte))
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

        val vm = PosViewModel(catalogRepo, quoteRepo, sessionManager)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Kopi Senja", vm.uiState.value.storeName)
        assertEquals("Kasir Budi", vm.uiState.value.cashierName)
        assertEquals(1, vm.uiState.value.allProducts.size)

        vm.addProductToCart(latte)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.uiState.value.cart.single().quantity)
        assertEquals(30800L, vm.uiState.value.quote?.totalAmount)

        vm.increaseItemQuantity(latte.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, vm.uiState.value.cart.single().quantity)
        assertEquals(61600L, vm.uiState.value.quote?.totalAmount)

        vm.clearCartAfterPayment()
        assertTrue(vm.uiState.value.cart.isEmpty())
        assertNull(vm.uiState.value.quote)
    }
}
