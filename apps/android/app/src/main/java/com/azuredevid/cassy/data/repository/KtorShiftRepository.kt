package com.azuredevid.cassy.data.repository

import android.content.Context
import com.azuredevid.cassy.data.model.*
import com.azuredevid.cassy.domain.model.CashMovement
import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.domain.model.ZReport
import com.azuredevid.cassy.domain.repository.ShiftRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class KtorShiftRepository(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val authTokenProvider: () -> String? = { null },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ShiftRepository {

    constructor(
        context: Context,
        serverSettings: ServerSettingsRepository = SharedPreferencesServerSettingsRepository(context),
        authTokenProvider: () -> String? = { KtorPaymentCredentialRepository(context).getStoredAuthToken() },
        customHttpClient: HttpClient? = null
    ) : this(
        httpClient = customHttpClient ?: HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        },
        serverSettings = serverSettings,
        authTokenProvider = authTokenProvider
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun getCurrentShift(): Result<Shift?> = withContext(ioDispatcher) {
        runCatching {
            val token = authTokenProvider()
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val response = httpClient.get("$baseUrl/api/v1/pos/shifts/current") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            when {
                response.status.isSuccess() -> {
                    val dto = response.body<ShiftDto>()
                    dto.toDomain()
                }
                response.status == HttpStatusCode.Conflict || response.status == HttpStatusCode.NotFound -> {
                    // Tidak ada shift aktif yang terbuka
                    null
                }
                else -> {
                    val errorBody = response.bodyAsText()
                    throw Exception("Gagal memuat status shift: HTTP ${response.status.value} - $errorBody")
                }
            }
        }
    }

    override suspend fun openShift(
        openingFloat: Long,
        drawerId: String?,
        notes: String
    ): Result<Shift> = withContext(ioDispatcher) {
        runCatching {
            val token = authTokenProvider()
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
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
                val errorBody = response.bodyAsText()
                throw Exception("Gagal membuka shift: HTTP ${response.status.value} - $errorBody")
            }

            val dto = response.body<ShiftDto>()
            dto.toDomain()
        }
    }

    override suspend fun recordCashMovement(
        movementType: CashMovementType,
        amount: Long,
        reason: String,
        shiftId: String?,
        drawerId: String?
    ): Result<CashMovement> = withContext(ioDispatcher) {
        runCatching {
            val token = authTokenProvider()
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
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                setBody(requestDto)
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                throw Exception("Gagal mencatat mutasi kas: HTTP ${response.status.value} - $errorBody")
            }

            val dto = response.body<CashMovementDto>()
            dto.toDomain()
        }
    }

    override suspend fun closeShift(
        shiftId: String,
        countedCash: Long,
        ownerPin: String?,
        varianceReason: String?,
        notes: String?
    ): Result<ZReport> = withContext(ioDispatcher) {
        runCatching {
            val token = authTokenProvider()
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
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
                val errorBody = response.bodyAsText()
                throw Exception("Gagal menutup shift: HTTP ${response.status.value} - $errorBody")
            }

            val dto = response.body<ZReportDto>()
            dto.toDomain()
        }
    }

    override suspend fun getZReport(shiftId: String): Result<ZReport> = withContext(ioDispatcher) {
        runCatching {
            val token = authTokenProvider()
            val baseUrl = serverSettings.getBaseUrl().trimEnd('/')
            val response = httpClient.get("$baseUrl/api/v1/pos/shifts/$shiftId/z-report") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                throw Exception("Gagal memuat Z-Report: HTTP ${response.status.value} - $errorBody")
            }

            val dto = response.body<ZReportDto>()
            dto.toDomain()
        }
    }
}
