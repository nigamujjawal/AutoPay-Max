package com.autopaymax.data.remote.dto

data class PaymentDto(
    val id: String,
    val user_id: String,
    val amount: Double,
    val currency: String,
    val provider: String,
    val transaction_id: String,
    val payee_name: String,
    val timestamp: Long,
    val created_at: String
)
