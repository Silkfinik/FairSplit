package com.silkfinik.fairsplit.features.account.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.model.User
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.silkfinik.fairsplit.core.database.AppDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Job

data class AccountUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val isNotificationsEnabled: Boolean = false
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val appDatabase: AppDatabase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()
    
    private var userJob: Job? = null

    init {
        loadUser()
    }

    private fun loadUser() {
        userJob?.cancel()
        userJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = authRepository.getUserId()
            if (userId != null) {
                userRepository.getUser(userId)
                    .catch { e -> 
                        emit(null) // Emit null on error to clear user
                    }
                    .collectLatest { user ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                user = user,
                                isNotificationsEnabled = user?.fcmToken != null
                            ) 
                        }
                    }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNameChange(newName: String) {
        viewModelScope.launch {
            val currentUser = _uiState.value.user ?: return@launch
            if (newName.isBlank() || newName == currentUser.displayName) return@launch

            val updatedUser = currentUser.copy(displayName = newName)
            userRepository.createOrUpdateUser(updatedUser)
        }
    }

    fun onToggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val userId = _uiState.value.user?.id ?: return@launch
            
            if (enabled) {
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    userRepository.updateFcmToken(userId, token)
                } catch (e: Exception) {
                    sendEvent(UiEvent.ShowSnackbar("Не удалось включить уведомления"))
                    _uiState.update { it.copy(isNotificationsEnabled = false) } // Revert
                }
            } else {
                userRepository.updateFcmToken(userId, null)
            }
        }
    }

    fun onSignOut() {
        // Stop listening to user updates immediately
        userJob?.cancel()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = _uiState.value.user?.id
            
            // Fire and forget FCM removal
            if (userId != null) {
                launch {
                    try {
                        userRepository.updateFcmToken(userId, null)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
            
            // Clear local database
            withContext(Dispatchers.IO) {
                appDatabase.clearAllTables()
            }
            
            authRepository.signOut()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun onDeleteAccount() {
        // Stop listening to user updates immediately
        userJob?.cancel()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = _uiState.value.user?.id
            
            if (userId != null) {
                 try {
                     userRepository.deleteUser(userId)
                 } catch (e: Exception) {
                     // Ignore errors, we want to sign out anyway
                 }
                 
                 // Clear local database
                 withContext(Dispatchers.IO) {
                     appDatabase.clearAllTables()
                 }
                 
                 authRepository.signOut() // Also sign out from auth
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
