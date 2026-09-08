package com.autopaymax.ui.subscription

sealed interface SubscriptionUiEvent {
    data class ShowMessage(val message: String) : SubscriptionUiEvent
}
