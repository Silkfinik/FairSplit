package com.silkfinik.fairsplit.core.common.util

fun Any?.asSafeMap(): Map<String, Any> {
    return (this as? Map<*, *>)?.mapNotNull { (key, value) ->
        (key as? String)?.let { k ->
            value?.let { v -> k to v }
        }
    }?.toMap() ?: emptyMap()
}
