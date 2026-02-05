package com.silkfinik.fairsplit.core.domain.usecase.payment

import com.silkfinik.fairsplit.core.domain.repository.PaymentRepository
import javax.inject.Inject

class SyncGroupPaymentsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    fun start(groupId: String) {
        paymentRepository.startSync(groupId)
    }

    fun stop(groupId: String) {
        paymentRepository.stopSync(groupId)
    }
}