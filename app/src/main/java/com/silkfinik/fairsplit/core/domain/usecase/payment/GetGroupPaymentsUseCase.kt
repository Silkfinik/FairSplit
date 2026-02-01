package com.silkfinik.fairsplit.core.domain.usecase.payment

import com.silkfinik.fairsplit.core.domain.repository.PaymentRepository
import com.silkfinik.fairsplit.core.model.Payment
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGroupPaymentsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(groupId: String): Flow<List<Payment>> {
        return paymentRepository.getPayments(groupId)
    }
}
