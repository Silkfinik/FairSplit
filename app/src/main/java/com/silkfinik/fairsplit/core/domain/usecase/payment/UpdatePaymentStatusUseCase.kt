package com.silkfinik.fairsplit.core.domain.usecase.payment

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.PaymentRepository
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdatePaymentStatusUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(paymentId: String, newStatus: PaymentStatus): Result<Unit> {
        return try {
            val payment = paymentRepository.getPayment(paymentId).first()
                ?: return Result.Error("Платеж не найден")

            if (payment.status != PaymentStatus.PENDING) {
                return Result.Error("Статус платежа уже изменен")
            }

            // In a real app, we should check permissions (only Receiver can Confirm/Reject).
            // But we'll trust the UI/Caller for now or add check if we pass currentUserId.
            
            val updatedPayment = payment.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            
            paymentRepository.updatePayment(updatedPayment)
            
            // Trigger sync to push the update
            try {
                paymentRepository.syncPayments(payment.groupId)
            } catch (e: Exception) {
                // Ignore sync errors here
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка обновления статуса", e)
        }
    }
}
