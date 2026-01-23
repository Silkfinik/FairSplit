package com.silkfinik.fairsplit.app.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.silkfinik.fairsplit.app.navigation.AppNavHost
import com.silkfinik.fairsplit.core.ui.theme.FairSplitTheme
import com.silkfinik.fairsplit.features.auth.screen.WelcomeScreen
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
                        MainUiState.Loading -> LoadingScreen()
                        MainUiState.Success -> {
                            val navController = rememberNavController()
                            AppNavHost(navController = navController)
                        }
                        MainUiState.NeedsName -> {
                            WelcomeScreen(
                                onContinue = { viewModel.onNameEntered() }
                            )
                        }
                        MainUiState.ErrorNoInternet -> {
                            BlockingErrorScreen(
                                message = "Для первого запуска требуется интернет",
                                onRetry = { viewModel.retry() }
                            )
                        }
                        is MainUiState.ErrorAuthFailed -> {
                            BlockingErrorScreen(
                                message = state.message,
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
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}

@Composable
fun BlockingErrorScreen(message: String, onRetry: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Повторить")
            }
        }
    }
}
