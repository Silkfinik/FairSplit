package com.silkfinik.fairsplit.core.domain.usecase.payment

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.PaymentRepository
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdatePaymentStatusUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(paymentId: String, newStatus: PaymentStatus): Result<Unit> {
        return try {
            val payment = paymentRepository.getPayment(paymentId).first()
                ?: return Result.Error(AppError.Payment.NotFound)

            if (payment.status != PaymentStatus.PENDING) {
                return Result.Error(AppError.Payment.StatusAlreadyChanged)
            }

            val updatedPayment = payment.copy(
                status = newStatus,
                updatedAt = timeProvider.now()
            )

            paymentRepository.updatePayment(updatedPayment)

        } catch (e: Exception) {
            Result.Error(AppError.General(e.message ?: "Ошибка обновления статуса", e))
        }
    }
}