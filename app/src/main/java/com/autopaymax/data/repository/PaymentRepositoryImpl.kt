package com.autopaymax.data.repository

import com.autopaymax.common.Resource
import com.autopaymax.common.safeApiCall
import com.autopaymax.data.remote.AutoPayApi
import com.autopaymax.data.remote.dto.CreatePaymentRequest
import com.autopaymax.data.remote.dto.PaymentDto
import com.autopaymax.domain.payment.model.Payment
import com.autopaymax.domain.payment.repository.PaymentRepository
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
