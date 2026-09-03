package com.uj.appstorysautopaymanager.domain.subscription.usecase

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.domain.subscription.model.Subscription
import com.uj.appstorysautopaymanager.domain.subscription.repository.SubscriptionRepository
import javax.inject.Inject

class CaptureSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    // subscriptionId must be the .id from the Subscription returned by CreateSubscriptionUseCase,
    // called immediately before this in the same purchase flow.
    suspend operator fun invoke(subscriptionId: String): Resource<Subscription> =
        repository.captureSubscription(subscriptionId)
}
