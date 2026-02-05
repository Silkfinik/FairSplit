package com.silkfinik.fairsplit.core.data.sync.listener

abstract class BaseFirestoreListener {

    protected fun shouldUpdate(
        localEntityExists: Boolean,
        localIsDirty: Boolean,
        localUpdatedAt: Long,
        serverUpdatedAt: Long
    ): Boolean {
        if (!localEntityExists) return true
        if (localIsDirty) return false
        return serverUpdatedAt > localUpdatedAt
    }
}