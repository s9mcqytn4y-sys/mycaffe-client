package com.azuredevid.cassy.data.repository

import com.azuredevid.cassy.data.model.TableBoardEntry
import com.azuredevid.cassy.data.model.TableInput
import com.azuredevid.cassy.data.model.TableRecord
import com.azuredevid.cassy.data.model.TableSessionInfo
import com.azuredevid.cassy.data.network.HttpErrorMapper
import com.azuredevid.cassy.domain.repository.TableRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class KtorTableRepository(
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TableRepository {

    override suspend fun listTables(section: String?): Result<List<TableBoardEntry>> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.get("$baseUrl/api/v1/pos/tables") {
                if (!section.isNullOrBlank()) {
                    parameter("section", section.trim())
                }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<TableBoardResponse>().tables)
            } else {
                Result.failure(HttpErrorMapper.toDomainException(response, "Gagal memuat papan meja."))
            }
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal terhubung saat memuat papan meja."))
        }
    }

    override suspend fun createTable(input: TableInput): Result<TableRecord> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.post("$baseUrl/api/v1/pos/tables") {
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(HttpErrorMapper.toDomainException(response, "Gagal menambah meja."))
            }
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal menambah meja."))
        }
    }

    override suspend fun updateTable(tableId: String, input: TableInput): Result<TableRecord> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.patch("$baseUrl/api/v1/pos/tables/$tableId") {
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(HttpErrorMapper.toDomainException(response, "Gagal memperbarui meja."))
            }
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memperbarui meja."))
        }
    }

    override suspend fun archiveTable(tableId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.post("$baseUrl/api/v1/pos/tables/$tableId/archive")
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(HttpErrorMapper.toDomainException(response, "Gagal mengarsipkan meja."))
            }
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal mengarsipkan meja."))
        }
    }

    override suspend fun rotateQr(tableId: String): Result<TableRecord> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.post("$baseUrl/api/v1/pos/tables/$tableId/rotate-qr")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(HttpErrorMapper.toDomainException(response, "Gagal memutar QR meja."))
            }
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memutar QR meja."))
        }
    }

    override suspend fun openSession(tableId: String): Result<TableSessionInfo> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.post("$baseUrl/api/v1/pos/tables/$tableId/sessions")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(HttpErrorMapper.toDomainException(response, "Gagal membuka sesi meja."))
            }
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal membuka sesi meja."))
        }
    }

    override suspend fun closeSession(sessionId: String): Result<TableSessionInfo> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.post("$baseUrl/api/v1/pos/table-sessions/$sessionId/close")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(HttpErrorMapper.toDomainException(response, "Gagal menutup sesi meja."))
            }
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal menutup sesi meja."))
        }
    }

    @Serializable
    private data class TableBoardResponse(
        val tables: List<TableBoardEntry> = emptyList()
    )
}
