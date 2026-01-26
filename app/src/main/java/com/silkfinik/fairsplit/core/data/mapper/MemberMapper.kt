package com.silkfinik.fairsplit.core.data.mapper

import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import com.silkfinik.fairsplit.core.model.Member

fun MemberEntity.asDomainModel(): Member {
    return Member(
        id = id,
        groupId = groupId,
        name = name,
        isGhost = isGhost,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Member.asEntity(isDirty: Boolean = true): MemberEntity {
    return MemberEntity(
        id = id,
        groupId = groupId,
        name = name,
        isGhost = isGhost,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDirty = isDirty
    )
}
