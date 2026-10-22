package com.autopaymax.domain.payment.usecase

import com.autopaymax.common.Resource
import com.autopaymax.domain.payment.model.Payment
import com.autopaymax.domain.payment.repository.PaymentRepository
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
