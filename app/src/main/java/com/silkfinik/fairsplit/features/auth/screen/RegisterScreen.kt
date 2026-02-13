package com.silkfinik.fairsplit.features.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitPasswordField
import com.silkfinik.fairsplit.core.ui.component.FairSplitScaffold
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.features.auth.viewmodel.EmailAuthViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    viewModel: EmailAuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val passwordState = rememberTextFieldState(initialText = state.password)
    val confirmPasswordState = rememberTextFieldState(initialText = state.confirmPassword)

    LaunchedEffect(passwordState) {
        snapshotFlow { passwordState.text }
            .collectLatest { newPassword ->
                if (newPassword.toString() != state.password) {
                    viewModel.onPasswordChange(newPassword.toString())
                }
            }
    }

    LaunchedEffect(confirmPasswordState) {
        snapshotFlow { confirmPasswordState.text }
            .collectLatest { newConfirm ->
                if (newConfirm.toString() != state.confirmPassword) {
                    viewModel.onConfirmPasswordChange(newConfirm.toString())
                }
            }
    }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState
    ) { event ->
        if (event is UiEvent.Success) {
            onRegistrationSuccess()
        }
    }

    FairSplitScaffold(
        topBar = {
            FairSplitTopAppBar(
                title = "Регистрация",
                onBackClick = onNavigateBack
            )
        },
        snackbarHostState = snackbarHostState,
        isLoading = state.isLoading
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.1f))

            Text(
                text = "Новый аккаунт",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Заполните форму, чтобы начать",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            FairSplitCard {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FairSplitTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = "Имя",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FairSplitTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = "Email",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FairSplitPasswordField(
                        state = passwordState,
                        label = "Пароль",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FairSplitPasswordField(
                        state = confirmPasswordState,
                        label = "Повторите пароль",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    )

                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.error!!.asString(context),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    FairSplitButton(
                        text = "Создать аккаунт",
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.signUp()
                        },
                        isLoading = state.isLoading,
                        enabled = state.name.isNotBlank() &&
                                state.email.isNotBlank() &&
                                state.password.isNotBlank() &&
                                state.confirmPassword.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.9f))
        }
    }
}