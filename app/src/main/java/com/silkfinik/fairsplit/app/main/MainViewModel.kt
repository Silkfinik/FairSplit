package com.silkfinik.fairsplit.app.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.NetworkMonitor
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUserId.collectLatest { userId ->
                if (userId != null) {
                    handleAuthenticatedUser()
                } else {
                    groupRepository.stopSync()
                    _uiState.value = MainUiState.Welcome
                }
            }
        }
    }
    
    fun retry() {
        viewModelScope.launch {
            if (authRepository.hasSession()) {
                handleAuthenticatedUser()
            } else {
                _uiState.value = MainUiState.Welcome
            }
        }
    }

    fun onNameEntered() {
        _uiState.value = MainUiState.Success
    }

    private suspend fun handleAuthenticatedUser() {
        val userId = authRepository.getUserId()
        if (userId != null) {
            authRepository.reloadUser()

            val isAnonymous = authRepository.isAnonymous()
            val isVerified = authRepository.isEmailVerified()

            if (isAnonymous || isVerified) {
                val name = authRepository.getUserName()
                groupRepository.startSync()

                if (name.isNullOrBlank()) {
                    _uiState.value = MainUiState.Welcome
                } else {
                    _uiState.value = MainUiState.Success
                }
            } else {
                _uiState.value = MainUiState.EmailVerification
            }
        } else {
            _uiState.value = MainUiState.Welcome
        }
    }
}