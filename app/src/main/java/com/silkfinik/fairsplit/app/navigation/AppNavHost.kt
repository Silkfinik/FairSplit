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
import com.silkfinik.fairsplit.features.members.screen.MembersScreen

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
                },
                onEditExpenseClick = { groupId, expenseId ->
                    navController.navigate(Screen.CreateExpense.createRoute(groupId, expenseId))
                },
                onMembersClick = { groupId ->
                    navController.navigate(Screen.Members.createRoute(groupId))
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
            )
        ) {
            CreateExpenseScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Members.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            MembersScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}