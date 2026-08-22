package com.uj.appstorysautopaymanager.data.repository

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.common.safeApiCall
import com.uj.appstorysautopaymanager.data.remote.AutoPayApi
import com.uj.appstorysautopaymanager.data.remote.dto.CaptureSubscriptionRequest
import com.uj.appstorysautopaymanager.data.remote.dto.EmptyRequest
import com.uj.appstorysautopaymanager.data.remote.dto.SubscriptionDto
import com.uj.appstorysautopaymanager.domain.subscription.model.Subscription
import com.uj.appstorysautopaymanager.domain.subscription.repository.SubscriptionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val api: AutoPayApi
) : SubscriptionRepository {

    override suspend fun createSubscription(): Resource<Subscription> = safeApiCall {
        api.createSubscription(EmptyRequest()).toDomain()
    }

    override suspend fun captureSubscription(subscriptionId: String): Resource<Subscription> = safeApiCall {
        api.captureSubscription(CaptureSubscriptionRequest(subscription_id = subscriptionId)).toDomain()
    }

    private fun SubscriptionDto.toDomain() = Subscription(
        id = id,
        userId = user_id,
        provider = provider,
        externalSubscriptionId = external_subscription_id,
        status = status,
        startsAt = starts_at,
        endsAt = ends_at,
        createdAt = created_at,
        updatedAt = updated_at
    )
}
