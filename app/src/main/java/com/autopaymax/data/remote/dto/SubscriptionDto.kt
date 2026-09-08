package com.autopaymax.data.remote.dto

data class SubscriptionDto(
    val id: String,
    val user_id: String,
    val provider: String,
    val external_subscription_id: String,
    val status: String,
    val starts_at: String,
    val ends_at: String?,
    val created_at: String,
    val updated_at: String
)
