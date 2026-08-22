package com.uj.appstorysautopaymanager.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.domain.subscription.usecase.CaptureSubscriptionUseCase
import com.uj.appstorysautopaymanager.domain.subscription.usecase.CreateSubscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val createSubscription: CreateSubscriptionUseCase,
    private val captureSubscription: CaptureSubscriptionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<SubscriptionUiEvent>()
    val event: SharedFlow<SubscriptionUiEvent> = _event.asSharedFlow()

    // Create + immediately capture - there's no real payment collection step wired up yet
    // (Razorpay checkout isn't integrated), so this is create-and-activate for now.
    fun subscribe() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            when (val created = createSubscription()) {
                is Resource.Success -> {
                    val subscriptionId = created.data?.id
                    if (subscriptionId == null) {
                        _state.update { it.copy(isProcessing = false) }
                        _event.emit(SubscriptionUiEvent.ShowMessage("Subscription was created without an id"))
                        return@launch
                    }
                    when (val captured = captureSubscription(subscriptionId)) {
                        is Resource.Success -> {
                            _state.update { it.copy(isProcessing = false, subscription = captured.data) }
                            _event.emit(SubscriptionUiEvent.ShowMessage("Subscription active"))
                        }
                        is Resource.Error -> {
                            _state.update { it.copy(isProcessing = false) }
                            _event.emit(SubscriptionUiEvent.ShowMessage(captured.message ?: "Failed to activate subscription"))
                        }
                        is Resource.Loading -> Unit
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isProcessing = false) }
                    _event.emit(SubscriptionUiEvent.ShowMessage(created.message ?: "Failed to create subscription"))
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
