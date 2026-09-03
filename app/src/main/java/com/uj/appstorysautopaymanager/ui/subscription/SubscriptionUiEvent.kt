package com.uj.appstorysautopaymanager.ui.subscription

sealed interface SubscriptionUiEvent {
    data class ShowMessage(val message: String) : SubscriptionUiEvent
}
