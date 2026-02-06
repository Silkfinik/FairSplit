package com.silkfinik.fairsplit.core.domain.usecase.payment

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.PaymentRepository
import com.silkfinik.fairsplit.core.model.Payment
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class CreatePaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository,
    private val timeProvider: TimeProvider
) {
    data class Params(
        val groupId: String,
        val payerId: String,
        val receiverId: String,
        val amount: Double
    )

    suspend operator fun invoke(params: Params): Result<Unit> {
        if (params.amount <= 0) {
            return Result.Error(AppError.Payment.AmountTooLow)
        }
        if (params.payerId == params.receiverId) {
            return Result.Error(AppError.Payment.SelfTransferForbidden)
        }

        return try {
            if (!authRepository.hasSession()) {
                return Result.Error(AppError.Auth.NotAuthorized)
            }

            val group = groupRepository.getGroup(params.groupId).first()
                ?: return Result.Error(AppError.Group.NotFound)

            val now = timeProvider.now()

            val payment = Payment(
                id = UUID.randomUUID().toString(),
                groupId = params.groupId,
                payerId = params.payerId,
                receiverId = params.receiverId,
                amount = params.amount,
                currency = group.currency,
                status = PaymentStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )

            paymentRepository.createPayment(payment)

        } catch (e: Exception) {
            Result.Error(AppError.General(e.message ?: "Ошибка создания платежа", e))
        }
    }
}