package com.autopaymax.ui.subscription

import com.autopaymax.domain.subscription.model.Subscription
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package

enum class PlanType {
    MONTHLY,
    YEARLY,
    LIFETIME
}

data class SubscriptionState(
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val isProUser: Boolean = false,
    val currentOffering: Offerings? = null,
    val monthlyPackage: Package? = null,
    val yearlyPackage: Package? = null,
    val lifetimePackage: Package? = null,
    val selectedPlanType: PlanType = PlanType.MONTHLY,
    val selectedPackage: Package? = null,
    val formattedPrice: String = "$9.99",
    val errorMessage: String? = null,
    val subscription: Subscription? = null,
    val showRevenueCatPaywall: Boolean = false,
    val showCustomerCenter: Boolean = false
)
