package com.uj.appstorysautopaymanager.domain.subscription.repository

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.domain.subscription.model.Subscription

interface SubscriptionRepository {
    suspend fun createSubscription(): Resource<Subscription>
    suspend fun captureSubscription(subscriptionId: String): Resource<Subscription>
}
