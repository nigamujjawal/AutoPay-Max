package com.autopaymax.domain.subscription.repository

import com.autopaymax.common.Resource
import com.autopaymax.domain.subscription.model.Subscription

interface SubscriptionRepository {
    suspend fun createSubscription(): Resource<Subscription>
    suspend fun captureSubscription(subscriptionId: String): Resource<Subscription>
}
