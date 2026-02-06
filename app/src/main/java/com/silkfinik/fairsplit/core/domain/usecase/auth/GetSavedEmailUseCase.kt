package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.data.preferences.AuthPreferences
import javax.inject.Inject

class GetSavedEmailUseCase @Inject constructor(
    private val authPreferences: AuthPreferences
) {
    suspend operator fun invoke(): String? {
        return authPreferences.getAndClearEmail()
    }
}