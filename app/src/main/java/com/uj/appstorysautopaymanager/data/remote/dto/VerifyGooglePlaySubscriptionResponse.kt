package com.uj.appstorysautopaymanager.data.remote.dto

data class VerifyGooglePlaySubscriptionResponse(
    val isSubscribed: Boolean,
    val subscriptionId: String,
    val status: String,
    val expiresAt: String? = null,
    val message: String? = null
)
