package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory

@Composable
fun CategoryIcon(
    category: ExpenseCategory,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val (backgroundColor, iconColor) = getCategoryColors(category)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.displayName,
            tint = iconColor,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

private fun getCategoryColors(category: ExpenseCategory): Pair<Color, Color> {
    return when (category) {
        ExpenseCategory.GROCERIES -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32)) // Light Green / Dark Green
        ExpenseCategory.EATING_OUT -> Pair(Color(0xFFFFF3E0), Color(0xFFEF6C00)) // Light Orange / Dark Orange
        ExpenseCategory.TRANSPORT -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0)) // Light Blue / Dark Blue
        ExpenseCategory.HOUSING -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2)) // Light Purple / Dark Purple
        ExpenseCategory.TRAVEL -> Pair(Color(0xFFE0F2F1), Color(0xFF00695C)) // Light Teal / Dark Teal
        ExpenseCategory.ENTERTAINMENT -> Pair(Color(0xFFFCE4EC), Color(0xFFC2185B)) // Light Pink / Dark Pink
        ExpenseCategory.HEALTH -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828)) // Light Red / Dark Red
        ExpenseCategory.SHOPPING -> Pair(Color(0xFFE8EAF6), Color(0xFF283593)) // Light Indigo / Dark Indigo
        ExpenseCategory.GIFTS -> Pair(Color(0xFFFFF8E1), Color(0xFFF9A825)) // Light Yellow / Dark Yellow
        ExpenseCategory.OTHER -> Pair(Color(0xFFF5F5F5), Color(0xFF616161)) // Light Gray / Dark Gray
    }
}
