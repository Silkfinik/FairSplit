package com.silkfinik.fairsplit.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionExtTest {

    @Test
    fun `asSafeMap returns empty map for null`() {
        val input: Any? = null
        val result = input.asSafeMap()
        assertEquals(emptyMap<String, Any>(), result)
    }

    @Test
    fun `asSafeMap returns empty map for non-map object`() {
        val input = "I am a string"
        val result = input.asSafeMap()
        assertEquals(emptyMap<String, Any>(), result)
    }

    @Test
    fun `asSafeMap filters non-string keys`() {
        val input = mapOf(
            "validKey" to "value",
            123 to "invalidKeyType",
            "anotherValid" to 100
        )
        val result = input.asSafeMap()
        assertEquals(2, result.size)
        assertEquals("value", result["validKey"])
        assertEquals(100, result["anotherValid"])
    }

    @Test
    fun `asSafeMap filters null values`() {
        val input = mapOf(
            "key1" to "value",
            "key2" to null
        )
        val result = input.asSafeMap()
        assertEquals(1, result.size)
        assertEquals("value", result["key1"])
    }
}
