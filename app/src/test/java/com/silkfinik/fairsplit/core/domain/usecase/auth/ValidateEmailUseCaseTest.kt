package com.silkfinik.fairsplit.core.domain.usecase.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateEmailUseCaseTest {

    private lateinit var validateEmailUseCase: ValidateEmailUseCase

    @Before
    fun setUp() {
        validateEmailUseCase = ValidateEmailUseCase()
    }

    @Test
    fun `test valid email`() {
        assertTrue(validateEmailUseCase("test@example.com"))
        assertTrue(validateEmailUseCase("user.name+tag@sub.domain.org"))
        assertTrue(validateEmailUseCase("123@123.com"))
    }

    @Test
    fun `test empty email is invalid`() {
        assertFalse(validateEmailUseCase(""))
        assertFalse(validateEmailUseCase("   "))
    }

    @Test
    fun `test email without @ is invalid`() {
        assertFalse(validateEmailUseCase("testexample.com"))
    }

    @Test
    fun `test email without domain is invalid`() {
        assertFalse(validateEmailUseCase("test@"))
    }

    @Test
    fun `test email with invalid domain is invalid`() {
        assertFalse(validateEmailUseCase("test@example"))
        assertFalse(validateEmailUseCase("test@example.c"))
    }

    @Test
    fun `test email starting with invalid character`() {
        assertFalse(validateEmailUseCase("@example.com"))
    }
}
