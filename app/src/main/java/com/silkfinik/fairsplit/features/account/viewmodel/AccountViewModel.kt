package com.silkfinik.fairsplit.features.account.viewmodel

import android.content.Context
import android.net.Uri
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
import com.silkfinik.fairsplit.core.common.auth.GoogleSignInHelper
import com.silkfinik.fairsplit.core.domain.usecase.auth.LinkEmailAccountUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.LinkGoogleAccountUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.UpdateUserAvatarUseCase
import com.silkfinik.fairsplit.features.account.ui.AccountUiState
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Job

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val appDatabase: AppDatabase,
    private val updateUserAvatarUseCase: UpdateUserAvatarUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
    private val linkEmailAccountUseCase: LinkEmailAccountUseCase,
    private val googleSignInHelper: GoogleSignInHelper
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
            val isAnon = authRepository.isAnonymous()

            if (userId != null) {
                userRepository.getUser(userId)
                    .catch { e ->
                        emit(null)
                    }
                    .collectLatest { user ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = user,
                                isNotificationsEnabled = user?.fcmToken != null,
                                isAnonymous = isAnon
                            )
                        }
                    }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onAvatarSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            updateUserAvatarUseCase(uri.toString())
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar("Аватар успешно обновлен"))
                }
                .onError { message, _ ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar("Ошибка: $message"))
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
        userJob?.cancel()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = _uiState.value.user?.id

            if (userId != null) {
                launch {
                    try {
                        userRepository.updateFcmToken(userId, null)
                    } catch (e: Exception) {

                    }
                }
            }

            withContext(Dispatchers.IO) {
                appDatabase.clearAllTables()
            }
            
            authRepository.signOut()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun onDeleteAccount() {
        userJob?.cancel()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = _uiState.value.user?.id
            
            if (userId != null) {
                 try {
                     userRepository.deleteUser(userId)
                 } catch (e: Exception) {

                 }

                 withContext(Dispatchers.IO) {
                     appDatabase.clearAllTables()
                 }
                 
                 authRepository.signOut()
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun startGoogleAccountLink(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val signInResult = googleSignInHelper.signIn(activityContext)) {
                is Result.Success -> {
                    val idToken = signInResult.data.idToken

                    linkGoogleAccountUseCase(idToken)
                        .onSuccess {
                            _uiState.update { it.copy(isLoading = false, isAnonymous = false) }
                            sendEvent(UiEvent.ShowSnackbar("Google аккаунт успешно привязан"))
                        }
                        .onError { message, _ ->
                            _uiState.update { it.copy(isLoading = false) }
                            sendEvent(UiEvent.ShowSnackbar("Ошибка привязки: $message"))
                        }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    if (!signInResult.message.contains("отменен", ignoreCase = true)) {
                        sendEvent(UiEvent.ShowSnackbar(signInResult.message))
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun onLinkGoogleAccount(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            linkGoogleAccountUseCase(idToken)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isAnonymous = false) }
                    sendEvent(UiEvent.ShowSnackbar("Google аккаунт успешно привязан"))
                }
                .onError { message, _ ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar("Ошибка: $message"))
                }
        }
    }

    fun onLinkEmailAccount(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            linkEmailAccountUseCase(email, password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isAnonymous = false) }
                    sendEvent(UiEvent.ShowSnackbar("Почта успешно привязана"))
                }
                .onError { message, _ ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar("Ошибка: $message"))
                }
        }
    }
}
