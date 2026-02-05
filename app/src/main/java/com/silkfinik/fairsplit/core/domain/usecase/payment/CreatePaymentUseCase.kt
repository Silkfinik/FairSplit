package com.silkfinik.fairsplit.core.domain.usecase.payment

import com.silkfinik.fairsplit.core.common.util.Result
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
    private val authRepository: AuthRepository
) {
    data class Params(
        val groupId: String,
        val payerId: String,
        val receiverId: String,
        val amount: Double
    )

    suspend operator fun invoke(params: Params): Result<Unit> {
        if (params.amount <= 0) return Result.Error("Сумма должна быть больше 0")
        if (params.payerId == params.receiverId) return Result.Error("Нельзя перевести самому себе")

        return try {
            val group = groupRepository.getGroup(params.groupId).first()
                ?: return Result.Error("Группа не найдена")

            if (!authRepository.hasSession()) return Result.Error("Не авторизован")

            val payment = Payment(
                id = UUID.randomUUID().toString(),
                groupId = params.groupId,
                payerId = params.payerId,
                receiverId = params.receiverId,
                amount = params.amount,
                currency = group.currency,
                status = PaymentStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            paymentRepository.createPayment(payment)

        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка создания платежа", e)
        }
    }
}