package com.autopaymax.domain.subscription.usecase

import com.autopaymax.common.Resource
import com.autopaymax.domain.subscription.model.Subscription
import com.autopaymax.domain.subscription.repository.SubscriptionRepository
import javax.inject.Inject

class CreateSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(): Resource<Subscription> = repository.createSubscription()
}
