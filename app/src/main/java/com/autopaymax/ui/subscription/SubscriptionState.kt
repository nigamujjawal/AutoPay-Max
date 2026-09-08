package com.autopaymax.ui.subscription

import com.autopaymax.domain.subscription.model.Subscription

data class SubscriptionState(
    val isProcessing: Boolean = false,
    val subscription: Subscription? = null
)
