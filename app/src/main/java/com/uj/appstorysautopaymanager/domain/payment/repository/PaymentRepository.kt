package com.uj.appstorysautopaymanager.domain.payment.repository

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.domain.payment.model.Payment

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
