package com.silkfinik.fairsplit.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.silkfinik.fairsplit.features.expenses.screen.CreateExpenseScreen
import com.silkfinik.fairsplit.features.groupdetails.screen.GroupDetailsScreen
import com.silkfinik.fairsplit.features.groups.screen.CreateGroupScreen
import com.silkfinik.fairsplit.features.groups.screen.GroupsListScreen

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
        composable(Screen.GroupsList.route) {
            GroupsListScreen(
                onNavigateToCreateGroup = {
                    navController.navigate(Screen.CreateGroup.route)
                },
                onNavigateToGroupDetails = { groupId ->
                    navController.navigate(Screen.GroupDetails.createRoute(groupId))
                }
            )
        }

        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.GroupDetails.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            GroupDetailsScreen(
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = { groupId ->
                    navController.navigate(Screen.CreateExpense.createRoute(groupId))
                }
            )
        }

        composable(
            route = Screen.CreateExpense.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            CreateExpenseScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}