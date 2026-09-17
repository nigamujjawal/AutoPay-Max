package com.autopaymax.data.repository

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

const val ENTITLEMENT_AUTOPAY_MAX_PRO = "autopay_max_pro"

fun CustomerInfo.hasAutopayMaxPro(): Boolean {
    return entitlements[ENTITLEMENT_AUTOPAY_MAX_PRO]?.isActive == true
}

interface BillingRepository {
    fun getCustomerInfoFlow(): Flow<CustomerInfo>
    fun getOfferings(onSuccess: (Offerings) -> Unit, onError: (PurchasesError) -> Unit)
    fun purchasePackage(
        activity: Activity,
        rcPackage: Package,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError, Boolean) -> Unit
    )
    fun restorePurchases(onSuccess: (CustomerInfo) -> Unit, onError: (PurchasesError) -> Unit)
}

// AutoPayApplication.onCreate() only calls Purchases.configure() when a
// REVENUECAT_API_KEY is present in local.properties (blank in local/dev
// builds); every call below must check isConfigured first or it throws
// UninitializedPropertyAccessException on Purchases.sharedInstance.
private const val NOT_CONFIGURED_MESSAGE = "RevenueCat is not configured (missing REVENUECAT_API_KEY)"
private fun notConfiguredError() = PurchasesError(PurchasesErrorCode.ConfigurationError, NOT_CONFIGURED_MESSAGE)

@Singleton
class RevenueCatBillingRepositoryImpl @Inject constructor() : BillingRepository {

    override fun getCustomerInfoFlow(): Flow<CustomerInfo> = callbackFlow {
        if (!Purchases.isConfigured) {
            awaitClose { }
            return@callbackFlow
        }

        val listener = UpdatedCustomerInfoListener { customerInfo ->
            trySend(customerInfo)
        }
        Purchases.sharedInstance.updatedCustomerInfoListener = listener

        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                trySend(customerInfo)
            }
            override fun onError(error: PurchasesError) {
                // Initial fetch error ignored; listener will handle updates
            }
        })

        awaitClose {
            Purchases.sharedInstance.updatedCustomerInfoListener = null
        }
    }

    override fun getOfferings(onSuccess: (Offerings) -> Unit, onError: (PurchasesError) -> Unit) {
        if (!Purchases.isConfigured) {
            onError(notConfiguredError())
            return
        }
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                onSuccess(offerings)
            }
            override fun onError(error: PurchasesError) {
                onError(error)
            }
        })
    }

    override fun purchasePackage(
        activity: Activity,
        rcPackage: Package,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError, Boolean) -> Unit
    ) {
        if (!Purchases.isConfigured) {
            onError(notConfiguredError(), false)
            return
        }
        val params = PurchaseParams.Builder(activity, rcPackage).build()
        Purchases.sharedInstance.purchase(
            params,
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    onSuccess(customerInfo)
                }
                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    onError(error, userCancelled)
                }
            }
        )
    }

    override fun restorePurchases(onSuccess: (CustomerInfo) -> Unit, onError: (PurchasesError) -> Unit) {
        if (!Purchases.isConfigured) {
            onError(notConfiguredError())
            return
        }
        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                onSuccess(customerInfo)
            }
            override fun onError(error: PurchasesError) {
                onError(error)
            }
        })
    }
}
