package com.silkfinik.fairsplit.features.auth.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
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
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(EmailAuthUiState())
    val uiState: StateFlow<EmailAuthUiState> = _uiState.asStateFlow()

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
        val email = uiState.value.email
        val password = uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.signInWithEmail(email, password)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.Success)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun signUp() {
        val name = uiState.value.name
        val email = uiState.value.email
        val password = uiState.value.password
        val confirmPassword = uiState.value.confirmPassword

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все обязательные поля") }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Пароли не совпадают") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(error = "Пароль должен быть не менее 6 символов") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.signUpWithEmail(name, email, password)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.Success)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}