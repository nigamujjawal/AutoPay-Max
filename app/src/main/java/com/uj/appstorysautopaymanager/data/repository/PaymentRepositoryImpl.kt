package com.uj.appstorysautopaymanager.data.repository

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.common.safeApiCall
import com.uj.appstorysautopaymanager.data.remote.AutoPayApi
import com.uj.appstorysautopaymanager.data.remote.dto.CreatePaymentRequest
import com.uj.appstorysautopaymanager.data.remote.dto.PaymentDto
import com.uj.appstorysautopaymanager.domain.payment.model.Payment
import com.uj.appstorysautopaymanager.domain.payment.repository.PaymentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val api: AutoPayApi
) : PaymentRepository {

    override suspend fun savePayment(
        amount: Double,
        currency: String,
        provider: String,
        transactionId: String,
        payeeName: String,
        timestamp: Long
    ): Resource<Payment> = safeApiCall {
        api.savePayment(
            CreatePaymentRequest(
                amount = amount,
                currency = currency,
                provider = provider,
                transaction_id = transactionId,
                payee_name = payeeName,
                timestamp = timestamp
            )
        ).toDomain()
    }

    override suspend fun getPaymentHistory(): Resource<List<Payment>> = safeApiCall {
        api.getPayments().map { it.toDomain() }
    }

    private fun PaymentDto.toDomain() = Payment(
        id = id,
        userId = user_id,
        amount = amount,
        currency = currency,
        provider = provider,
        transactionId = transaction_id,
        payeeName = payee_name,
        timestamp = timestamp,
        createdAt = created_at
    )
}
