package com.silkfinik.fairsplit.features.auth.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.domain.usecase.auth.UpdateUserNameUseCase
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
import com.silkfinik.fairsplit.core.common.auth.GoogleSignInHelper
import com.silkfinik.fairsplit.core.domain.usecase.auth.LinkGoogleAccountUseCase

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val updateUserNameUseCase: UpdateUserNameUseCase,
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
            val credential = googleSignInHelper.signIn(context)
            if (credential != null) {
                val result = linkGoogleAccountUseCase(credential.idToken)
                when (result) {
                    is Result.Success -> {
                        val name = credential.displayName
                        if (name != null) {
                            updateUserNameUseCase(name)
                        }
                        _uiState.update { it.copy(isLoading = false, isSaved = true) }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        sendEvent(UiEvent.ShowSnackbar(result.message))
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                // Cancelled or failed
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
            val result = updateUserNameUseCase(name)
            
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                    // Navigation will be handled by UI observing isSaved or sending event
                    // We can also send a one-time event
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }
}
