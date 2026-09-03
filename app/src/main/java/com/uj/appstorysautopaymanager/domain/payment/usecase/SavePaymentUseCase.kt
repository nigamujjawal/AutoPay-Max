package com.uj.appstorysautopaymanager.domain.payment.usecase

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.domain.payment.model.Payment
import com.uj.appstorysautopaymanager.domain.payment.repository.PaymentRepository
import javax.inject.Inject

class SavePaymentUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(
        amount: Double,
        currency: String,
        provider: String,
        transactionId: String,
        payeeName: String,
        timestamp: Long
    ): Resource<Payment> =
        repository.savePayment(amount, currency, provider, transactionId, payeeName, timestamp)
}
