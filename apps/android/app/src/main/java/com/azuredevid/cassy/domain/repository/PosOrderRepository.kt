package com.azuredevid.cassy.domain.repository

import com.azuredevid.cassy.domain.model.CreatePosOrderInput
import com.azuredevid.cassy.domain.model.InitiatePosPaymentInput
import com.azuredevid.cassy.domain.model.InitiatePosPaymentResult
import com.azuredevid.cassy.domain.model.PosOrder
import com.azuredevid.cassy.domain.model.PosOrderDetail

interface PosOrderRepository {
    suspend fun createOrder(input: CreatePosOrderInput): Result<PosOrder>
    suspend fun initiatePayment(input: InitiatePosPaymentInput): Result<InitiatePosPaymentResult>
    suspend fun getOrder(orderId: String): Result<PosOrderDetail>
}
