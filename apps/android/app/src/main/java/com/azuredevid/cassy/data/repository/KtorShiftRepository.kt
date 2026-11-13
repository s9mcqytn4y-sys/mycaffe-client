package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.data.model.CashMovementDto
import com.azuredevid.cassy.data.model.CashMovementRequestDto
import com.azuredevid.cassy.data.model.CloseShiftRequestDto
import com.azuredevid.cassy.data.model.OpenShiftRequestDto
import com.azuredevid.cassy.data.model.ShiftDto
import com.azuredevid.cassy.data.model.ZReportDto
import com.azuredevid.cassy.data.network.HttpErrorMapper
import com.azuredevid.cassy.domain.model.CashMovement
import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.domain.model.ZReport
import com.azuredevid.cassy.domain.repository.ShiftRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KtorShiftRepository(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val authTokenProvider: () -> String? = { null },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ShiftRepository {

    override suspend fun getCurrentShift(): Result<Shift?> = withContext(ioDispatcher) {
        try {
            val token = authTokenProvider()
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.get("$baseUrl/api/v1/pos/shifts/current") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            when {
                response.status.isSuccess() -> Result.success(response.body<ShiftDto>().toDomain())
                response.status == HttpStatusCode.Conflict || response.status == HttpStatusCode.NotFound -> Result.success(null)
                else -> Result.failure(HttpErrorMapper.toDomainException(response, "Gagal memuat status shift."))
            }
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat status shift."))
        }
    }

    override suspend fun openShift(
        openingFloat: Long,
        drawerId: String?,
        notes: String
    ): Result<Shift> = withContext(ioDispatcher) {
        try {
            val token = authTokenProvider()
            val baseUrl = serverSettings.getBaseUrl()
            val requestDto = OpenShiftRequestDto(
                drawerId = drawerId,
                openingFloat = openingFloat,
                notes = notes.ifBlank { null }
            )
            val response = httpClient.post("$baseUrl/api/v1/pos/shifts") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                setBody(requestDto)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal membuka shift."))
            }
            Result.success(response.body<ShiftDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal membuka shift."))
        }
    }

    override suspend fun recordCashMovement(
        movementType: CashMovementType,
        amount: Long,
        reason: String,
        shiftId: String?,
        drawerId: String?
    ): Result<CashMovement> = withContext(ioDispatcher) {
        try {
            val token = authTokenProvider()
            val baseUrl = serverSettings.getBaseUrl()
            val requestDto = CashMovementRequestDto(
                shiftId = shiftId,
                drawerId = drawerId,
                movementType = movementType.value,
                amount = amount,
                reason = reason
            )
            val response = httpClient.post("$baseUrl/api/v1/pos/shifts/movements") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                setBody(requestDto)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal mencatat mutasi kas."))
            }
            Result.success(response.body<CashMovementDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal mencatat mutasi kas."))
        }
    }

    override suspend fun closeShift(
        shiftId: String,
        countedCash: Long,
        ownerPin: String?,
        varianceReason: String?,
        notes: String?
    ): Result<ZReport> = withContext(ioDispatcher) {
        try {
            val token = authTokenProvider()
            val baseUrl = serverSettings.getBaseUrl()
            val requestDto = CloseShiftRequestDto(
                countedCash = countedCash,
                varianceReason = varianceReason?.ifBlank { null },
                ownerPin = ownerPin?.ifBlank { null },
                notes = notes?.ifBlank { null }
            )
            val response = httpClient.post("$baseUrl/api/v1/pos/shifts/$shiftId/close") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                setBody(requestDto)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal menutup shift."))
            }
            Result.success(response.body<ZReportDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal menutup shift."))
        }
    }

    override suspend fun getZReport(shiftId: String): Result<ZReport> = withContext(ioDispatcher) {
        try {
            val token = authTokenProvider()
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.get("$baseUrl/api/v1/pos/shifts/$shiftId/z-report") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal memuat Z-Report."))
            }
            Result.success(response.body<ZReportDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat Z-Report."))
        }
    }
}
