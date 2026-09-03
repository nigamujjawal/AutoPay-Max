package com.uj.appstorysautopaymanager.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.data.billing.GooglePlayBillingManager
import com.uj.appstorysautopaymanager.domain.subscription.usecase.VerifyGooglePlayPurchaseUseCase
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
    private val billingManager: GooglePlayBillingManager,
    private val verifyGooglePlayPurchase: VerifyGooglePlayPurchaseUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<SubscriptionUiEvent>()
    val event: SharedFlow<SubscriptionUiEvent> = _event.asSharedFlow()

    init {
        billingManager.startConnection()

        // Listen for successful purchases from Google Play
        viewModelScope.launch {
            billingManager.purchaseSuccessFlow.collect { purchase ->
                handlePurchaseSuccess(purchase)
            }
        }

        // Listen for billing error messages
        viewModelScope.launch {
            billingManager.billingErrorFlow.collect { errorMessage ->
                _state.update { it.copy(isProcessing = false) }
                _event.emit(SubscriptionUiEvent.ShowMessage(errorMessage))
            }
        }
    }

    fun launchGooglePlaySubscription(activity: Activity, productId: String = "autopay_monthly_plan") {
        _state.update { it.copy(isProcessing = true) }
        billingManager.startConnection {
            billingManager.querySubscriptionDetails(productId) { productDetails, offerToken ->
                if (productDetails != null && offerToken != null) {
                    val result = billingManager.launchPurchaseFlow(activity, productDetails, offerToken)
                    if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                        _state.update { it.copy(isProcessing = false) }
                    }
                } else {
                    _state.update { it.copy(isProcessing = false) }
                    viewModelScope.launch {
                        _event.emit(SubscriptionUiEvent.ShowMessage("Subscription plan not found in Play Store"))
                    }
                }
            }
        }
    }

    private fun handlePurchaseSuccess(purchase: com.android.billingclient.api.Purchase) {
        viewModelScope.launch {
            val productId = purchase.products.firstOrNull() ?: "autopay_monthly_plan"
            when (val verified = verifyGooglePlayPurchase(productId, purchase.purchaseToken, purchase.orderId)) {
                is Resource.Success -> {
                    // Acknowledge the purchase with Google Play after backend verification
                    billingManager.acknowledgePurchase(purchase.purchaseToken) { _ -> }
                    _state.update { it.copy(isProcessing = false, subscription = verified.data) }
                    _event.emit(SubscriptionUiEvent.ShowMessage("Subscription active! AutoPay mandate setup successfully."))
                }
                is Resource.Error -> {
                    _state.update { it.copy(isProcessing = false) }
                    _event.emit(SubscriptionUiEvent.ShowMessage(verified.message ?: "Failed to verify purchase on server"))
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
