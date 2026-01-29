package com.silkfinik.fairsplit.features.auth.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
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

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val updateUserUseCase: UpdateUserUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
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
            val signInResult = googleSignInHelper.signIn(context)
            when (signInResult) {
                is Result.Success -> {
                    val credential = signInResult.data
                    val result = linkGoogleAccountUseCase(credential.idToken)
                    when (result) {
                        is Result.Success -> {
                            val name = credential.displayName
                            val photoUrl = credential.profilePictureUri?.toString()
                            if (name != null) {
                                updateUserUseCase(name, photoUrl)
                            }
                            _uiState.update { it.copy(isLoading = false, isSaved = true) }
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(isLoading = false) }
                            sendEvent(UiEvent.ShowSnackbar(result.message))
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    if (signInResult.exception !is GetCredentialCancellationException) {
                        sendEvent(UiEvent.ShowSnackbar(signInResult.message))
                    }
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
            val result = updateUserUseCase(name)
            
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }
}
