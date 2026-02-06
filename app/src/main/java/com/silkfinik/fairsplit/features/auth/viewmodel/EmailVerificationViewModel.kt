package com.silkfinik.fairsplit.features.auth.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.usecase.auth.CheckEmailVerificationUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.SendVerificationEmailUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.SignOutUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.auth.ui.EmailVerificationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val sendVerificationEmailUseCase: SendVerificationEmailUseCase,
    private val checkEmailVerificationUseCase: CheckEmailVerificationUseCase,
    private val signOutUseCase: SignOutUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    fun sendVerificationEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }

            sendVerificationEmailUseCase()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Письмо отправлено! Проверьте папку Спам."
                        )
                    }
                }
                .onError { message, _ ->
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
        }
    }

    fun checkVerificationStatus(onVerified: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            checkEmailVerificationUseCase()
                .onSuccess { isVerified ->
                    if (isVerified) {
                        _uiState.update { it.copy(isLoading = false) }
                        onVerified()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Почта еще не подтверждена. Попробуйте обновить страницу после перехода по ссылке."
                            )
                        }
                    }
                }
                .onError { message, _ ->
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
        }
    }
}