package com.silkfinik.fairsplit.core.domain.usecase.payment

import com.silkfinik.fairsplit.core.data.sync.PaymentRealtimeListener
import com.silkfinik.fairsplit.core.domain.repository.PaymentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class SyncGroupPaymentsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val paymentRealtimeListener: PaymentRealtimeListener
) {
    fun start(groupId: String) {
        paymentRealtimeListener.startListening(groupId)
        
        CoroutineScope(Dispatchers.IO).launch {
             try {
                 paymentRepository.syncPayments(groupId)
             } catch (e: Exception) {
                 // Ignore errors
             }
        }
    }

    fun stop(groupId: String) {
        paymentRealtimeListener.stopListening(groupId)
    }
}
