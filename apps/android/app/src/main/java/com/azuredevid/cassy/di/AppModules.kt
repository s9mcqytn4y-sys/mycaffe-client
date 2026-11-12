package com.azuredevid.cassy.di

import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.core.security.BiometricSecurityManager
import com.azuredevid.cassy.data.repository.*
import com.azuredevid.cassy.domain.repository.*
import com.azuredevid.cassy.ui.settings.StoreSettingsViewModel
import com.azuredevid.cassy.ui.shift.ShiftViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<PrinterPort9100Manager> { PrinterPort9100Manager() }
    single<BiometricSecurityManager> { BiometricSecurityManager(androidContext()) }
    single<HttpClient> {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }
    }
    single<ServerSettingsRepository> { SharedPreferencesServerSettingsRepository(androidContext()) }
}

val repositoryModule = module {
    single<PaymentCredentialRepository> {
        KtorPaymentCredentialRepository(androidContext(), customHttpClient = get())
    }
    single<StoreGovernanceRepository> {
        val credRepo = get<PaymentCredentialRepository>() as KtorPaymentCredentialRepository
        KtorStoreGovernanceRepository(
            httpClient = get(),
            serverSettings = get(),
            authTokenProvider = { credRepo.getStoredAuthToken() }
        )
    }
    single<CatalogRepository> {
        KtorCatalogRepository(androidContext(), httpClient = get())
    }
    single<OrderQuoteRepository> {
        val credRepo = get<PaymentCredentialRepository>() as KtorPaymentCredentialRepository
        val serverSettings = get<ServerSettingsRepository>()
        KtorOrderQuoteRepository(
            httpClient = get(),
            apiBaseUrl = serverSettings.getBaseUrl(),
            staffToken = { credRepo.getStoredAuthToken() }
        )
    }
    single<ShiftRepository> {
        val credRepo = get<PaymentCredentialRepository>() as KtorPaymentCredentialRepository
        KtorShiftRepository(
            httpClient = get(),
            serverSettings = get(),
            authTokenProvider = { credRepo.getStoredAuthToken() }
        )
    }
}

val viewModelModule = module {
    viewModel { StoreSettingsViewModel(get(), get()) }
    viewModel { ShiftViewModel(get(), get(), get()) }
}
