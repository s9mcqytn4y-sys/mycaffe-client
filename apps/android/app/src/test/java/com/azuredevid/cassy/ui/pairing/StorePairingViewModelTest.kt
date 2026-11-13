package com.azuredevid.cassy.ui.pairing

import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.repository.AuthLoginResult
import com.azuredevid.cassy.domain.repository.AuthRepository
import com.azuredevid.cassy.domain.repository.MobileBootstrapData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class StorePairingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthRepository : AuthRepository {
        var shouldFail = false
        var lastStoreSlug: String? = null
        var lastEmail: String? = null
        var lastPin: String? = null
        var lastDeviceCred: String? = null

        override suspend fun login(
            storeSlug: String,
            email: String,
            badge: String,
            pin: String,
            deviceCredential: String?
        ): Result<AuthLoginResult> {
            lastStoreSlug = storeSlug
            lastEmail = email
            lastPin = pin
            lastDeviceCred = deviceCredential
            if (shouldFail) return Result.failure(IllegalStateException("PIN salah"))
            return Result.success(
                AuthLoginResult(
                    staff = StaffIdentity("u1", "Budi", email, "owner", "s1", storeSlug, "Kopi Senja"),
                    pinChangeRequired = false,
                    offlinePinVerifier = null
                )
            )
        }

        override suspend fun fetchMobileBootstrap(): Result<MobileBootstrapData> =
            Result.success(MobileBootstrapData(storeId = "s1", storeSlug = lastStoreSlug.orEmpty(), storeName = "Kopi Senja"))

        override suspend fun fetchMe(): Result<StaffIdentity> =
            Result.failure(UnsupportedOperationException())

        override suspend fun logout(): Result<Unit> = Result.success(Unit)
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
    fun initialState_hasNoHardcodedStoreSlug() {
        val vm = StorePairingViewModel(FakeAuthRepository())
        assertEquals("", vm.uiState.value.storeSlug)
        assertEquals("", vm.uiState.value.email)
        assertEquals("", vm.uiState.value.pin)
    }

    @Test
    fun submitWithEmptyFields_setsInlineValidationErrors() {
        val vm = StorePairingViewModel(FakeAuthRepository())
        vm.onEvent(StorePairingUiEvent.SubmitLogin)
        assertNotNull(vm.uiState.value.storeSlugError)
        assertNotNull(vm.uiState.value.emailError)
        assertNotNull(vm.uiState.value.pinError)
    }

    @Test
    fun validLoginAndBootstrap_emitsPairingSucceededAndClearsPinFromState() = runTest {
        val repo = FakeAuthRepository()
        val vm = StorePairingViewModel(repo)

        vm.onEvent(StorePairingUiEvent.UpdateStoreSlug("Kopi-Senja"))
        vm.onEvent(StorePairingUiEvent.UpdateEmail("budi@kopi.id"))
        vm.onEvent(StorePairingUiEvent.UpdatePin("482913"))
        vm.onEvent(StorePairingUiEvent.SubmitLogin)

        val effect = vm.effects.first()
        assertTrue(effect is StorePairingUiEffect.PairingSucceeded)
        val succeeded = effect as StorePairingUiEffect.PairingSucceeded
        assertEquals("kopi-senja", succeeded.storeSlug)
        assertEquals("Kopi Senja", succeeded.storeName)
        assertEquals("482913", repo.lastPin)
        assertEquals("", vm.uiState.value.pin)
        assertNull(vm.uiState.value.formError)
    }

    @Test
    fun qrPayload_populatesStoreSlugEmailBadgeAndDeviceCredential() {
        val vm = StorePairingViewModel(FakeAuthRepository())
        val payload = """{"store_slug":"kopi-senja","email":"kasir@kopi.id","badge":"K01","pairing_token":"tok-123"}"""

        vm.onEvent(StorePairingUiEvent.HandleQrPayload(payload))

        val state = vm.uiState.value
        assertEquals("kopi-senja", state.storeSlug)
        assertEquals("kasir@kopi.id", state.email)
        assertEquals("K01", state.badge)
        assertEquals("tok-123", state.deviceCredential)
        assertNull(state.formError)
    }
}
