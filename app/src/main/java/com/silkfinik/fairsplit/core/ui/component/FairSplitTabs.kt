package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.silkfinik.fairsplit.core.ui.theme.FairSplitTheme

@Composable
fun FairSplitTabs(
    titles: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerShape = CircleShape
    val containerHeight = 48.dp
    val containerHorizontalPadding = 32.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth

        val textMeasurer = rememberTextMeasurer()
        val textStyle = MaterialTheme.typography.labelLarge

        val useScrollable = remember(titles, maxWidthPx, textStyle) {
            val paddingPerTab = 32.dp
            val paddingPerTabPx = with(density) { paddingPerTab.toPx() }
            val containerPaddingPx = with(density) { containerHorizontalPadding.toPx() }
            val availableWidth = maxWidthPx - containerPaddingPx

            val maxTitleWidth = titles.maxOfOrNull { title ->
                textMeasurer.measure(
                    text = title,
                    style = textStyle,
                    maxLines = 1
                ).size.width
            } ?: 0

            val widthPerTabInFixedMode = availableWidth / titles.size.coerceAtLeast(1)
            val requiredWidthForMaxTab = maxTitleWidth + paddingPerTabPx

            requiredWidthForMaxTab > widthPerTabInFixedMode
        }

        val commonModifier = Modifier
            .fillMaxWidth()
            .height(containerHeight)
            .padding(horizontal = 16.dp)
            .clip(containerShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

        if (useScrollable) {
            val minTabWidth = maxWidth / titles.size

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 0.dp,
                indicator = {
                    TabIndicator(selectedIndex, Modifier.tabIndicatorOffset(selectedIndex))
                },
                divider = {},
                modifier = commonModifier
            ) {
                titles.forEachIndexed { index, title ->
                    FairSplitTabItem(
                        title = title,
                        isSelected = selectedIndex == index,
                        onClick = { onTabSelected(index) },
                        modifier = Modifier.widthIn(min = minTabWidth)
                    )
                }
            }
        } else {
            PrimaryTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = {
                    TabIndicator(selectedIndex, Modifier.tabIndicatorOffset(selectedIndex))
                },
                divider = {},
                modifier = commonModifier
            ) {
                titles.forEachIndexed { index, title ->
                    FairSplitTabItem(
                        title = title,
                        isSelected = selectedIndex == index,
                        onClick = { onTabSelected(index) },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun TabIndicator(
    selectedIndex: Int,
    modifier: Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
private fun FairSplitTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
        label = "TextColor"
    )

    Tab(
        selected = isSelected,
        onClick = onClick,
        modifier = modifier
            .clip(CircleShape)
            .zIndex(1f)
            .fillMaxSize(),
        text = {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun FairSplitTabsTwoItemsPreview() {
    FairSplitTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FairSplitTabs(
                titles = listOf("Траты", "Платежи"),
                selectedIndex = 0,
                onTabSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun FairSplitTabsFourItemsPreview() {
    FairSplitTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FairSplitTabs(
                titles = listOf("Один", "Два", "Три", "Четыре"),
                selectedIndex = 0,
                onTabSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun FairSplitTabsScrollPreview() {
    FairSplitTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FairSplitTabs(
                titles = listOf("Очень длинные расходы", "Задолженности", "Архив групп"),
                selectedIndex = 1,
                onTabSelected = {}
            )
        }
    }
}