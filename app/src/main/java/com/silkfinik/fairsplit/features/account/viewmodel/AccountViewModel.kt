package com.silkfinik.fairsplit.features.account.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.common.auth.GoogleSignInHelper
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.common.util.asUiText
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.data.preferences.AuthPreferences
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.domain.usecase.auth.DeleteAccountUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.LinkEmailAccountUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.LinkGoogleAccountUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.SignOutUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.UpdateUserAvatarUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.account.ui.AccountUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val updateUserAvatarUseCase: UpdateUserAvatarUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
    private val linkEmailAccountUseCase: LinkEmailAccountUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val googleSignInHelper: GoogleSignInHelper,
    private val authPreferences: AuthPreferences
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

            val isVerified = if (isAnon) true else authRepository.isEmailVerified()

            if (userId != null) {
                userRepository.getUser(userId)
                    .catch { emit(null) }
                    .collectLatest { user ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = user,
                                isNotificationsEnabled = user?.fcmToken != null,
                                isAnonymous = isAnon,
                                isEmailVerified = isVerified
                            )
                        }
                    }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun checkVerificationStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            authRepository.reloadUser()
                .onSuccess {
                    val isVerified = authRepository.isEmailVerified()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEmailVerified = isVerified
                        )
                    }
                    if (isVerified) {
                        sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_email_verified)))
                    } else {
                        sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_email_not_verified)))
                    }
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            authRepository.sendEmailVerification()
                .onSuccess {
                    sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_verification_email_sent)))
                }
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }

    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            authRepository.updateEmail(newEmail)
                .onSuccess {
                    authPreferences.saveEmailForNextLogin(newEmail)
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_email_change_sent, newEmail)))
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }

    fun onAvatarSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            updateUserAvatarUseCase(uri.toString())
                .onSuccess {
                    sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_avatar_updated)))
                }
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onNameChange(newName: String) {
        viewModelScope.launch {
            val currentUser = _uiState.value.user ?: return@launch
            if (newName.isBlank() || newName == currentUser.displayName) return@launch

            val updatedUser = currentUser.copy(displayName = newName)

            userRepository.createOrUpdateUser(updatedUser)
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }

    fun onToggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val userId = _uiState.value.user?.id ?: return@launch

            if (enabled) {
                val token = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    sendEvent(UiEvent.ShowError(UiText.StringResource(R.string.error_fcm_token_get)))
                    _uiState.update { it.copy(isNotificationsEnabled = false) }
                    return@launch
                }

                userRepository.updateFcmToken(userId, token)
                    .onError { error ->
                        val uiError = if (error is AppError.Common) error.asUiText()
                        else UiText.StringResource(R.string.error_fcm_token_save)

                        sendEvent(UiEvent.ShowError(uiError))
                        _uiState.update { it.copy(isNotificationsEnabled = false) }
                    }
            } else {
                userRepository.updateFcmToken(userId, null)
                    .onError { error ->
                        sendEvent(UiEvent.ShowError(error.asUiText()))
                    }
            }
        }
    }

    fun onSignOut() {
        userJob?.cancel()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            signOutUseCase()
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onDeleteAccount() {
        userJob?.cancel()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            deleteAccountUseCase()
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun startGoogleAccountLink(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            googleSignInHelper.signIn(activityContext)
                .onSuccess { result ->
                    onLinkGoogleAccount(result.idToken)
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false) }

                    if (error !is AppError.Common.Cancelled) {
                        sendEvent(UiEvent.ShowError(error.asUiText()))
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
                    sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_google_linked)))
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }

    fun onLinkEmailAccount(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            linkEmailAccountUseCase(email, password)
                .onSuccess {
                    authRepository.sendEmailVerification()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAnonymous = false,
                            isEmailVerified = false
                        )
                    }
                    sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_email_linked)))
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }
}