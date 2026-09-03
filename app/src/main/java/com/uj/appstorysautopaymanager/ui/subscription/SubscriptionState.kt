package com.uj.appstorysautopaymanager.ui.subscription

import com.uj.appstorysautopaymanager.domain.subscription.model.Subscription

data class SubscriptionState(
    val isProcessing: Boolean = false,
    val subscription: Subscription? = null
)
