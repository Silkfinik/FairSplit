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
import com.silkfinik.fairsplit.core.domain.usecase.auth.PasswordValidationResult
import com.silkfinik.fairsplit.core.domain.usecase.auth.SignOutUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.UpdateUserAvatarUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.ValidateEmailUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.ValidatePasswordUseCase
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
    private val authPreferences: AuthPreferences,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase
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
                    .catch { emit(null) }
                    .collectLatest { user ->
                        val authEmail = authRepository.getUserEmail()
                        val dbEmail = user?.email
                        val isAnonNow = authRepository.isAnonymous()
                        val isPendingEmailVerification = dbEmail != null && authEmail != null && !dbEmail.equals(authEmail, ignoreCase = true)
                        val currentVerifiedStatus = if (isPendingEmailVerification) false else (if (isAnonNow) true else authRepository.isEmailVerified())

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = user,
                                isNotificationsEnabled = user?.fcmToken != null,
                                isAnonymous = isAnonNow,
                                isEmailVerified = currentVerifiedStatus
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
                    val authEmail = authRepository.getUserEmail()
                    val dbEmail = _uiState.value.user?.email
                    
                    val isPendingEmailVerification = dbEmail != null && authEmail != null && !dbEmail.equals(authEmail, ignoreCase = true)
                    val isVerified = if (isPendingEmailVerification) false else authRepository.isEmailVerified()
                    
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
            val dbEmail = _uiState.value.user?.email
            val authEmail = authRepository.getUserEmail()

            if (dbEmail != null && authEmail != null && !dbEmail.equals(authEmail, ignoreCase = true)) {
                authRepository.updateEmail(dbEmail)
                    .onSuccess {
                        sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_verification_email_sent)))
                    }
                    .onError { error ->
                        sendEvent(UiEvent.ShowError(error.asUiText()))
                    }
            } else {
                authRepository.sendEmailVerification()
                    .onSuccess {
                        sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_verification_email_sent)))
                    }
                    .onError { error ->
                        sendEvent(UiEvent.ShowError(error.asUiText()))
                    }
            }
        }
    }

    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            authRepository.updateEmail(newEmail)
                .onSuccess {
                    val currentUser = _uiState.value.user
                    if (currentUser != null) {
                        userRepository.createOrUpdateUser(currentUser.copy(email = newEmail))
                    }
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

    fun showLinkEmailSheet(show: Boolean) {
        _uiState.update { 
            it.copy(
                isLinkSheetVisible = show,
                linkEmailError = null,
                linkPasswordError = null,
                linkEmail = if (show) "" else it.linkEmail,
                linkPassword = if (show) "" else it.linkPassword,
                linkConfirmPassword = if (show) "" else it.linkConfirmPassword
            ) 
        }
    }

    fun onLinkEmailChange(email: String) {
        _uiState.update { it.copy(linkEmail = email, linkEmailError = null) }
    }

    fun onLinkPasswordChange(password: String) {
        _uiState.update { it.copy(linkPassword = password, linkPasswordError = null) }
    }

    fun onLinkConfirmPasswordChange(password: String) {
        _uiState.update { it.copy(linkConfirmPassword = password, linkPasswordError = null) }
    }

    fun onLinkEmailAccount() {
        val state = _uiState.value
        val email = state.linkEmail.trim()
        val password = state.linkPassword
        val confirmPassword = state.linkConfirmPassword

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(linkEmailError = UiText.StringResource(R.string.error_fill_all_fields)) }
            return
        }

        if (!validateEmailUseCase(email)) {
            _uiState.update { it.copy(linkEmailError = UiText.StringResource(R.string.error_invalid_email)) }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(linkPasswordError = UiText.StringResource(R.string.error_passwords_mismatch)) }
            return
        }

        val validationResult = validatePasswordUseCase(password)
        if (validationResult != PasswordValidationResult.SUCCESS) {
            val errorStringRes = when (validationResult) {
                PasswordValidationResult.TOO_SHORT -> R.string.error_password_too_short
                PasswordValidationResult.NO_UPPERCASE -> R.string.error_password_no_uppercase
                PasswordValidationResult.NO_LOWERCASE -> R.string.error_password_no_lowercase
                PasswordValidationResult.NO_DIGIT -> R.string.error_password_no_digit
                PasswordValidationResult.SUCCESS -> return
            }
            _uiState.update { it.copy(linkPasswordError = UiText.StringResource(errorStringRes)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            linkEmailAccountUseCase(email, password)
                .onSuccess {
                    authRepository.sendEmailVerification()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAnonymous = false,
                            isEmailVerified = false,
                            isLinkSheetVisible = false,
                            linkEmail = "",
                            linkPassword = "",
                            linkConfirmPassword = ""
                        )
                    }
                    sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_email_linked)))
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false, isLinkSheetVisible = false) }
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }
}