package com.silkfinik.fairsplit.features.auth.viewmodel

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.auth.GoogleSignInHelper
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.domain.usecase.auth.LinkGoogleAccountUseCase
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

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val updateUserUseCase: UpdateUserUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
    private val googleSignInHelper: GoogleSignInHelper,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
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
                    } else {
                        authRepository.signInWithGoogle(credential.idToken)
                    }

                    result.onSuccess {
                        val uid = authRepository.getUserId()

                        if (uid != null && userRepository.userExists(uid)) {
                            _uiState.update { it.copy(isLoading = false) }
                            sendEvent(UiEvent.Success)
                        } else {
                            val name = credential.displayName
                            val photoUrl = credential.profilePictureUri?.toString()
                            val email = credential.id

                            val nameToUpdate = name ?: authRepository.getUserName() ?: "User"

                            updateUserUseCase(nameToUpdate, photoUrl, email)
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