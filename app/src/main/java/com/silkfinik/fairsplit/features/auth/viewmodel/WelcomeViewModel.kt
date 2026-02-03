package com.silkfinik.fairsplit.features.auth.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.usecase.auth.UpdateUserUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.auth.ui.WelcomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.silkfinik.fairsplit.core.common.auth.GoogleSignInHelper
import com.silkfinik.fairsplit.core.domain.usecase.auth.LinkGoogleAccountUseCase
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val updateUserUseCase: UpdateUserUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
    private val googleSignInHelper: GoogleSignInHelper,
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }
    
    fun onGoogleSignInClick(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            googleSignInHelper.signIn(context)
                .onSuccess { credential ->
                    val result = if (authRepository.hasSession()) {
                        linkGoogleAccountUseCase(credential.idToken)
                    }
                    else {
                        authRepository.signInWithGoogle(credential.idToken)
                    }
                    
                    result.onSuccess {
                        val name = credential.displayName
                        val photoUrl = credential.profilePictureUri?.toString()
                        // Use id (which is email for GoogleIdTokenCredential often, but strictly strictly strictly id is subject)
                        // Actually, credential.id is the email in many cases for GoogleIdTokenCredential if set so.
                        // But let's check GoogleSignInHelper again.
                        // GoogleIdTokenCredential has `id` which IS the email address usually.
                        val email = credential.id
                        
                        if (name != null) {
                            updateUserUseCase(name, photoUrl, email)
                        } else {
                            val currentName = authRepository.getUserName() ?: "User"
                            updateUserUseCase(currentName, photoUrl, email)
                        }
                        _uiState.update { it.copy(isLoading = false) }
                        sendEvent(UiEvent.Success)
                    }
                    .onError { message, _ ->
                        _uiState.update { it.copy(isLoading = false) }
                        sendEvent(UiEvent.ShowSnackbar(message))
                    }
                }
                .onError { message, exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    if (exception !is GetCredentialCancellationException) {
                        sendEvent(UiEvent.ShowSnackbar(message))
                    }
                }
        }
    }

    fun onContinueClick() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(nameError = "Пожалуйста, введите имя") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            if (!authRepository.hasSession()) {
                val signInResult = authRepository.signInAnonymously()
                if (signInResult is Result.Error) {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar("Не удалось создать профиль: ${signInResult.message}"))
                    return@launch
                }
            }

            updateUserUseCase(name)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.Success)
                }
                .onError { message, _ ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(message))
                }
        }
    }
}
