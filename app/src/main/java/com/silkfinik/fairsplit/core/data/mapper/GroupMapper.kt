package com.silkfinik.fairsplit.core.data.mapper

import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.network.model.GroupDto

fun GroupEntity.asDomainModel(): Group {
    return Group(
        id = this.id,
        name = this.name,
        currency = this.currency
    )
}

fun GroupEntity.asDto(): GroupDto {
    return GroupDto(
        id = this.id,
        name = this.name,
        currency = this.currency.name,
        ownerId = this.ownerId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun GroupDto.asEntity(): GroupEntity {
    val currencyEnum = try {
        Currency.valueOf(this.currency)
    } catch (e: IllegalArgumentException) {
        Currency.USD
    }

    return GroupEntity(
        id = this.id,
        name = this.name,
        currency = currencyEnum,
        ownerId = this.ownerId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isDirty = false
    )
}
