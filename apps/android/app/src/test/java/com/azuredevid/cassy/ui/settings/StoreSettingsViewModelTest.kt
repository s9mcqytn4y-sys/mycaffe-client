package com.azuredevid.cassy.ui.settings

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.core.security.DataStoreSessionManager
import com.azuredevid.cassy.domain.model.MidtransEnvironment
import com.azuredevid.cassy.domain.model.PaymentCredentialInput
import com.azuredevid.cassy.domain.model.PaymentCredentials
import com.azuredevid.cassy.domain.model.QRServiceMode
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoreCMSConfig
import com.azuredevid.cassy.domain.model.StoreCMSConfigInput
import com.azuredevid.cassy.domain.model.StoreSettings
import com.azuredevid.cassy.domain.model.StoreSettingsPatch
import com.azuredevid.cassy.domain.model.StoredSession
import com.azuredevid.cassy.domain.repository.PrinterConfig
import com.azuredevid.cassy.domain.repository.PrinterSettingsRepository
import com.azuredevid.cassy.domain.repository.StoreGovernanceRepository
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
class StoreSettingsViewModelTest {

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
        private val state = MutableStateFlow(initial)
        override val configFlow: Flow<PrinterConfig> = state
        override suspend fun getConfig(): PrinterConfig = state.value
        override suspend fun saveConfig(config: PrinterConfig) { state.value = config }
        override suspend fun clearConfig() { state.value = PrinterConfig() }
    }

    private class FakeStoreGovernanceRepository : StoreGovernanceRepository {
        var currentCreds = PaymentCredentials(
            environment = MidtransEnvironment.SANDBOX,
            merchantId = "G357841497",
            clientKey = "SB-Mid-client-12345678",
            serverKeyMasked = "SB-Mid-server-••••••••497",
            isEnabled = true,
            fallbackStaticQrisEnabled = true,
            validationStatus = "verified"
        )

        var currentCMS = StoreCMSConfig(
            defaultLocale = "id-ID",
            supportedLocales = listOf("id-ID", "en-US"),
            heroTitleId = "Judul Id Asli",
            heroTitleEn = "Original En Title",
            heroSubtitleId = "Subjudul Id",
            heroSubtitleEn = "Subtitle En",
            heroBannerUrl = "",
            primaryHex = "#1F5F52",
            accentHex = "#E9A23B",
            highContrastMode = false,
            hidePoweredByBadge = false,
            customDomain = ""
        )

        var currentSettings = StoreSettings(
            qrServiceMode = QRServiceMode.TABLE,
            qrOrderingPaused = false,
            tableTimeLimitMinutes = 90,
            tableTimerWarningMinutes = 15,
            staticQrisFallbackMode = "manual",
            cashierHandlesBar = false
        )

        var savedCredsInput: PaymentCredentialInput? = null
        var savedCMSInput: StoreCMSConfigInput? = null
        var patchedSettings: StoreSettingsPatch? = null

        override suspend fun getPaymentCredentials(): Result<PaymentCredentials> = Result.success(currentCreds)
        override suspend fun savePaymentCredentials(input: PaymentCredentialInput): Result<PaymentCredentials> {
            savedCredsInput = input
            return Result.success(currentCreds)
        }

        override suspend fun getStoreCMS(): Result<StoreCMSConfig> = Result.success(currentCMS)
        override suspend fun saveStoreCMS(input: StoreCMSConfigInput): Result<StoreCMSConfig> {
            savedCMSInput = input
            return Result.success(currentCMS)
        }

        override suspend fun getStoreSettings(): Result<StoreSettings> = Result.success(currentSettings)
        override suspend fun updateStoreSettings(patch: StoreSettingsPatch): Result<StoreSettings> {
            patchedSettings = patch
            return Result.success(currentSettings)
        }
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
    fun `new brand form has no colour so saving keeps the neutral fallback`() {
        val state = StoreSettingsUiState()
        assertEquals("", state.primaryHex)
        assertEquals("", state.accentHex)
    }

    @Test
    fun ownerSession_populatesUiStateAndAllowsLiveMidtransVerify() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-owner",
            accessTokenExpiresAt = "2026-11-14T21:00:00Z",
            sessionExpiresAt = "2026-11-15T21:00:00Z",
            staff = StaffIdentity("u1", "Owner", "owner@cassy.id", "owner", "s1", "cassy", "Cassy")
        )
        val repo = FakeStoreGovernanceRepository()
        val printerSettings = FakePrinterSettingsRepository()
        val vm = StoreSettingsViewModel(repo, sessionManager, printerSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.hasManageAccess)
        assertEquals("G357841497", state.merchantId)
        assertEquals("verified", state.validationStatus)
        assertEquals("Judul Id Asli", state.heroTitleId)
        assertEquals(QRServiceMode.TABLE, state.qrServiceMode)

        // Blank serverKey must be rejected before calling API
        vm.onEvent(StoreSettingsUiEvent.VerifyAndSaveMidtrans)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(repo.savedCredsInput)

        // Full serverKey triggers verify_live = true
        vm.onEvent(StoreSettingsUiEvent.UpdateServerKey("SB-Mid-server-secretkey123456"))
        vm.onEvent(StoreSettingsUiEvent.VerifyAndSaveMidtrans)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(repo.savedCredsInput)
        assertTrue(repo.savedCredsInput!!.verifyLive)
    }

    @Test
    fun cashierWithoutManagePermission_failsClosedOnGovernanceLoadsAndSaves() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-cashier",
            accessTokenExpiresAt = "2026-11-14T21:00:00Z",
            sessionExpiresAt = "2026-11-15T21:00:00Z",
            staff = StaffIdentity("u2", "Kasir", "kasir@cassy.id", "cashier", "s1", "cassy", "Cassy"),
            permissions = listOf("pos.order.create")
        )
        val repo = FakeStoreGovernanceRepository()
        val printerSettings = FakePrinterSettingsRepository()
        val vm = StoreSettingsViewModel(repo, sessionManager, printerSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.hasManageAccess)
        assertEquals("", vm.uiState.value.merchantId)

        // Attempt to save service settings is blocked
        vm.onEvent(StoreSettingsUiEvent.SaveStoreSettings)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(repo.patchedSettings)

        // Cashier can still configure local LAN printer
        vm.onEvent(StoreSettingsUiEvent.UpdateCashierPrinterIp("192.168.10.210"))
        vm.onEvent(StoreSettingsUiEvent.SavePrinterConfig)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("192.168.10.210", printerSettings.getConfig().cashierPrinterIp)
    }

    @Test
    fun serviceMode_and_storeCMS_updateAndSave() = runTest {
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "jwt-mgr",
            accessTokenExpiresAt = "2026-11-14T21:00:00Z",
            sessionExpiresAt = "2026-11-15T21:00:00Z",
            staff = StaffIdentity("u3", "Manager", "mgr@cassy.id", "manager", "s1", "cassy", "Cassy"),
            permissions = listOf("store.settings.manage")
        )
        val repo = FakeStoreGovernanceRepository()
        val printerSettings = FakePrinterSettingsRepository()
        val vm = StoreSettingsViewModel(repo, sessionManager, printerSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(StoreSettingsUiEvent.SetServiceMode(QRServiceMode.PICKUP))
        vm.onEvent(StoreSettingsUiEvent.ToggleEmergencyQrPause(true))
        vm.onEvent(StoreSettingsUiEvent.UpdateTableTimeLimit(120))
        vm.onEvent(StoreSettingsUiEvent.SaveStoreSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(QRServiceMode.PICKUP, repo.patchedSettings?.qrServiceMode)
        assertEquals(true, repo.patchedSettings?.qrOrderingPaused)
        assertEquals(120, repo.patchedSettings?.tableTimeLimitMinutes)

        vm.onEvent(StoreSettingsUiEvent.UpdateHeroTitleId("Kopi Mantap"))
        vm.onEvent(StoreSettingsUiEvent.UpdateHeroBannerUrl("https://cdn.cassy.id/banner.webp"))
        vm.onEvent(StoreSettingsUiEvent.ToggleWhiteLabel(true))
        vm.onEvent(StoreSettingsUiEvent.SaveStoreCms)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Kopi Mantap", repo.savedCMSInput?.heroTitleId)
        assertEquals("https://cdn.cassy.id/banner.webp", repo.savedCMSInput?.heroBannerUrl)
        assertTrue(repo.savedCMSInput?.hidePoweredByBadge == true)
    }
}
