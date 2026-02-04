package com.silkfinik.fairsplit.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.silkfinik.fairsplit.features.auth.screen.LoginScreen
import com.silkfinik.fairsplit.features.auth.screen.RegisterScreen
import com.silkfinik.fairsplit.features.auth.screen.WelcomeScreen

@Composable
fun AuthNavHost(
    onAnonymousLogin: () -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route,
        modifier = modifier
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onContinue = onAnonymousLogin,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { onAuthSuccess() }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegistrationSuccess = { onAuthSuccess() }
            )
        }
    }
}