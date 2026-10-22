package com.autopaymax.domain.payment.model

data class Payment(
    val id: String,
    val userId: String,
    val amount: Double,
    val currency: String,
    val provider: String,
    val transactionId: String,
    val payeeName: String,
    val timestamp: Long,
    val createdAt: String
)
