package com.azuredevid.cassy.ui.settings

import com.azuredevid.cassy.data.repository.ServerSettingsRepository
import com.azuredevid.cassy.domain.model.MidtransEnvironment
import com.azuredevid.cassy.domain.model.PaymentCredentialInput
import com.azuredevid.cassy.domain.model.PaymentCredentials
import com.azuredevid.cassy.domain.model.QRServiceMode
import com.azuredevid.cassy.domain.model.StoreCMSConfig
import com.azuredevid.cassy.domain.model.StoreCMSConfigInput
import com.azuredevid.cassy.domain.model.StoreSettings
import com.azuredevid.cassy.domain.model.StoreSettingsPatch
import com.azuredevid.cassy.domain.repository.StoreGovernanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private class FakeStoreGovernanceRepository : StoreGovernanceRepository {
        var currentCreds = PaymentCredentials(
            environment = MidtransEnvironment.SANDBOX,
            merchantId = "G357841497",
            clientKey = "SB-Mid-client-12345678",
            serverKeyMasked = "SB-Mid-server-••••••••497",
            isEnabled = true,
            fallbackStaticQrisEnabled = true
        )

        var currentCMS = StoreCMSConfig(
            defaultLocale = "id-ID",
            supportedLocales = listOf("id-ID", "en-US"),
            heroTitleId = "Judul Id Asli",
            heroTitleEn = "Original En Title",
            heroSubtitleId = "Subjudul Id",
            heroSubtitleEn = "Subtitle En",
            heroBannerUrl = "",
            primaryHex = "#2C1A12",
            accentHex = "#C68B59",
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

        override suspend fun uploadHeroBanner(imageBytes: ByteArray): Result<String> = Result.success("https://cdn.cassy.pos/banner_new.webp")
        override suspend fun probeMidtransApi(serverKey: String, environment: String): Result<Boolean> = Result.success(true)
    }

    private class FakeServerSettings : ServerSettingsRepository {
        override fun getBaseUrl(): String = "http://10.0.2.2:8080"
        override fun setBaseUrl(url: String) {}
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
    fun initialLoad_populatesUiStateCorrectly() = runTest {
        val repo = FakeStoreGovernanceRepository()
        val serverSettings = FakeServerSettings()
        val vm = StoreSettingsViewModel(repo, serverSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("G357841497", state.merchantId)
        assertEquals("SB-Mid-client-12345678", state.clientKey)
        assertEquals("Judul Id Asli", state.heroTitleId)
        assertEquals(QRServiceMode.TABLE, state.qrServiceMode)
        assertEquals(90, state.tableTimeLimitMinutes)
        assertTrue(state.isSupervisorAuthenticated)
    }

    @Test
    fun midtransTab_neverUsesLocalPinBypass() = runTest {
        val repo = FakeStoreGovernanceRepository()
        val serverSettings = FakeServerSettings()
        val vm = StoreSettingsViewModel(repo, serverSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(StoreSettingsUiEvent.SelectTab(0))
        assertFalse(vm.uiState.value.showSupervisorPinModal)
        assertEquals(0, vm.uiState.value.selectedTabIndex)
    }

    @Test
    fun serviceMode_switchAndSave() = runTest {
        val repo = FakeStoreGovernanceRepository()
        val serverSettings = FakeServerSettings()
        val vm = StoreSettingsViewModel(repo, serverSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Switch to PICKUP mode
        vm.onEvent(StoreSettingsUiEvent.SetServiceMode(QRServiceMode.PICKUP))
        assertEquals(QRServiceMode.PICKUP, vm.uiState.value.qrServiceMode)

        // Toggle Emergency Pause
        vm.onEvent(StoreSettingsUiEvent.ToggleEmergencyQrPause(true))
        assertTrue(vm.uiState.value.qrOrderingPaused)

        // Change Table Limit
        vm.onEvent(StoreSettingsUiEvent.UpdateTableTimeLimit(120))
        assertEquals(120, vm.uiState.value.tableTimeLimitMinutes)

        // Save
        vm.onEvent(StoreSettingsUiEvent.SaveStoreSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        val patch = repo.patchedSettings
        assertNotNull(patch)
        assertEquals(QRServiceMode.PICKUP, patch?.qrServiceMode)
        assertEquals(true, patch?.qrOrderingPaused)
        assertEquals(120, patch?.tableTimeLimitMinutes)
    }

    @Test
    fun storeCMS_updateAndSave() = runTest {
        val repo = FakeStoreGovernanceRepository()
        val serverSettings = FakeServerSettings()
        val vm = StoreSettingsViewModel(repo, serverSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(StoreSettingsUiEvent.UpdateHeroTitleId("Kopi Mantap"))
        vm.onEvent(StoreSettingsUiEvent.ToggleWhiteLabel(true))
        vm.onEvent(StoreSettingsUiEvent.SaveStoreCms)
        testDispatcher.scheduler.advanceUntilIdle()

        val input = repo.savedCMSInput
        assertNotNull(input)
        assertEquals("Kopi Mantap", input?.heroTitleId)
        assertTrue(input?.hidePoweredByBadge == true)
    }
}

