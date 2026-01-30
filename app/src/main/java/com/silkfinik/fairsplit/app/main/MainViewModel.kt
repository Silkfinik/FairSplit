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
            authRepository.currentUserId.collect { userId ->
                if (userId != null) {
                    handleAuthenticatedUser()
                } else {
                    checkAuthAndNetwork()
                }
            }
        }
    }

    fun retry() {
        checkAuthAndNetwork()
    }

    fun onNameEntered() {
        _uiState.value = MainUiState.Success
    }

    private fun checkAuthAndNetwork() {
        viewModelScope.launch {
            if (authRepository.hasSession()) {
                return@launch
            }

            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    authRepository.signInAnonymously()
                        .onError { message, _ ->
                            _uiState.value = MainUiState.ErrorAuthFailed("Ошибка входа: $message")
                        }
                } else {
                    _uiState.value = MainUiState.ErrorNoInternet
                }
            }
        }
    }

    private suspend fun handleAuthenticatedUser() {
        val userId = authRepository.getUserId()
        if (userId != null) {
            val name = authRepository.getUserName()
            val isAnonymous = authRepository.isAnonymous()

            if (!isAnonymous || !name.isNullOrBlank()) {
                ensureUserProfile(userId)
            }
            
            groupRepository.startSync()
            
            if (name.isNullOrBlank()) {
                _uiState.value = MainUiState.NeedsName
            } else {
                _uiState.value = MainUiState.Success
            }
        } else {
            _uiState.value = MainUiState.ErrorAuthFailed("UID is null after sign in")
        }
    }

    private suspend fun ensureUserProfile(uid: String) {
        try {
            if (!userRepository.userExists(uid)) {
                val newUser = User(
                    id = uid,
                    isAnonymous = authRepository.isAnonymous(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userRepository.createOrUpdateUser(newUser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
