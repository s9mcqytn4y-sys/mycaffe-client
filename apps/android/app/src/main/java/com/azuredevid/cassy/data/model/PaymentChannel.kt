package com.azuredevid.cassy.data.model

data class PaymentChannel(
    val id: String,
    val name: String,
    val category: String, // "qris", "ewallet", "bank_transfer", "card"
    val assetPath: String,
    val aspectRatio: Float,
    val feeType: String,
    val defaultFeeRate: Double? = null,
    val defaultFeeNominal: Long? = null,
    val settlementSla: String
)

object PaymentChannelRegistry {
    val defaultChannels = listOf(
        PaymentChannel(
            id = "qris",
            name = "QRIS (Semua Pembayaran)",
            category = "qris",
            assetPath = "brand/midtrans/qris.png",
            aspectRatio = 3.33f,
            feeType = "percentage",
            defaultFeeRate = 0.007,
            settlementSla = "T+1 Hari Kerja"
        ),
        PaymentChannel(
            id = "gopay",
            name = "GoPay / GoPay Later",
            category = "ewallet",
            assetPath = "brand/midtrans/gopay.svg",
            aspectRatio = 4.10f,
            feeType = "percentage",
            defaultFeeRate = 0.02,
            settlementSla = "Real-time"
        ),
        PaymentChannel(
            id = "shopeepay",
            name = "ShopeePay",
            category = "ewallet",
            assetPath = "brand/midtrans/shopeepay.svg",
            aspectRatio = 3.50f,
            feeType = "percentage",
            defaultFeeRate = 0.02,
            settlementSla = "Real-time"
        ),
        PaymentChannel(
            id = "bca_va",
            name = "BCA Virtual Account",
            category = "bank_transfer",
            assetPath = "brand/midtrans/bca.png",
            aspectRatio = 3.20f,
            feeType = "fixed",
            defaultFeeNominal = 4000L,
            settlementSla = "Real-time"
        ),
        PaymentChannel(
            id = "bni_va",
            name = "BNI Virtual Account",
            category = "bank_transfer",
            assetPath = "brand/midtrans/bni.png",
            aspectRatio = 3.29f,
            feeType = "fixed",
            defaultFeeNominal = 4000L,
            settlementSla = "Real-time"
        ),
        PaymentChannel(
            id = "bri_va",
            name = "BRI Virtual Account (BRIVA)",
            category = "bank_transfer",
            assetPath = "brand/midtrans/bri.png",
            aspectRatio = 2.64f,
            feeType = "fixed",
            defaultFeeNominal = 4000L,
            settlementSla = "Real-time"
        ),
        PaymentChannel(
            id = "mandiri_va",
            name = "Mandiri Bill Payment",
            category = "bank_transfer",
            assetPath = "brand/midtrans/mandiri.png",
            aspectRatio = 3.38f,
            feeType = "fixed",
            defaultFeeNominal = 4000L,
            settlementSla = "Real-time"
        ),
        PaymentChannel(
            id = "permata_va",
            name = "Permata Virtual Account",
            category = "bank_transfer",
            assetPath = "brand/midtrans/permata_bank.png",
            aspectRatio = 3.73f,
            feeType = "fixed",
            defaultFeeNominal = 4000L,
            settlementSla = "Real-time"
        ),
        PaymentChannel(
            id = "cimb_va",
            name = "CIMB Niaga VA",
            category = "bank_transfer",
            assetPath = "brand/midtrans/cimbniaga.png",
            aspectRatio = 5.84f,
            feeType = "fixed",
            defaultFeeNominal = 4000L,
            settlementSla = "Real-time"
        ),
        PaymentChannel(
            id = "credit_card",
            name = "Kartu Kredit / Debit",
            category = "card",
            assetPath = "brand/midtrans/visa.png",
            aspectRatio = 3.20f,
            feeType = "percentage_and_fixed",
            defaultFeeRate = 0.028,
            defaultFeeNominal = 2000L,
            settlementSla = "T+2 Hari Kerja"
        )
    )
}

