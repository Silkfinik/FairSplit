package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory

@Composable
fun FairSplitCategorySelector(
    selectedCategory: ExpenseCategory,
    onCategorySelected: (ExpenseCategory) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        items(ExpenseCategory.entries) { category ->
            val isSelected = category == selectedCategory
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCategorySelected(category) }
                    .animateContentSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {}
                    }
                    CategoryIcon(
                        category = category,
                        size = if (isSelected) 64.dp else 48.dp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}