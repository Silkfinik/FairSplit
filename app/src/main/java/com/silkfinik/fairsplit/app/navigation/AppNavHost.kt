package com.silkfinik.fairsplit.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.silkfinik.fairsplit.features.account.screen.AccountScreen
import com.silkfinik.fairsplit.features.expenses.screen.CreateExpenseScreen
import com.silkfinik.fairsplit.features.expenses.screen.ExpenseHistoryScreen
import com.silkfinik.fairsplit.features.groupdetails.screen.GroupDetailsScreen
import com.silkfinik.fairsplit.features.groups.screen.CreateGroupScreen
import com.silkfinik.fairsplit.features.groups.screen.GroupsListScreen
import com.silkfinik.fairsplit.features.members.screen.MembersScreen
import com.silkfinik.fairsplit.features.payments.screen.CreatePaymentScreen

// Emphasized easing for MD3 Expressive motion
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
private const val AnimDuration = 400

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.GroupsList.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.GroupsList.route,
            enterTransition = { fadeIn(tween(AnimDuration)) },
            exitTransition = {
                fadeOut(tween(AnimDuration)) + scaleOut(targetScale = 0.92f, animationSpec = tween(AnimDuration, easing = EmphasizedEasing))
            },
            popEnterTransition = {
                fadeIn(tween(AnimDuration)) + scaleIn(initialScale = 0.92f, animationSpec = tween(AnimDuration, easing = EmphasizedEasing))
            },
            popExitTransition = { fadeOut(tween(AnimDuration)) }
        ) {
            GroupsListScreen(
                onNavigateToCreateGroup = {
                    navController.navigate(Screen.CreateGroup.route)
                },
                onNavigateToGroupDetails = { groupId ->
                    navController.navigate(Screen.GroupDetails.createRoute(groupId))
                },
                onNavigateToAccount = {
                    navController.navigate(Screen.Account.route)
                }
            )
        }

        composable(
            route = Screen.Account.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(AnimDuration, easing = EmphasizedEasing))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(AnimDuration, easing = EmphasizedEasing))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(AnimDuration, easing = EmphasizedEasing))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(AnimDuration, easing = EmphasizedEasing))
            }
        ) {
            AccountScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CreateGroup.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(AnimDuration, easing = EmphasizedEasing))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(AnimDuration, easing = EmphasizedEasing))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(AnimDuration, easing = EmphasizedEasing))
            }
        ) {
            CreateGroupScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.GroupDetails.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(AnimDuration, easing = EmphasizedEasing))
            },
            exitTransition = {
                fadeOut(tween(AnimDuration)) + scaleOut(targetScale = 0.92f, animationSpec = tween(AnimDuration, easing = EmphasizedEasing))
            },
            popEnterTransition = {
                fadeIn(tween(AnimDuration)) + scaleIn(initialScale = 0.92f, animationSpec = tween(AnimDuration, easing = EmphasizedEasing))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(AnimDuration, easing = EmphasizedEasing))
            }
        ) {
            GroupDetailsScreen(
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = { groupId ->
                    navController.navigate(Screen.CreateExpense.createRoute(groupId))
                },
                onEditExpenseClick = { groupId, expenseId ->
                    navController.navigate(Screen.CreateExpense.createRoute(groupId, expenseId))
                },
                onMembersClick = { groupId ->
                    navController.navigate(Screen.Members.createRoute(groupId))
                },
                onSettleUpClick = { groupId, receiverId, amount ->
                    navController.navigate(Screen.CreatePayment.createRoute(groupId, receiverId, amount))
                }
            )
        }

        composable(
            route = Screen.CreateExpense.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(AnimDuration, easing = EmphasizedEasing))
            },
            exitTransition = {
                fadeOut(tween(AnimDuration)) + scaleOut(targetScale = 0.95f, animationSpec = tween(AnimDuration))
            },
            popEnterTransition = {
                fadeIn(tween(AnimDuration)) + scaleIn(initialScale = 0.95f, animationSpec = tween(AnimDuration))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(AnimDuration, easing = EmphasizedEasing))
            }
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")!!
            val expenseId = backStackEntry.arguments?.getString("expenseId")

            CreateExpenseScreen(
                onBack = { navController.popBackStack() },
                onHistoryClick = {
                    if (expenseId != null) {
                        navController.navigate(Screen.ExpenseHistory.createRoute(groupId, expenseId))
                    }
                }
            )
        }

        composable(
            route = Screen.ExpenseHistory.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("expenseId") { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(AnimDuration, easing = EmphasizedEasing))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(AnimDuration, easing = EmphasizedEasing))
            }
        ) {
            ExpenseHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Members.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(AnimDuration, easing = EmphasizedEasing))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(AnimDuration, easing = EmphasizedEasing))
            }
        ) {
            MembersScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CreatePayment.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("receiverId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("amount") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(AnimDuration, easing = EmphasizedEasing))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(AnimDuration, easing = EmphasizedEasing))
            }
        ) {
            CreatePaymentScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}