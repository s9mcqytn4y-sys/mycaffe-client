package com.azuredevid.cassy.domain.repository

import com.azuredevid.cassy.domain.model.CashMovement
import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.domain.model.ZReport

interface ShiftRepository {
    suspend fun getCurrentShift(): Result<Shift?>
    suspend fun openShift(openingFloat: Long, drawerId: String? = null, notes: String = ""): Result<Shift>
    suspend fun recordCashMovement(
        movementType: CashMovementType,
        amount: Long,
        reason: String,
        shiftId: String? = null,
        drawerId: String? = null
    ): Result<CashMovement>
    suspend fun closeShift(
        shiftId: String,
        countedCash: Long,
        ownerPin: String? = null,
        varianceReason: String? = null,
        notes: String? = null
    ): Result<ZReport>
    suspend fun getZReport(shiftId: String): Result<ZReport>
}
