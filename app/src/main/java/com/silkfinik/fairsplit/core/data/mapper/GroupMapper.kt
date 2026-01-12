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
        ownerId = this.owner_id,
        createdAt = this.created_at,
        updatedAt = this.updated_at
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
        owner_id = this.ownerId,
        created_at = this.createdAt,
        updated_at = this.updatedAt,
        is_dirty = false
    )
}
