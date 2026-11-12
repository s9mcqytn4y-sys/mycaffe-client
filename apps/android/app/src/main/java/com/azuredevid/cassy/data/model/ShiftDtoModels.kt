package com.azuredevid.cassy.data.model

import com.azuredevid.cassy.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenShiftRequestDto(
    @SerialName("drawer_id")
    val drawerId: String? = null,
    @SerialName("opening_float")
    val openingFloat: Long? = null,
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class ShiftDto(
    @SerialName("id")
    val id: String,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("drawer_id")
    val drawerId: String,
    @SerialName("opened_by")
    val openedBy: String,
    @SerialName("opening_float")
    val openingFloat: Long = 0L,
    @SerialName("opened_at")
    val openedAt: String,
    @SerialName("closed_by")
    val closedBy: String? = null,
    @SerialName("closed_at")
    val closedAt: String? = null,
    @SerialName("status")
    val status: String,
    @SerialName("expected_cash")
    val expectedCash: Long? = null,
    @SerialName("counted_cash")
    val countedCash: Long? = null,
    @SerialName("variance")
    val variance: Long? = null,
    @SerialName("variance_reason")
    val varianceReason: String? = null,
    @SerialName("notes")
    val notes: String? = null
) {
    fun toDomain(): Shift {
        return Shift(
            id = id,
            storeId = storeId,
            drawerId = drawerId,
            openedBy = openedBy,
            openingFloat = openingFloat,
            openedAt = openedAt,
            closedBy = closedBy,
            closedAt = closedAt,
            status = ShiftStatus.fromString(status),
            expectedCash = expectedCash,
            countedCash = countedCash,
            variance = variance,
            varianceReason = varianceReason,
            notes = notes
        )
    }
}

@Serializable
data class CashMovementRequestDto(
    @SerialName("shift_id")
    val shiftId: String? = null,
    @SerialName("drawer_id")
    val drawerId: String? = null,
    @SerialName("movement_type")
    val movementType: String,
    @SerialName("amount")
    val amount: Long,
    @SerialName("reason")
    val reason: String
)

@Serializable
data class CashMovementDto(
    @SerialName("id")
    val id: String,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("shift_id")
    val shiftId: String,
    @SerialName("movement_type")
    val movementType: String,
    @SerialName("amount")
    val amount: Long,
    @SerialName("actor_id")
    val actorId: String,
    @SerialName("reason")
    val reason: String? = null,
    @SerialName("created_at")
    val createdAt: String
) {
    fun toDomain(): CashMovement {
        return CashMovement(
            id = id,
            storeId = storeId,
            shiftId = shiftId,
            movementType = CashMovementType.fromString(movementType),
            amount = amount,
            actorId = actorId,
            reason = reason,
            createdAt = createdAt
        )
    }
}

@Serializable
data class CloseShiftRequestDto(
    @SerialName("counted_cash")
    val countedCash: Long,
    @SerialName("variance_reason")
    val varianceReason: String? = null,
    @SerialName("owner_pin")
    val ownerPin: String? = null,
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class ShiftCashSummaryDto(
    @SerialName("opening_float")
    val openingFloat: Long = 0L,
    @SerialName("cash_sales")
    val cashSales: Long = 0L,
    @SerialName("cash_refunds")
    val cashRefunds: Long = 0L,
    @SerialName("refund_count")
    val refundCount: Int = 0,
    @SerialName("paid_outs")
    val paidOuts: Long = 0L,
    @SerialName("cash_ins")
    val cashIns: Long = 0L,
    @SerialName("tips")
    val tips: Long = 0L,
    @SerialName("staff_meals")
    val staffMeals: Long = 0L,
    @SerialName("no_sale_count")
    val noSaleCount: Int = 0,
    @SerialName("total_movements")
    val totalMovements: Int = 0
) {
    fun toDomain(): ShiftCashSummary {
        return ShiftCashSummary(
            openingFloat = openingFloat,
            cashSales = cashSales,
            cashRefunds = cashRefunds,
            refundCount = refundCount,
            paidOuts = paidOuts,
            cashIns = cashIns,
            tips = tips,
            staffMeals = staffMeals,
            noSaleCount = noSaleCount,
            totalMovements = totalMovements
        )
    }
}

@Serializable
data class UnsettledTabSummaryDto(
    @SerialName("order_number")
    val orderNumber: String,
    @SerialName("label")
    val label: String = "",
    @SerialName("status")
    val status: String = "",
    @SerialName("total_amount")
    val totalAmount: Long = 0L,
    @SerialName("created_at")
    val createdAt: String = ""
) {
    fun toDomain(): UnsettledTabSummary {
        return UnsettledTabSummary(
            orderNumber = orderNumber,
            label = label,
            status = status,
            totalAmount = totalAmount,
            createdAt = createdAt
        )
    }
}

@Serializable
data class ZReportDto(
    @SerialName("shift_id")
    val shiftId: String,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("drawer_id")
    val drawerId: String,
    @SerialName("drawer_name")
    val drawerName: String = "",
    @SerialName("opened_by")
    val openedBy: String,
    @SerialName("opened_by_name")
    val openedByName: String = "",
    @SerialName("opened_at")
    val openedAt: String,
    @SerialName("closed_by")
    val closedBy: String? = null,
    @SerialName("closed_by_name")
    val closedByName: String? = null,
    @SerialName("closed_at")
    val closedAt: String? = null,
    @SerialName("status")
    val status: String = "",
    @SerialName("summary")
    val summary: ShiftCashSummaryDto = ShiftCashSummaryDto(),
    @SerialName("expected_cash")
    val expectedCash: Long = 0L,
    @SerialName("counted_cash")
    val countedCash: Long = 0L,
    @SerialName("variance")
    val variance: Long = 0L,
    @SerialName("variance_reason")
    val varianceReason: String? = null,
    @SerialName("notes")
    val notes: String? = null,
    @SerialName("unsettled_open_tabs_count")
    val unsettledOpenTabsCount: Int = 0,
    @SerialName("unsettled_open_tabs_total_amount")
    val unsettledOpenTabsTotalAmount: Long = 0L,
    @SerialName("unsettled_open_tabs")
    val unsettledOpenTabs: List<UnsettledTabSummaryDto> = emptyList()
) {
    fun toDomain(): ZReport {
        return ZReport(
            shiftId = shiftId,
            storeId = storeId,
            drawerId = drawerId,
            drawerName = drawerName,
            openedBy = openedBy,
            openedByName = openedByName,
            openedAt = openedAt,
            closedBy = closedBy,
            closedByName = closedByName,
            closedAt = closedAt,
            status = ShiftStatus.fromString(status),
            summary = summary.toDomain(),
            expectedCash = expectedCash,
            countedCash = countedCash,
            variance = variance,
            varianceReason = varianceReason,
            notes = notes,
            unsettledOpenTabsCount = unsettledOpenTabsCount,
            unsettledOpenTabsTotalAmount = unsettledOpenTabsTotalAmount,
            unsettledOpenTabs = unsettledOpenTabs.map { it.toDomain() }
        )
    }
}
