package com.silkfinik.fairsplit.features.auth.viewmodel

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.auth.GoogleSignInHelper
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.usecase.auth.HandleGoogleSignInUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.SignInAnonymouslyUseCase
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
    private val signInAnonymouslyUseCase: SignInAnonymouslyUseCase,
    private val handleGoogleSignInUseCase: HandleGoogleSignInUseCase,
    private val googleSignInHelper: GoogleSignInHelper
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
                    handleGoogleSignInUseCase(
                        idToken = credential.idToken,
                        email = credential.id,
                        displayName = credential.displayName,
                        photoUrl = credential.profilePictureUri?.toString()
                    )
                        .onSuccess {
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

            signInAnonymouslyUseCase(name)
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