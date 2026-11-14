package com.azuredevid.cassy.domain.model

data class CreatePosOrderItemInput(
    val productId: String,
    val quantity: Int,
    val notes: String = ""
)

data class CreatePosOrderInput(
    val idempotencyKey: String,
    val tableNumber: String? = null,
    val customerName: String,
    val customerPhone: String? = null,
    val orderType: String,
    val billingMode: String = "pay_first",
    val expectedTotal: Long? = null,
    val expectedQuoteFingerprint: String? = null,
    val notes: String = "",
    val items: List<CreatePosOrderItemInput>
)

data class PosOrderItem(
    val id: String,
    val productId: String? = null,
    val snapshotName: String,
    val unitBasePrice: Long = 0L,
    val modifierTotalPrice: Long = 0L,
    val snapshotPrice: Long = 0L,
    val quantity: Int,
    val subtotalAmount: Long = 0L,
    val snapshotPrepStation: String = "bar",
    val notes: String = ""
)

data class PosOrder(
    val id: String,
    val orderNumber: String,
    val queueNumber: String = "",
    val tableNumber: String = "",
    val customerName: String = "",
    val orderType: String = "dine_in",
    val billingMode: String = "pay_first",
    val status: String = "",
    val paymentStatus: String = "",
    val subtotalAmount: Long = 0L,
    val discountAmount: Long = 0L,
    val taxAmount: Long = 0L,
    val serviceChargeAmount: Long = 0L,
    val totalAmount: Long = 0L,
    val paidAmount: Long = 0L,
    val items: List<PosOrderItem> = emptyList()
)

data class InitiatePosPaymentInput(
    val orderId: String,
    val idempotencyKey: String,
    val method: String,
    val cashTenderedAmount: Long? = null,
    val drawerId: String? = null,
    val splitId: String? = null
)

data class PosPaymentAttempt(
    val id: String,
    val providerOrderId: String,
    val method: String,
    val status: String,
    val amount: Long,
    val cashTenderedAmount: Long? = null,
    val roundingAmount: Long? = null,
    val changeGivenAmount: Long? = null,
    val qrString: String = "",
    val deeplinkUrl: String = "",
    val expiresAt: String? = null
)

data class InitiatePosPaymentResult(
    val isIdempotentReplay: Boolean = false,
    val openDrawer: Boolean = false,
    val payment: PosPaymentAttempt,
    val order: PosOrder? = null
)

data class PosOrderPaymentSummary(
    val id: String,
    val method: String = "",
    val status: String = "",
    val amount: Long = 0L
)

data class PosOrderDetail(
    val order: PosOrder,
    val payments: List<PosOrderPaymentSummary> = emptyList(),
    val allowedActions: List<String> = emptyList()
)
