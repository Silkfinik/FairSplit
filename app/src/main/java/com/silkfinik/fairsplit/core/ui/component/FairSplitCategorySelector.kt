package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
import com.silkfinik.fairsplit.core.ui.theme.FairSplitShapes

@Composable
fun FairSplitCategorySelector(
    selectedCategory: ExpenseCategory,
    onCategorySelected: (ExpenseCategory) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(ExpenseCategory.entries) { category ->
            CategoryItem(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: ExpenseCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val (categoryBgColor, categoryIconColor) = getCategoryColors(category)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "ItemScale"
    )

    val innerContainerSize by animateDpAsState(
        targetValue = if (isSelected) 64.dp else 56.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "InnerContainerSize"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            categoryBgColor
        else
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        label = "ContainerColor"
    )

    val iconTint by animateColorAsState(
        targetValue = categoryIconColor,
        label = "IconTint"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected)
            categoryIconColor
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "TextColor"
    )

    val shape: Shape = FairSplitShapes.large

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null
            )
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(innerContainerSize)
                    .clip(shape)
                    .background(containerColor)
            ) {
                CompositionLocalProvider(LocalContentColor provides iconTint) {
                    CategoryIcon(
                        category = category,
                        size = if (isSelected) 32.dp else 24.dp,
                        withBackground = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}