package com.silkfinik.fairsplit.features.account.viewmodel

import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.common.auth.GoogleSignInHelper
import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.data.preferences.AuthPreferences
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
import com.silkfinik.fairsplit.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val updateUserAvatarUseCase: UpdateUserAvatarUseCase = mockk()
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase = mockk()
    private val linkEmailAccountUseCase: LinkEmailAccountUseCase = mockk()
    private val signOutUseCase: SignOutUseCase = mockk()
    private val deleteAccountUseCase: DeleteAccountUseCase = mockk()
    private val googleSignInHelper: GoogleSignInHelper = mockk()
    private val authPreferences: AuthPreferences = mockk()
    private val validateEmailUseCase: ValidateEmailUseCase = mockk()
    private val validatePasswordUseCase: ValidatePasswordUseCase = mockk()

    private lateinit var viewModel: AccountViewModel

    @Before
    fun setUp() {
        // Mock init loadUser
        coEvery { authRepository.getUserId() } returns null
        coEvery { userRepository.getUser(any()) } returns emptyFlow()

        viewModel = AccountViewModel(
            authRepository,
            userRepository,
            updateUserAvatarUseCase,
            linkGoogleAccountUseCase,
            linkEmailAccountUseCase,
            signOutUseCase,
            deleteAccountUseCase,
            googleSignInHelper,
            authPreferences,
            validateEmailUseCase,
            validatePasswordUseCase
        )
    }

    @Test
    fun `onLinkEmailAccount with blank fields shows error`() = runTest {
        viewModel.onLinkEmailChange("")
        viewModel.onLinkPasswordChange("")
        viewModel.onLinkConfirmPasswordChange("")

        viewModel.onLinkEmailAccount()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.linkEmailError is UiText.StringResource)
        assertEquals(R.string.error_fill_all_fields, (state.linkEmailError as UiText.StringResource).resId)
        coVerify(exactly = 0) { linkEmailAccountUseCase(any(), any()) }
    }

    @Test
    fun `onLinkEmailAccount with invalid email shows error`() = runTest {
        viewModel.onLinkEmailChange("invalid-email")
        viewModel.onLinkPasswordChange("Password123!")
        viewModel.onLinkConfirmPasswordChange("Password123!")

        every { validateEmailUseCase("invalid-email") } returns false

        viewModel.onLinkEmailAccount()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.linkEmailError is UiText.StringResource)
        assertEquals(R.string.error_invalid_email, (state.linkEmailError as UiText.StringResource).resId)
        coVerify(exactly = 0) { linkEmailAccountUseCase(any(), any()) }
    }

    @Test
    fun `onLinkEmailAccount with mismatched passwords shows error`() = runTest {
        viewModel.onLinkEmailChange("test@test.com")
        viewModel.onLinkPasswordChange("Password123!")
        viewModel.onLinkConfirmPasswordChange("Password321!")

        every { validateEmailUseCase("test@test.com") } returns true

        viewModel.onLinkEmailAccount()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.linkPasswordError is UiText.StringResource)
        assertEquals(R.string.error_passwords_mismatch, (state.linkPasswordError as UiText.StringResource).resId)
        coVerify(exactly = 0) { linkEmailAccountUseCase(any(), any()) }
    }

    @Test
    fun `onLinkEmailAccount with short password shows error`() = runTest {
        viewModel.onLinkEmailChange("test@test.com")
        viewModel.onLinkPasswordChange("short")
        viewModel.onLinkConfirmPasswordChange("short")

        every { validateEmailUseCase("test@test.com") } returns true
        every { validatePasswordUseCase("short") } returns PasswordValidationResult.TOO_SHORT

        viewModel.onLinkEmailAccount()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.linkPasswordError is UiText.StringResource)
        assertEquals(R.string.error_password_too_short, (state.linkPasswordError as UiText.StringResource).resId)
        coVerify(exactly = 0) { linkEmailAccountUseCase(any(), any()) }
    }

    @Test
    fun `onLinkEmailAccount with valid data calls use case`() = runTest {
        viewModel.onLinkEmailChange("test@test.com")
        viewModel.onLinkPasswordChange("Password123!")
        viewModel.onLinkConfirmPasswordChange("Password123!")

        every { validateEmailUseCase("test@test.com") } returns true
        every { validatePasswordUseCase("Password123!") } returns PasswordValidationResult.SUCCESS
        coEvery { linkEmailAccountUseCase("test@test.com", "Password123!") } returns com.silkfinik.fairsplit.core.common.util.Result.Success(Unit)

        viewModel.onLinkEmailAccount()
        advanceUntilIdle()

        coVerify { linkEmailAccountUseCase("test@test.com", "Password123!") }
    }
}
