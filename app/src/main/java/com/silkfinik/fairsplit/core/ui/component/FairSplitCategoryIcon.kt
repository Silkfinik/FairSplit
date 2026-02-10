package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
import com.silkfinik.fairsplit.core.ui.theme.FairSplitShapes

@Composable
fun CategoryIcon(
    category: ExpenseCategory,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    withBackground: Boolean = true
) {
    val (categoryBgColor, categoryIconColor) = getCategoryColors(category)

    val backgroundColor = if (withBackground) categoryBgColor else Color.Transparent
    val tint = if (withBackground) categoryIconColor else LocalContentColor.current
    val shape = FairSplitShapes.large

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.displayName,
            tint = tint,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

fun getCategoryColors(category: ExpenseCategory): Pair<Color, Color> {
    return when (category) {
        ExpenseCategory.GROCERIES -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        ExpenseCategory.EATING_OUT -> Pair(Color(0xFFFFF3E0), Color(0xFFEF6C00))
        ExpenseCategory.TRANSPORT -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        ExpenseCategory.HOUSING -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        ExpenseCategory.TRAVEL -> Pair(Color(0xFFE0F2F1), Color(0xFF00695C))
        ExpenseCategory.ENTERTAINMENT -> Pair(Color(0xFFFCE4EC), Color(0xFFC2185B))
        ExpenseCategory.HEALTH -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        ExpenseCategory.SHOPPING -> Pair(Color(0xFFE8EAF6), Color(0xFF283593))
        ExpenseCategory.GIFTS -> Pair(Color(0xFFFFF8E1), Color(0xFFF9A825))
        ExpenseCategory.OTHER -> Pair(Color(0xFFF5F5F5), Color(0xFF616161))
    }
}