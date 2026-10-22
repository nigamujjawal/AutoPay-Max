package com.autopaymax.domain.subscription.usecase

import com.autopaymax.common.Resource
import com.autopaymax.domain.subscription.model.Subscription
import com.autopaymax.domain.subscription.repository.SubscriptionRepository
import javax.inject.Inject

class CaptureSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    // subscriptionId must be the .id from the Subscription returned by CreateSubscriptionUseCase,
    // called immediately before this in the same purchase flow.
    suspend operator fun invoke(subscriptionId: String): Resource<Subscription> =
        repository.captureSubscription(subscriptionId)
}
