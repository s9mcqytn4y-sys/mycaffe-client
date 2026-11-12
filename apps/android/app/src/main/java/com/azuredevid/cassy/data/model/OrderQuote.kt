package com.azuredevid.cassy.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Totals priced by the server (POST /api/v1/pos/orders/quote). All amounts are whole rupiah. */
@Serializable
data class OrderQuote(
    @SerialName("subtotal_amount") val subtotalAmount: Long,
    @SerialName("discount_amount") val discountAmount: Long = 0,
    @SerialName("service_charge_amount") val serviceChargeAmount: Long = 0,
    @SerialName("tax_amount") val taxAmount: Long = 0,
    @SerialName("total_amount") val totalAmount: Long,
    @SerialName("quote_fingerprint") val quoteFingerprint: String = ""
)

@Serializable
data class OrderQuoteRequest(
    @SerialName("order_type") val orderType: String,
    val items: List<OrderQuoteItem>
)

@Serializable
data class OrderQuoteItem(
    @SerialName("product_id") val productId: String,
    val quantity: Int
)
