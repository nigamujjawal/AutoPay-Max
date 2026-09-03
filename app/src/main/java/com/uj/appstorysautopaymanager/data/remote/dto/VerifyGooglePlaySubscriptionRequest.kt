package com.uj.appstorysautopaymanager.data.remote.dto

data class VerifyGooglePlaySubscriptionRequest(
    val productId: String,
    val purchaseToken: String,
    val orderId: String? = null
)
