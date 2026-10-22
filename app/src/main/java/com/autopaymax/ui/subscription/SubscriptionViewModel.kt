package com.autopaymax.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopaymax.data.repository.BillingRepository
import com.autopaymax.data.repository.hasAutopayMaxPro
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
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
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<SubscriptionUiEvent>()
    val event: SharedFlow<SubscriptionUiEvent> = _event.asSharedFlow()

    init {
        observeCustomerInfo()
        fetchOfferings()
    }

    private fun observeCustomerInfo() {
        viewModelScope.launch {
            billingRepository.getCustomerInfoFlow().collect { customerInfo ->
                val isPro = customerInfo.hasAutopayMaxPro()
                _state.update { it.copy(isProUser = isPro) }
            }
        }
    }

    fun fetchOfferings() {
        _state.update { it.copy(isLoading = true) }
        billingRepository.getOfferings(
            onSuccess = { offerings ->
                val currentOffering = offerings.current
                val monthly = currentOffering?.monthly ?: currentOffering?.availablePackages?.find { it.packageType == PackageType.MONTHLY || it.identifier.contains("monthly", ignoreCase = true) }
                val yearly = currentOffering?.annual ?: currentOffering?.availablePackages?.find { it.packageType == PackageType.ANNUAL || it.identifier.contains("yearly", ignoreCase = true) || it.identifier.contains("annual", ignoreCase = true) }
                val lifetime = currentOffering?.lifetime ?: currentOffering?.availablePackages?.find { it.packageType == PackageType.LIFETIME || it.identifier.contains("lifetime", ignoreCase = true) }

                val initialSelectedPackage = when (_state.value.selectedPlanType) {
                    PlanType.MONTHLY -> monthly ?: currentOffering?.availablePackages?.firstOrNull()
                    PlanType.YEARLY -> yearly ?: monthly
                    PlanType.LIFETIME -> lifetime ?: monthly
                }

                val price = initialSelectedPackage?.product?.price?.formatted ?: "$9.99"

                _state.update {
                    it.copy(
                        isLoading = false,
                        currentOffering = offerings,
                        monthlyPackage = monthly,
                        yearlyPackage = yearly,
                        lifetimePackage = lifetime,
                        selectedPackage = initialSelectedPackage,
                        formattedPrice = price
                    )
                }
            },
            onError = { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            }
        )
    }

    fun selectPlanType(planType: PlanType) {
        val selectedPkg = when (planType) {
            PlanType.MONTHLY -> _state.value.monthlyPackage
            PlanType.YEARLY -> _state.value.yearlyPackage
            PlanType.LIFETIME -> _state.value.lifetimePackage
        } ?: _state.value.selectedPackage

        val price = selectedPkg?.product?.price?.formatted ?: _state.value.formattedPrice

        _state.update {
            it.copy(
                selectedPlanType = planType,
                selectedPackage = selectedPkg,
                formattedPrice = price
            )
        }
    }

    fun subscribe(activity: Activity?) {
        val packageToPurchase = _state.value.selectedPackage
        if (activity == null || packageToPurchase == null) {
            viewModelScope.launch {
                _event.emit(SubscriptionUiEvent.ShowMessage("Package or Activity not available"))
            }
            return
        }

        _state.update { it.copy(isProcessing = true) }
        billingRepository.purchasePackage(
            activity = activity,
            rcPackage = packageToPurchase,
            onSuccess = { customerInfo ->
                val isPro = customerInfo.hasAutopayMaxPro()
                _state.update { it.copy(isProcessing = false, isProUser = isPro) }
                viewModelScope.launch {
                    _event.emit(SubscriptionUiEvent.ShowMessage("autopay_max_pro entitlement unlocked!"))
                }
            },
            onError = { error, userCancelled ->
                _state.update { it.copy(isProcessing = false) }
                if (!userCancelled) {
                    viewModelScope.launch {
                        _event.emit(SubscriptionUiEvent.ShowMessage(error.message))
                    }
                }
            }
        )
    }

    fun restorePurchases() {
        _state.update { it.copy(isProcessing = true) }
        billingRepository.restorePurchases(
            onSuccess = { customerInfo ->
                val isPro = customerInfo.hasAutopayMaxPro()
                _state.update { it.copy(isProcessing = false, isProUser = isPro) }
                viewModelScope.launch {
                    val msg = if (isPro) "Purchases restored: autopay_max_pro active!" else "No active subscriptions found for autopay_max_pro."
                    _event.emit(SubscriptionUiEvent.ShowMessage(msg))
                }
            },
            onError = { error ->
                _state.update { it.copy(isProcessing = false) }
                viewModelScope.launch {
                    _event.emit(SubscriptionUiEvent.ShowMessage(error.message))
                }
            }
        )
    }

    fun toggleRevenueCatPaywall(show: Boolean) {
        _state.update { it.copy(showRevenueCatPaywall = show) }
    }

    fun toggleCustomerCenter(show: Boolean) {
        _state.update { it.copy(showCustomerCenter = show) }
    }
}
