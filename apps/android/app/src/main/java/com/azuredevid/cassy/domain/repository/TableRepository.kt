package com.azuredevid.cassy.domain.repository

import com.azuredevid.cassy.data.model.TableBoardEntry
import com.azuredevid.cassy.data.model.TableInput
import com.azuredevid.cassy.data.model.TableRecord
import com.azuredevid.cassy.data.model.TableSessionInfo

interface TableRepository {
    suspend fun listTables(section: String? = null): Result<List<TableBoardEntry>>
    suspend fun createTable(input: TableInput): Result<TableRecord>
    suspend fun updateTable(tableId: String, input: TableInput): Result<TableRecord>
    suspend fun archiveTable(tableId: String): Result<Unit>
    suspend fun rotateQr(tableId: String): Result<TableRecord>
    suspend fun openSession(tableId: String): Result<TableSessionInfo>
    suspend fun closeSession(sessionId: String): Result<TableSessionInfo>
}
