package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.data.model.CashDrawersResponseDto
import com.azuredevid.cassy.data.model.CashMovementRequestDto
import com.azuredevid.cassy.data.model.CloseShiftRequestDto
import com.azuredevid.cassy.data.model.CloseShiftResponseDto
import com.azuredevid.cassy.data.model.CreateCashMovementResponseDto
import com.azuredevid.cassy.data.model.OpenShiftRequestDto
import com.azuredevid.cassy.data.model.OpenShiftResponseDto
import com.azuredevid.cassy.data.model.ShiftDto
import com.azuredevid.cassy.data.model.XReportDto
import com.azuredevid.cassy.data.model.ZReportDto
import com.azuredevid.cassy.data.network.HttpErrorMapper
import com.azuredevid.cassy.domain.model.CashDrawer
import com.azuredevid.cassy.domain.model.CashMovementResult
import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.domain.model.ShiftCloseResult
import com.azuredevid.cassy.domain.model.ShiftOpenResult
import com.azuredevid.cassy.domain.model.XReport
import com.azuredevid.cassy.domain.model.ZReport
import com.azuredevid.cassy.domain.repository.ShiftRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KtorShiftRepository(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ShiftRepository {

    override suspend fun getDrawers(): Result<List<CashDrawer>> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val response = httpClient.get("$baseUrl/api/v1/pos/drawers") {
                contentType(ContentType.Application.Json)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal memuat daftar laci kasir."))
            }
            val payload = response.body<CashDrawersResponseDto>()
            Result.success(payload.drawers.map { it.toDomain() }.filter { it.isActive })
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat daftar laci kasir."))
        }
    }

    override suspend fun getCurrentShift(drawerId: String?): Result<Shift?> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val response = httpClient.get("$baseUrl/api/v1/pos/shifts/current") {
                contentType(ContentType.Application.Json)
                if (!drawerId.isNullOrBlank()) {
                    parameter("drawer_id", drawerId)
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
    ): Result<ShiftOpenResult> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val requestDto = OpenShiftRequestDto(
                drawerId = drawerId?.takeIf { it.isNotBlank() },
                openingFloat = openingFloat,
                notes = notes.ifBlank { null }
            )
            val response = httpClient.post("$baseUrl/api/v1/pos/shifts") {
                contentType(ContentType.Application.Json)
                setBody(requestDto)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal membuka shift."))
            }
            val wrapper = response.body<OpenShiftResponseDto>()
            Result.success(ShiftOpenResult(shift = wrapper.shift.toDomain(), openDrawer = wrapper.openDrawer))
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
    ): Result<CashMovementResult> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val requestDto = CashMovementRequestDto(
                shiftId = shiftId,
                drawerId = drawerId,
                movementType = movementType.value,
                amount = amount,
                reason = reason
            )
            val response = httpClient.post("$baseUrl/api/v1/pos/shifts/movements") {
                contentType(ContentType.Application.Json)
                setBody(requestDto)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal mencatat mutasi kas."))
            }
            val wrapper = response.body<CreateCashMovementResponseDto>()
            Result.success(CashMovementResult(movement = wrapper.movement.toDomain(), openDrawer = wrapper.openDrawer))
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
    ): Result<ShiftCloseResult> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val requestDto = CloseShiftRequestDto(
                countedCash = countedCash,
                varianceReason = varianceReason?.ifBlank { null },
                ownerPin = ownerPin?.ifBlank { null },
                notes = notes?.ifBlank { null }
            )
            val response = httpClient.post("$baseUrl/api/v1/pos/shifts/$shiftId/close") {
                contentType(ContentType.Application.Json)
                setBody(requestDto)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal menutup shift."))
            }
            val wrapper = response.body<CloseShiftResponseDto>()
            Result.success(ShiftCloseResult(report = wrapper.report.toDomain(), openDrawer = wrapper.openDrawer))
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal menutup shift."))
        }
    }

    override suspend fun getXReport(shiftId: String): Result<XReport> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val response = httpClient.get("$baseUrl/api/v1/pos/shifts/$shiftId/x-report") {
                contentType(ContentType.Application.Json)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(HttpErrorMapper.toDomainException(response, "Gagal memuat X-Report."))
            }
            Result.success(response.body<XReportDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat X-Report."))
        }
    }

    override suspend fun getZReport(shiftId: String): Result<ZReport> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val response = httpClient.get("$baseUrl/api/v1/pos/shifts/$shiftId/z-report") {
                contentType(ContentType.Application.Json)
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
