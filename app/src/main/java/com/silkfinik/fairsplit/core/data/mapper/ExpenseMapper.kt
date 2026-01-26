package com.silkfinik.fairsplit.core.data.mapper

import com.silkfinik.fairsplit.core.database.entity.ExpenseEntity
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.network.model.ExpenseDto

fun ExpenseEntity.asDomainModel(): Expense {
    return Expense(
        id = id,
        groupId = groupId,
        description = description,
        amount = amount,
        currency = currency,
        date = date,
        creatorId = creatorId,
        payers = payers,
        splits = splits,
        category = category,
        isDeleted = isDeleted,
        isMathValid = isMathValid,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Expense.asEntity(isDirty: Boolean = true): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        groupId = groupId,
        description = description,
        amount = amount,
        currency = currency,
        date = date,
        creatorId = creatorId,
        payers = payers,
        splits = splits,
        category = category,
        isDeleted = isDeleted,
        isMathValid = isMathValid,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDirty = isDirty
    )
}

fun ExpenseEntity.asDto(): ExpenseDto {
    return ExpenseDto(
        id = id,
        description = description,
        amount = amount,
        currency = currency.name,
        date = date,
        creatorId = creatorId,
        payers = payers,
        splits = splits,
        category = category,
        isDeleted = isDeleted,
        isMathValid = isMathValid,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ExpenseDto.asEntity(groupId: String): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        groupId = groupId,
        description = description,
        amount = amount,
        currency = try { Currency.valueOf(currency) } catch (e: Exception) { Currency.USD },
        date = date,
        creatorId = creatorId,
        payers = payers,
        splits = splits,
        category = category,
        isDeleted = isDeleted,
        isMathValid = isMathValid,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDirty = false
    )
}
