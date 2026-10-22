package com.autopaymax.domain.subscription.model

data class Subscription(
    val id: String,
    val userId: String,
    val provider: String,
    val externalSubscriptionId: String,
    val status: String,
    val startsAt: String,
    val endsAt: String?,
    val createdAt: String,
    val updatedAt: String
)
