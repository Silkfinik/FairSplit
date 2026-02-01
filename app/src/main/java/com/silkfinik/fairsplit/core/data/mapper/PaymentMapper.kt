package com.silkfinik.fairsplit.core.data.mapper

import com.silkfinik.fairsplit.core.database.entity.PaymentEntity
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Payment
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
import com.silkfinik.fairsplit.core.network.model.PaymentDto

fun PaymentEntity.asDomainModel(): Payment {
    return Payment(
        id = id,
        groupId = groupId,
        payerId = payerId,
        receiverId = receiverId,
        amount = amount,
        currency = currency,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Payment.asEntity(isDirty: Boolean = true): PaymentEntity {
    return PaymentEntity(
        id = id,
        groupId = groupId,
        payerId = payerId,
        receiverId = receiverId,
        amount = amount,
        currency = currency,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDirty = isDirty
    )
}

fun PaymentEntity.asDto(): PaymentDto {
    return PaymentDto(
        id = id,
        payerId = payerId,
        receiverId = receiverId,
        amount = amount,
        currency = currency.name,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun PaymentDto.asEntity(groupId: String): PaymentEntity {
    return PaymentEntity(
        id = id,
        groupId = groupId,
        payerId = payerId,
        receiverId = receiverId,
        amount = amount,
        currency = try { Currency.valueOf(currency) } catch (e: Exception) { Currency.USD },
        status = try { PaymentStatus.valueOf(status) } catch (e: Exception) { PaymentStatus.PENDING },
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDirty = false
    )
}
