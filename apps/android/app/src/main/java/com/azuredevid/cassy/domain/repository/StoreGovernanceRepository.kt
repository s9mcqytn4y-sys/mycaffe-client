package com.azuredevid.cassy.domain.repository

import com.azuredevid.cassy.domain.model.PaymentCredentialInput
import com.azuredevid.cassy.domain.model.PaymentCredentials
import com.azuredevid.cassy.domain.model.StoreCMSConfig
import com.azuredevid.cassy.domain.model.StoreCMSConfigInput
import com.azuredevid.cassy.domain.model.StoreSettings
import com.azuredevid.cassy.domain.model.StoreSettingsPatch

interface StoreGovernanceRepository {
    suspend fun getPaymentCredentials(): Result<PaymentCredentials>
    suspend fun savePaymentCredentials(input: PaymentCredentialInput): Result<PaymentCredentials>
    suspend fun getStoreCMS(): Result<StoreCMSConfig>
    suspend fun saveStoreCMS(input: StoreCMSConfigInput): Result<StoreCMSConfig>
    suspend fun getStoreSettings(): Result<StoreSettings>
    suspend fun updateStoreSettings(patch: StoreSettingsPatch): Result<StoreSettings>
}

