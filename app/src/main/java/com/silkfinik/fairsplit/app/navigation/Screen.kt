package com.silkfinik.fairsplit.app.navigation

sealed class Screen(val route: String) {
    data object GroupsList : Screen("groups_list")

    data object CreateGroup : Screen("create_group")
}