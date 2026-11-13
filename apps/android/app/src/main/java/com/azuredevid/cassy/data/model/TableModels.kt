package com.azuredevid.cassy.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TableRecord(
    val id: String,
    @SerialName("table_number") val tableNumber: String,
    val label: String = "",
    val section: String = "",
    val capacity: Int = 2,
    @SerialName("qr_token") val qrToken: String = "",
    @SerialName("qr_rotated_at") val qrRotatedAt: String = "",
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class TableSessionInfo(
    val id: String,
    @SerialName("table_id") val tableId: String = "",
    val status: String = "",
    @SerialName("opened_at") val openedAt: String = "",
    @SerialName("ends_at") val endsAt: String? = null
)

@Serializable
data class TableBoardEntry(
    val id: String,
    @SerialName("table_number") val tableNumber: String,
    val label: String = "",
    val section: String = "",
    val capacity: Int = 2,
    @SerialName("qr_token") val qrToken: String = "",
    @SerialName("qr_rotated_at") val qrRotatedAt: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    val status: String = "available",
    val session: TableSessionInfo? = null,
    @SerialName("unpaid_orders") val unpaidOrders: Int = 0,
    @SerialName("unpaid_amount") val unpaidAmount: Long = 0L,
    @SerialName("merged_into_table_id") val mergedIntoTableId: String? = null,
    @SerialName("timer_state") val timerState: String = "none"
) {
    val displayTitle: String
        get() = label.ifBlank { "Meja $tableNumber" }
}

@Serializable
data class TableInput(
    @SerialName("table_number") val tableNumber: String? = null,
    val label: String? = null,
    val section: String? = null,
    val capacity: Int? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)
