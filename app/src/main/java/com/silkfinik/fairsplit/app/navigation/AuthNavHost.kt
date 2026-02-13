package com.silkfinik.fairsplit.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.silkfinik.fairsplit.features.auth.screen.LoginScreen
import com.silkfinik.fairsplit.features.auth.screen.RegisterScreen
import com.silkfinik.fairsplit.features.auth.screen.WelcomeScreen

private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
private const val AnimDuration = 500

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
        composable(
            route = Screen.Welcome.route,
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(AnimDuration, easing = EmphasizedEasing))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(AnimDuration, easing = EmphasizedEasing))
            }
        ) {
            WelcomeScreen(
                onContinue = onAnonymousLogin,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(
            route = Screen.Login.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(AnimDuration, easing = EmphasizedEasing))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(AnimDuration, easing = EmphasizedEasing))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(AnimDuration, easing = EmphasizedEasing))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(AnimDuration, easing = EmphasizedEasing))
            }
        ) {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { onAuthSuccess() }
            )
        }

        composable(
            route = Screen.Register.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(AnimDuration, easing = EmphasizedEasing))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(AnimDuration, easing = EmphasizedEasing))
            }
        ) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegistrationSuccess = { onAuthSuccess() }
            )
        }
    }
}