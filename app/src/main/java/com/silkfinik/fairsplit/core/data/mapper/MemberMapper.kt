package com.silkfinik.fairsplit.core.data.mapper

import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import com.silkfinik.fairsplit.core.model.Member

fun MemberEntity.asDomainModel(): Member {
    return Member(
        id = id,
        groupId = group_id,
        name = name,
        isGhost = is_ghost,
        createdAt = created_at,
        updatedAt = updated_at
    )
}

fun Member.asEntity(isDirty: Boolean = true): MemberEntity {
    return MemberEntity(
        id = id,
        group_id = groupId,
        name = name,
        is_ghost = isGhost,
        created_at = createdAt,
        updated_at = updatedAt,
        is_dirty = isDirty
    )
}
