package com.silkfinik.fairsplit.app.navigation

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object Register : Screen("register")
    
    data object GroupsList : Screen("groups_list")

    data object CreateGroup : Screen("create_group")

    data object GroupDetails : Screen("group_details/{groupId}") {
        fun createRoute(groupId: String) = "group_details/$groupId"
    }

    data object CreateExpense : Screen("create_expense/{groupId}?expenseId={expenseId}") {
        fun createRoute(groupId: String, expenseId: String? = null): String {
            return if (expenseId != null) {
                "create_expense/$groupId?expenseId=$expenseId"
            } else {
                "create_expense/$groupId"
            }
        }
    }

    data object Members : Screen("members/{groupId}") {
        fun createRoute(groupId: String) = "members/$groupId"
    }

    data object ExpenseHistory : Screen("expense_history/{groupId}/{expenseId}") {
        fun createRoute(groupId: String, expenseId: String) = "expense_history/$groupId/$expenseId"
    }

    data object CreatePayment : Screen("create_payment/{groupId}?receiverId={receiverId}&amount={amount}") {
        fun createRoute(groupId: String, receiverId: String? = null, amount: String? = null): String {
            val builder = StringBuilder("create_payment/$groupId")
            val params = mutableListOf<String>()
            if (receiverId != null) params.add("receiverId=$receiverId")
            if (amount != null) params.add("amount=$amount")
            
            if (params.isNotEmpty()) {
                builder.append("?")
                builder.append(params.joinToString("&"))
            }
            return builder.toString()
        }
    }

    data object Account : Screen("account")
}
