package com.silkfinik.fairsplit.core.model.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseCategoryTest {

    @Test
    fun `fromId returns correct category for valid ids`() {
        assertEquals(ExpenseCategory.GROCERIES, ExpenseCategory.fromId("groceries"))
        assertEquals(ExpenseCategory.TRANSPORT, ExpenseCategory.fromId("transport"))
        assertEquals(ExpenseCategory.EATING_OUT, ExpenseCategory.fromId("eating_out"))
    }

    @Test
    fun `fromId returns OTHER for unknown id`() {
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromId("unknown_id_xyz"))
    }

    @Test
    fun `fromId returns OTHER for null id`() {
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromId(null))
    }
}
