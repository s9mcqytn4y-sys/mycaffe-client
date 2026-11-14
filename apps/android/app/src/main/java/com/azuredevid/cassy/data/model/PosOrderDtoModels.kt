package com.azuredevid.cassy.data.model

import com.azuredevid.cassy.domain.model.InitiatePosPaymentResult
import com.azuredevid.cassy.domain.model.PosOrder
import com.azuredevid.cassy.domain.model.PosOrderDetail
import com.azuredevid.cassy.domain.model.PosOrderItem
import com.azuredevid.cassy.domain.model.PosOrderPaymentSummary
import com.azuredevid.cassy.domain.model.PosPaymentAttempt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderItemInputDto(
    @SerialName("product_id")
    val productId: String,
    @SerialName("quantity")
    val quantity: Int,
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class CreateOrderRequestDto(
    @SerialName("idempotency_key")
    val idempotencyKey: String,
    @SerialName("table_number")
    val tableNumber: String? = null,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("customer_phone")
    val customerPhone: String? = null,
    @SerialName("order_type")
    val orderType: String,
    @SerialName("billing_mode")
    val billingMode: String = "pay_first",
    @SerialName("expected_total")
    val expectedTotal: Long? = null,
    @SerialName("expected_quote_fingerprint")
    val expectedQuoteFingerprint: String? = null,
    @SerialName("notes")
    val notes: String? = null,
    @SerialName("items")
    val items: List<CreateOrderItemInputDto>
)

@Serializable
data class PosOrderItemDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("product_id")
    val productId: String? = null,
    @SerialName("snapshot_name")
    val snapshotName: String = "",
    @SerialName("unit_base_price")
    val unitBasePrice: Long = 0L,
    @SerialName("modifier_total_price")
    val modifierTotalPrice: Long = 0L,
    @SerialName("snapshot_price")
    val snapshotPrice: Long = 0L,
    @SerialName("quantity")
    val quantity: Int = 1,
    @SerialName("subtotal_amount")
    val subtotalAmount: Long = 0L,
    @SerialName("snapshot_prep_station")
    val snapshotPrepStation: String = "bar",
    @SerialName("notes")
    val notes: String = ""
) {
    fun toDomain(): PosOrderItem = PosOrderItem(
        id = id,
        productId = productId,
        snapshotName = snapshotName,
        unitBasePrice = unitBasePrice,
        modifierTotalPrice = modifierTotalPrice,
        snapshotPrice = snapshotPrice,
        quantity = quantity,
        subtotalAmount = subtotalAmount,
        snapshotPrepStation = snapshotPrepStation,
        notes = notes
    )
}

@Serializable
data class PosOrderDto(
    @SerialName("id")
    val id: String,
    @SerialName("order_number")
    val orderNumber: String = "",
    @SerialName("queue_number")
    val queueNumber: String = "",
    @SerialName("table_number")
    val tableNumber: String = "",
    @SerialName("customer_name")
    val customerName: String = "",
    @SerialName("order_type")
    val orderType: String = "dine_in",
    @SerialName("billing_mode")
    val billingMode: String = "pay_first",
    @SerialName("status")
    val status: String = "",
    @SerialName("payment_status")
    val paymentStatus: String = "",
    @SerialName("subtotal_amount")
    val subtotalAmount: Long = 0L,
    @SerialName("discount_amount")
    val discountAmount: Long = 0L,
    @SerialName("tax_amount")
    val taxAmount: Long = 0L,
    @SerialName("service_charge_amount")
    val serviceChargeAmount: Long = 0L,
    @SerialName("total_amount")
    val totalAmount: Long = 0L,
    @SerialName("paid_amount")
    val paidAmount: Long = 0L,
    @SerialName("items")
    val items: List<PosOrderItemDto> = emptyList()
) {
    fun toDomain(): PosOrder = PosOrder(
        id = id,
        orderNumber = orderNumber,
        queueNumber = queueNumber,
        tableNumber = tableNumber,
        customerName = customerName,
        orderType = orderType,
        billingMode = billingMode,
        status = status,
        paymentStatus = paymentStatus,
        subtotalAmount = subtotalAmount,
        discountAmount = discountAmount,
        taxAmount = taxAmount,
        serviceChargeAmount = serviceChargeAmount,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        items = items.map { it.toDomain() }
    )
}

@Serializable
data class CreateOrderResponseDto(
    @SerialName("order")
    val order: PosOrderDto,
    @SerialName("is_idempotent_replay")
    val isIdempotentReplay: Boolean = false
)

@Serializable
data class InitiatePaymentRequestDto(
    @SerialName("idempotency_key")
    val idempotencyKey: String,
    @SerialName("split_id")
    val splitId: String? = null,
    @SerialName("method")
    val method: String,
    @SerialName("cash_tendered_amount")
    val cashTenderedAmount: Long? = null,
    @SerialName("drawer_id")
    val drawerId: String? = null
)

@Serializable
data class PosPaymentAttemptDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("provider_order_id")
    val providerOrderId: String = "",
    @SerialName("method")
    val method: String = "",
    @SerialName("status")
    val status: String = "",
    @SerialName("amount")
    val amount: Long = 0L,
    @SerialName("cash_tendered_amount")
    val cashTenderedAmount: Long? = null,
    @SerialName("rounding_amount")
    val roundingAmount: Long? = null,
    @SerialName("change_given_amount")
    val changeGivenAmount: Long? = null,
    @SerialName("qr_string")
    val qrString: String = "",
    @SerialName("deeplink_url")
    val deeplinkUrl: String = "",
    @SerialName("expires_at")
    val expiresAt: String? = null
) {
    fun toDomain(): PosPaymentAttempt = PosPaymentAttempt(
        id = id,
        providerOrderId = providerOrderId,
        method = method,
        status = status,
        amount = amount,
        cashTenderedAmount = cashTenderedAmount,
        roundingAmount = roundingAmount,
        changeGivenAmount = changeGivenAmount,
        qrString = qrString,
        deeplinkUrl = deeplinkUrl,
        expiresAt = expiresAt
    )
}

@Serializable
data class InitiatePaymentResponseDto(
    @SerialName("is_idempotent_replay")
    val isIdempotentReplay: Boolean = false,
    @SerialName("open_drawer")
    val openDrawer: Boolean = false,
    @SerialName("payment")
    val payment: PosPaymentAttemptDto,
    @SerialName("order")
    val order: PosOrderDto? = null
) {
    fun toDomain(): InitiatePosPaymentResult = InitiatePosPaymentResult(
        isIdempotentReplay = isIdempotentReplay,
        openDrawer = openDrawer,
        payment = payment.toDomain(),
        order = order?.toDomain()
    )
}

@Serializable
data class PosOrderPaymentViewDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("method")
    val method: String = "",
    @SerialName("status")
    val status: String = "",
    @SerialName("amount")
    val amount: Long = 0L
) {
    fun toDomain(): PosOrderPaymentSummary = PosOrderPaymentSummary(
        id = id,
        method = method,
        status = status,
        amount = amount
    )
}

@Serializable
data class OrderDetailResponseDto(
    @SerialName("order")
    val order: PosOrderDto,
    @SerialName("payments")
    val payments: List<PosOrderPaymentViewDto> = emptyList(),
    @SerialName("allowed_actions")
    val allowedActions: List<String> = emptyList()
) {
    fun toDomain(): PosOrderDetail = PosOrderDetail(
        order = order.toDomain(),
        payments = payments.map { it.toDomain() },
        allowedActions = allowedActions
    )
}
