package com.autopaymax.data.repository

import com.autopaymax.common.Resource
import com.autopaymax.common.safeApiCall
import com.autopaymax.data.remote.AutoPayApi
import com.autopaymax.data.remote.dto.CaptureSubscriptionRequest
import com.autopaymax.data.remote.dto.EmptyRequest
import com.autopaymax.data.remote.dto.SubscriptionDto
import com.autopaymax.domain.subscription.model.Subscription
import com.autopaymax.domain.subscription.repository.SubscriptionRepository
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
