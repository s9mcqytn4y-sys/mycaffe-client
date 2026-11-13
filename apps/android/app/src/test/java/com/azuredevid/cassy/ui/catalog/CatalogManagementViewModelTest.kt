package com.azuredevid.cassy.ui.catalog

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.core.security.DataStoreSessionManager
import com.azuredevid.cassy.data.model.CatalogCategory
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.CatalogSnapshot
import com.azuredevid.cassy.data.model.CatalogVariant
import com.azuredevid.cassy.data.model.ProductUpsertInput
import com.azuredevid.cassy.data.model.VariantUpsertInput
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoredSession
import com.azuredevid.cassy.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
class CatalogManagementViewModelTest {

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

    private class FakeCatalogRepository : CatalogRepository {
        var failAvailability = false
        var failVariant = false
        var createCategoryCalls = 0
        val categories = mutableListOf(CatalogCategory("cat-1", "Kopi", 1, true))
        val products = mutableListOf(
            CatalogProduct("prod-1", "Kopi Susu", "cat-1", "Kopi", "", 25000L, "", "bar", isAvailable = true)
        )

        override suspend fun getCatalog(storeSlug: String): Result<List<CatalogProduct>> = Result.success(products.toList())
        override suspend fun getStaffCatalog(): Result<CatalogSnapshot> =
            Result.success(CatalogSnapshot(categories.toList(), products.toList()))

        override suspend fun createCategory(name: String, displayOrder: Int, isActive: Boolean): Result<CatalogCategory> {
            createCategoryCalls++
            val created = CatalogCategory("cat-${categories.size + 1}", name, displayOrder, isActive)
            categories.add(created)
            return Result.success(created)
        }

        override suspend fun updateCategory(id: String, name: String, displayOrder: Int, isActive: Boolean): Result<CatalogCategory> =
            Result.failure(UnsupportedOperationException())

        override suspend fun deleteCategory(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun createProduct(input: ProductUpsertInput): Result<CatalogProduct> =
            Result.failure(UnsupportedOperationException())

        override suspend fun updateProduct(id: String, input: ProductUpsertInput): Result<CatalogProduct> =
            Result.failure(UnsupportedOperationException())

        override suspend fun archiveProduct(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun setProductAvailability(id: String, isAvailable: Boolean): Result<Unit> =
            if (failAvailability) Result.failure(IllegalStateException("Network error"))
            else Result.success(Unit)

        override suspend fun createVariant(productId: String, input: VariantUpsertInput): Result<CatalogVariant> =
            if (failVariant) Result.failure(IllegalStateException("Save variant failed"))
            else Result.success(CatalogVariant(id = "var-1", name = input.name, price = input.price, isDefault = input.isDefault))

        override suspend fun updateVariant(variantId: String, input: VariantUpsertInput): Result<CatalogVariant> =
            Result.failure(UnsupportedOperationException())

        override suspend fun archiveVariant(variantId: String): Result<Unit> = Result.success(Unit)
        override suspend fun uploadProductImage(productId: String, webpBytes: ByteArray): Result<String> =
            Result.success("https://cdn.example.com/p.jpg")
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
    fun cashierRole_cannotManageCatalogCrud_butCanToggleAvailabilityWithRollbackOnError() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-cashier",
            accessTokenExpiresAt = "2026-11-14T00:00:00Z",
            sessionExpiresAt = "2026-11-15T00:00:00Z",
            staff = StaffIdentity("u1", "Kasir", "kasir@cassy.id", "cashier", "s1", "kopi", "Kopi")
        )
        val repo = FakeCatalogRepository()
        val vm = CatalogManagementViewModel(repo, sessionManager, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.canManageCatalog)
        vm.saveCategory(null, "Pastry", 2, true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, repo.createCategoryCalls)

        // Optimistic toggle succeeds when server accepts
        vm.toggleProductAvailability(vm.uiState.value.products.single())
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.products.single().isAvailable)

        // Optimistic toggle rolls back when server fails
        repo.failAvailability = true
        vm.toggleProductAvailability(vm.uiState.value.products.single())
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.products.single().isAvailable)
    }

    @Test
    fun ownerRole_canManageCatalogAndCreateCategory() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-owner",
            accessTokenExpiresAt = "2026-11-14T00:00:00Z",
            sessionExpiresAt = "2026-11-15T00:00:00Z",
            staff = StaffIdentity("u2", "Owner", "owner@cassy.id", "owner", "s1", "kopi", "Kopi")
        )
        val repo = FakeCatalogRepository()
        val vm = CatalogManagementViewModel(repo, sessionManager, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.canManageCatalog)
        vm.saveCategory(null, "Pastry", 2, true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repo.createCategoryCalls)
        assertEquals(2, vm.uiState.value.categories.size)
    }

    @Test
    fun saveVariant_emitsVariantSavedOnlyOnSuccess() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-owner",
            accessTokenExpiresAt = "2026-11-14T00:00:00Z",
            sessionExpiresAt = "2026-11-15T00:00:00Z",
            staff = StaffIdentity("u2", "Owner", "owner@cassy.id", "owner", "s1", "kopi", "Kopi")
        )
        val repo = FakeCatalogRepository()
        val vm = CatalogManagementViewModel(repo, sessionManager, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val effects = mutableListOf<CatalogManagementUiEffect>()
        val collectJob = backgroundScope.launch {
            vm.effects.take(3).toList(effects)
        }

        repo.failVariant = true
        vm.saveVariant("prod-1", null, VariantUpsertInput(name = "Large", price = 30000L, isDefault = false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(effects.any { it is CatalogManagementUiEffect.VariantSaved })

        repo.failVariant = false
        vm.saveVariant("prod-1", null, VariantUpsertInput(name = "Large", price = 30000L, isDefault = false))
        testDispatcher.scheduler.advanceUntilIdle()
        collectJob.join()
        assertTrue(effects.any { it is CatalogManagementUiEffect.VariantSaved })
    }
}
