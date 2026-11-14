package com.azuredevid.cassy.domain.repository

import com.azuredevid.cassy.domain.model.CashDrawer
import com.azuredevid.cassy.domain.model.CashMovementResult
import com.azuredevid.cassy.domain.model.CashMovementType
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.domain.model.ShiftCloseResult
import com.azuredevid.cassy.domain.model.ShiftOpenResult
import com.azuredevid.cassy.domain.model.XReport
import com.azuredevid.cassy.domain.model.ZReport

interface ShiftRepository {
    suspend fun getDrawers(): Result<List<CashDrawer>>
    suspend fun getCurrentShift(drawerId: String? = null): Result<Shift?>
    suspend fun openShift(
        openingFloat: Long,
        drawerId: String? = null,
        notes: String = ""
    ): Result<ShiftOpenResult>

    suspend fun recordCashMovement(
        movementType: CashMovementType,
        amount: Long,
        reason: String,
        shiftId: String? = null,
        drawerId: String? = null
    ): Result<CashMovementResult>

    suspend fun closeShift(
        shiftId: String,
        countedCash: Long,
        ownerPin: String? = null,
        varianceReason: String? = null,
        notes: String? = null
    ): Result<ShiftCloseResult>

    suspend fun getXReport(shiftId: String): Result<XReport>
    suspend fun getZReport(shiftId: String): Result<ZReport>
}
