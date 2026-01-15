package com.silkfinik.fairsplit.app.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.NetworkMonitor
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
        checkAuthAndNetwork()
    }

    fun retry() {
        checkAuthAndNetwork()
    }

    private fun checkAuthAndNetwork() {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading

            if (authRepository.hasSession()) {
                val userId = authRepository.getUserId()
                if (userId != null) {
                    ensureUserProfile(userId)
                    groupRepository.startSync()
                    _uiState.value = MainUiState.Success
                    return@launch
                }
            }

            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    val result = authRepository.signInAnonymously()
                    if (result.isSuccess) {
                        val userId = authRepository.getUserId()
                        if (userId != null) {
                            ensureUserProfile(userId)
                            groupRepository.startSync()
                            _uiState.value = MainUiState.Success
                        } else {
                            _uiState.value = MainUiState.ErrorAuthFailed("UID is null after sign in")
                        }
                    } else {
                        _uiState.value = MainUiState.ErrorAuthFailed("Ошибка входа: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    _uiState.value = MainUiState.ErrorNoInternet
                }
            }
        }
    }

    private suspend fun ensureUserProfile(uid: String) {
        try {
            if (!userRepository.userExists(uid)) {
                val newUser = User(
                    id = uid,
                    isAnonymous = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userRepository.createOrUpdateUser(newUser)
            }
        } catch (e: Exception) {
            // Log error, but don't block app start. 
            // Ideally we should retry or show a non-blocking error.
            e.printStackTrace()
        }
    }
}