package com.silkfinik.fairsplit.core.domain.usecase.auth

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ValidatePasswordUseCaseTest {

    private lateinit var validatePasswordUseCase: ValidatePasswordUseCase

    @Before
    fun setUp() {
        validatePasswordUseCase = ValidatePasswordUseCase()
    }

    @Test
    fun `password too short returns TOO_SHORT`() {
        val result = validatePasswordUseCase("A1a")
        assertEquals(PasswordValidationResult.TOO_SHORT, result)
    }

    @Test
    fun `password without uppercase returns NO_UPPERCASE`() {
        val result = validatePasswordUseCase("abcdefg1")
        assertEquals(PasswordValidationResult.NO_UPPERCASE, result)
    }

    @Test
    fun `password without lowercase returns NO_LOWERCASE`() {
        val result = validatePasswordUseCase("ABCDEFG1")
        assertEquals(PasswordValidationResult.NO_LOWERCASE, result)
    }

    @Test
    fun `password without digit returns NO_DIGIT`() {
        val result = validatePasswordUseCase("Abcdefgh")
        assertEquals(PasswordValidationResult.NO_DIGIT, result)
    }

    @Test
    fun `valid password returns SUCCESS`() {
        val result = validatePasswordUseCase("Abcdefg1")
        assertEquals(PasswordValidationResult.SUCCESS, result)
    }
}
