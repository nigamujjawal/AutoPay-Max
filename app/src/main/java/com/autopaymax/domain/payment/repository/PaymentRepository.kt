package com.autopaymax.domain.payment.repository

import com.autopaymax.common.Resource
import com.autopaymax.domain.payment.model.Payment

interface PaymentRepository {
    suspend fun savePayment(
        amount: Double,
        currency: String,
        provider: String,
        transactionId: String,
        payeeName: String,
        timestamp: Long
    ): Resource<Payment>

    suspend fun getPaymentHistory(): Resource<List<Payment>>
}
