package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.data.model.CreateOrderItemInputDto
import com.azuredevid.cassy.data.model.CreateOrderRequestDto
import com.azuredevid.cassy.data.model.CreateOrderResponseDto
import com.azuredevid.cassy.data.model.InitiatePaymentRequestDto
import com.azuredevid.cassy.data.model.InitiatePaymentResponseDto
import com.azuredevid.cassy.data.model.OrderDetailResponseDto
import com.azuredevid.cassy.data.network.HttpErrorMapper
import com.azuredevid.cassy.domain.model.CreatePosOrderInput
import com.azuredevid.cassy.domain.model.InitiatePosPaymentInput
import com.azuredevid.cassy.domain.model.InitiatePosPaymentResult
import com.azuredevid.cassy.domain.model.PosOrder
import com.azuredevid.cassy.domain.model.PosOrderDetail
import com.azuredevid.cassy.domain.repository.PosOrderRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KtorPosOrderRepository(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PosOrderRepository {

    override suspend fun createOrder(input: CreatePosOrderInput): Result<PosOrder> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val dto = CreateOrderRequestDto(
                idempotencyKey = input.idempotencyKey,
                tableNumber = input.tableNumber?.takeIf { it.isNotBlank() },
                customerName = input.customerName.trim().ifBlank { "Pelanggan Walk-In" },
                customerPhone = input.customerPhone?.takeIf { it.isNotBlank() },
                orderType = input.orderType,
                billingMode = input.billingMode,
                expectedTotal = input.expectedTotal,
                expectedQuoteFingerprint = input.expectedQuoteFingerprint?.takeIf { it.isNotBlank() },
                notes = input.notes.takeIf { it.isNotBlank() },
                items = input.items.map {
                    CreateOrderItemInputDto(
                        productId = it.productId,
                        quantity = it.quantity,
                        notes = it.notes.takeIf { n -> n.isNotBlank() }
                    )
                }
            )
            val response = httpClient.post("$baseUrl/api/v1/pos/orders") {
                contentType(ContentType.Application.Json)
                setBody(dto)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal membuat pesanan ke server."))
            }
            Result.success(response.body<CreateOrderResponseDto>().order.toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal membuat pesanan ke server."))
        }
    }

    override suspend fun initiatePayment(input: InitiatePosPaymentInput): Result<InitiatePosPaymentResult> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val dto = InitiatePaymentRequestDto(
                idempotencyKey = input.idempotencyKey,
                splitId = input.splitId?.takeIf { it.isNotBlank() },
                method = input.method,
                cashTenderedAmount = input.cashTenderedAmount,
                drawerId = input.drawerId?.takeIf { it.isNotBlank() }
            )
            val response = httpClient.post("$baseUrl/api/v1/pos/orders/${input.orderId}/payments") {
                contentType(ContentType.Application.Json)
                setBody(dto)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal memproses pembayaran."))
            }
            Result.success(response.body<InitiatePaymentResponseDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memproses pembayaran."))
        }
    }

    override suspend fun getOrder(orderId: String): Result<PosOrderDetail> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val response = httpClient.get("$baseUrl/api/v1/pos/orders/$orderId") {
                contentType(ContentType.Application.Json)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal memeriksa status pesanan."))
            }
            Result.success(response.body<OrderDetailResponseDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memeriksa status pesanan."))
        }
    }
}
