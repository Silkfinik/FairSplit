package com.silkfinik.fairsplit.app.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.silkfinik.fairsplit.app.navigation.AppNavHost
import com.silkfinik.fairsplit.core.ui.component.FairSplitEmptyState
import com.silkfinik.fairsplit.core.ui.component.FairSplitLoader
import com.silkfinik.fairsplit.core.ui.component.FairSplitScaffold
import com.silkfinik.fairsplit.core.ui.theme.FairSplitTheme
import com.silkfinik.fairsplit.features.auth.screen.EmailVerificationScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FairSplitTheme {
                val uiState by viewModel.uiState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = uiState) {
                        MainUiState.Loading -> {
                            LoadingScreen()
                        }
                        MainUiState.Success -> {
                            val navController = rememberNavController()
                            AppNavHost(navController = navController)
                        }
                        MainUiState.Welcome -> {
                            com.silkfinik.fairsplit.app.navigation.AuthNavHost(
                                onAnonymousLogin = { viewModel.onNameEntered() },
                                onAuthSuccess = { viewModel.retry() }
                            )
                        }
                        MainUiState.EmailVerification -> {
                            EmailVerificationScreen(
                                onVerificationConfirmed = {
                                    viewModel.retry()
                                }
                            )
                        }
                        MainUiState.ErrorNoInternet -> {
                            BlockingErrorScreen(
                                title = "Нет интернета",
                                message = "Для первого запуска приложения требуется подключение к сети.",
                                icon = Icons.Default.CloudOff,
                                onRetry = { viewModel.retry() }
                            )
                        }
                        is MainUiState.ErrorAuthFailed -> {
                            BlockingErrorScreen(
                                title = "Ошибка входа",
                                message = state.message,
                                icon = Icons.Default.ErrorOutline,
                                onRetry = { viewModel.retry() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    FairSplitScaffold {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            FairSplitLoader(modifier = Modifier.size(64.dp))
        }
    }
}

@Composable
fun BlockingErrorScreen(
    title: String,
    message: String,
    icon: ImageVector,
    onRetry: () -> Unit
) {
    FairSplitScaffold { padding ->
        FairSplitEmptyState(
            modifier = Modifier.padding(padding),
            icon = icon,
            title = title,
            description = message,
            actionLabel = "Повторить попытку",
            onActionClick = onRetry
        )
    }
}