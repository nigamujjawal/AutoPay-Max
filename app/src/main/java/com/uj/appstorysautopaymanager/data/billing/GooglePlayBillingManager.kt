package com.uj.appstorysautopaymanager.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePlayBillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _purchaseSuccessFlow = MutableSharedFlow<Purchase>()
    val purchaseSuccessFlow: SharedFlow<Purchase> = _purchaseSuccessFlow.asSharedFlow()

    private val _billingErrorFlow = MutableSharedFlow<String>()
    val billingErrorFlow: SharedFlow<String> = _billingErrorFlow.asSharedFlow()

    fun startConnection(onConnected: () -> Unit = {}) {
        if (billingClient.isReady) {
            _isConnected.value = true
            onConnected()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    onConnected()
                } else {
                    _isConnected.value = false
                    CoroutineScope(Dispatchers.IO).launch {
                        _billingErrorFlow.emit("Billing setup failed: ${billingResult.debugMessage}")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
            }
        })
    }

    fun querySubscriptionDetails(
        productId: String,
        onResult: (ProductDetails?, String?) -> Unit
    ) {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val details = productDetailsList.first()
                val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
                onResult(details, offerToken)
            } else {
                onResult(null, null)
                CoroutineScope(Dispatchers.IO).launch {
                    _billingErrorFlow.emit("Failed to query product details: ${billingResult.debugMessage}")
                }
            }
        }
    }

    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ): BillingResult {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    fun acknowledgePurchase(purchaseToken: String, onComplete: (Boolean) -> Unit) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            onComplete(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        CoroutineScope(Dispatchers.IO).launch {
                            _purchaseSuccessFlow.emit(purchase)
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    _billingErrorFlow.emit("Purchase cancelled by user.")
                }
            }
            else -> {
                CoroutineScope(Dispatchers.IO).launch {
                    _billingErrorFlow.emit("Purchase failed: ${billingResult.debugMessage}")
                }
            }
        }
    }
}
