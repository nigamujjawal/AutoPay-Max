package com.autopaymax.data.remote.dto

data class CreatePaymentRequest(
    val amount: Double,
    val currency: String,
    val provider: String,
    val transaction_id: String,
    val payee_name: String,
    val timestamp: Long
)
