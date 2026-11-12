package com.azuredevid.cassy.domain.model

enum class ShiftStatus(val value: String) {
    OPEN("open"),
    CLOSED("closed"),
    FORCE_CLOSED("force_closed");

    companion object {
        fun fromString(value: String?): ShiftStatus {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: CLOSED
        }
    }
}

enum class CashMovementType(val value: String, val isDeduction: Boolean) {
    OPENING_FLOAT("opening_float", false),
    SALE("sale", false),
    REFUND("refund", true),
    PAID_OUT("paid_out", true),
    CASH_IN("cash_in", false),
    NO_SALE("no_sale", false),
    TIP("tip", false),
    STAFF_MEAL("staff_meal", true);

    companion object {
        fun fromString(value: String?): CashMovementType {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: CASH_IN
        }
    }
}

data class CashDrawer(
    val id: String,
    val storeId: String,
    val name: String,
    val isActive: Boolean = true
)

data class Shift(
    val id: String,
    val storeId: String,
    val drawerId: String,
    val openedBy: String,
    val openedByName: String = "",
    val openingFloat: Long,
    val openedAt: String,
    val closedBy: String? = null,
    val closedByName: String? = null,
    val closedAt: String? = null,
    val status: ShiftStatus = ShiftStatus.OPEN,
    val expectedCash: Long? = null,
    val countedCash: Long? = null,
    val variance: Long? = null,
    val varianceReason: String? = null,
    val notes: String? = null
)

data class CashMovement(
    val id: String,
    val storeId: String,
    val shiftId: String,
    val movementType: CashMovementType,
    val amount: Long,
    val actorId: String,
    val reason: String? = null,
    val createdAt: String
)

data class ShiftCashSummary(
    val openingFloat: Long = 0L,
    val cashSales: Long = 0L,
    val cashRefunds: Long = 0L,
    val refundCount: Int = 0,
    val paidOuts: Long = 0L,
    val cashIns: Long = 0L,
    val tips: Long = 0L,
    val staffMeals: Long = 0L,
    val noSaleCount: Int = 0,
    val totalMovements: Int = 0
) {
    fun calculateExpectedCash(): Long {
        return openingFloat + cashSales + tips + cashIns - cashRefunds - paidOuts - staffMeals
    }
}

data class UnsettledTabSummary(
    val orderNumber: String,
    val label: String,
    val status: String,
    val totalAmount: Long,
    val createdAt: String
)

data class ZReport(
    val shiftId: String,
    val storeId: String,
    val drawerId: String,
    val drawerName: String = "",
    val openedBy: String,
    val openedByName: String = "",
    val openedAt: String,
    val closedBy: String? = null,
    val closedByName: String? = null,
    val closedAt: String? = null,
    val status: ShiftStatus = ShiftStatus.CLOSED,
    val summary: ShiftCashSummary = ShiftCashSummary(),
    val expectedCash: Long = 0L,
    val countedCash: Long = 0L,
    val variance: Long = 0L,
    val varianceReason: String? = null,
    val notes: String? = null,
    val unsettledOpenTabsCount: Int = 0,
    val unsettledOpenTabsTotalAmount: Long = 0L,
    val unsettledOpenTabs: List<UnsettledTabSummary> = emptyList()
)
