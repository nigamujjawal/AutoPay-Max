package com.uj.appstorysautopaymanager.ui.profile

// One-time events (toasts, etc.) - never replayed to a new collector, unlike state.
sealed interface ProfileUiEvent {
    data class ShowMessage(val message: String) : ProfileUiEvent
}
