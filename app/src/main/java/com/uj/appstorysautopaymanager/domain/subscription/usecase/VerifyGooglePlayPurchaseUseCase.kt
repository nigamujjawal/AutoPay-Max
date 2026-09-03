package com.uj.appstorysautopaymanager.domain.subscription.usecase

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.domain.subscription.model.Subscription
import com.uj.appstorysautopaymanager.domain.subscription.repository.SubscriptionRepository
import javax.inject.Inject

class VerifyGooglePlayPurchaseUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(
        productId: String,
        purchaseToken: String,
        orderId: String?
    ): Resource<Subscription> = repository.verifyGooglePlaySubscription(productId, purchaseToken, orderId)
}
