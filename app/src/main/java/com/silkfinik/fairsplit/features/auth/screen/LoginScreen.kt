package com.silkfinik.fairsplit.features.auth.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.ui.component.FairSplitButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitButtonStyle
import com.silkfinik.fairsplit.core.ui.component.FairSplitPasswordField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.features.auth.viewmodel.EmailAuthViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: EmailAuthViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    // 1. Создаем состояние для нового компонента ввода пароля
    val passwordState = rememberTextFieldState(initialText = state.password)

    // 2. Синхронизация: Когда меняется текст в поле (TextFieldState) -> обновляем ViewModel
    LaunchedEffect(passwordState) {
        snapshotFlow { passwordState.text }
            .collectLatest { newPassword ->
                // Проверка, чтобы избежать зацикливания, если текст совпадает
                if (newPassword.toString() != state.password) {
                    viewModel.onPasswordChange(newPassword.toString())
                }
            }
    }

    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            if (event is UiEvent.Success) {
                onLoginSuccess()
            }
        }
    }

    Scaffold(
        topBar = {
            FairSplitTopAppBar(
                title = "Вход",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Обычное поле (Email)
            FairSplitTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Expressive Password Field
            // Вся логика скрытия (Shapes), иконок и анимаций теперь внутри компонента
            FairSplitPasswordField(
                state = passwordState,
                label = "Пароль",
                modifier = Modifier.fillMaxWidth(),
                isError = state.error != null,
                enabled = !state.isLoading
            )

            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error!!.asString(context),
                    color = MaterialTheme.colorScheme.error, // Используем тему вместо Color.Red
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            FairSplitButton(
                text = "Войти",
                onClick = viewModel::signIn,
                isLoading = state.isLoading,
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            FairSplitButton(
                text = "Нет аккаунта? Зарегистрироваться",
                onClick = onNavigateToRegister,
                style = FairSplitButtonStyle.Text,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}