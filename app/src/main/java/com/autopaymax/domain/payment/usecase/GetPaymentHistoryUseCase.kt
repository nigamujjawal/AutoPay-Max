package com.autopaymax.domain.payment.usecase

import com.autopaymax.common.Resource
import com.autopaymax.domain.payment.model.Payment
import com.autopaymax.domain.payment.repository.PaymentRepository
import javax.inject.Inject

class GetPaymentHistoryUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(): Resource<List<Payment>> = repository.getPaymentHistory()
}
