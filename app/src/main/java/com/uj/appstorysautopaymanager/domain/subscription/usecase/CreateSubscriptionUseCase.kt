package com.uj.appstorysautopaymanager.domain.subscription.usecase

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.domain.subscription.model.Subscription
import com.uj.appstorysautopaymanager.domain.subscription.repository.SubscriptionRepository
import javax.inject.Inject

class CreateSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(): Resource<Subscription> = repository.createSubscription()
}
