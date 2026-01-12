package com.silkfinik.fairsplit.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    }
}