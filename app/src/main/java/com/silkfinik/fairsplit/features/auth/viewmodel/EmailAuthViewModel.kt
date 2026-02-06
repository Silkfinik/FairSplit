package com.silkfinik.fairsplit.features.auth.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.common.util.asUiText
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.usecase.auth.GetSavedEmailUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.SignInWithEmailUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.SignUpWithEmailUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.auth.ui.EmailAuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailAuthViewModel @Inject constructor(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val getSavedEmailUseCase: GetSavedEmailUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(EmailAuthUiState())
    val uiState: StateFlow<EmailAuthUiState> = _uiState.asStateFlow()

    init {
        checkSavedEmail()
    }

    private fun checkSavedEmail() {
        viewModelScope.launch {
            val savedEmail = getSavedEmailUseCase()
            if (!savedEmail.isNullOrBlank()) {
                _uiState.update { it.copy(email = savedEmail) }
            }
        }
    }

    fun onNameChange(newValue: String) {
        _uiState.update { it.copy(name = newValue, error = null) }
    }

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue, error = null) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue, error = null) }
    }

    fun onConfirmPasswordChange(newValue: String) {
        _uiState.update { it.copy(confirmPassword = newValue, error = null) }
    }

    fun signIn() {
        val email = uiState.value.email.trim()
        val password = uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_fill_all_fields)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            signInWithEmailUseCase(email, password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.Success)
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.asUiText()) }
                }
        }
    }

    fun signUp() {
        val name = uiState.value.name.trim()
        val email = uiState.value.email.trim()
        val password = uiState.value.password
        val confirmPassword = uiState.value.confirmPassword

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_fill_required_fields)) }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_passwords_mismatch)) }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_password_too_short)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            signUpWithEmailUseCase(name, email, password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.Success)
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.asUiText()) }
                }
        }
    }
}