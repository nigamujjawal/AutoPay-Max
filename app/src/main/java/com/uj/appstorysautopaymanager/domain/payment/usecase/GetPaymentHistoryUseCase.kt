package com.uj.appstorysautopaymanager.domain.payment.usecase

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.domain.payment.model.Payment
import com.uj.appstorysautopaymanager.domain.payment.repository.PaymentRepository
import javax.inject.Inject

class GetPaymentHistoryUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(): Resource<List<Payment>> = repository.getPaymentHistory()
}
