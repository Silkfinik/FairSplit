package com.silkfinik.fairsplit.app.navigation

sealed class Screen(val route: String) {
    data object GroupsList : Screen("groups_list")

    data object CreateGroup : Screen("create_group")

    data object GroupDetails : Screen("group_details/{groupId}") {
        fun createRoute(groupId: String) = "group_details/$groupId"
    }

    data object CreateExpense : Screen("create_expense/{groupId}") {
        fun createRoute(groupId: String) = "create_expense/$groupId"
    }
}