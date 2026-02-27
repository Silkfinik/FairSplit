package com.silkfinik.fairsplit.core.domain.usecase.auth

import javax.inject.Inject

enum class PasswordValidationResult {
    SUCCESS,
    TOO_SHORT,
    NO_UPPERCASE,
    NO_LOWERCASE,
    NO_DIGIT
}

class ValidatePasswordUseCase @Inject constructor() {
    operator fun invoke(password: String): PasswordValidationResult {
        if (password.length < 8) return PasswordValidationResult.TOO_SHORT
        if (!password.any { it.isUpperCase() }) return PasswordValidationResult.NO_UPPERCASE
        if (!password.any { it.isLowerCase() }) return PasswordValidationResult.NO_LOWERCASE
        if (!password.any { it.isDigit() }) return PasswordValidationResult.NO_DIGIT
        return PasswordValidationResult.SUCCESS
    }
}
