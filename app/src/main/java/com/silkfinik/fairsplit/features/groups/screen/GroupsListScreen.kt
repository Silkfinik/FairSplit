package com.silkfinik.fairsplit.features.groups.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitButtonStyle
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitEmptyState
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.features.groups.viewmodel.GroupsViewModel
import java.util.Locale

@Composable
fun GroupsListScreen(
    viewModel: GroupsViewModel = hiltViewModel(),
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToGroupDetails: (String) -> Unit,
    onNavigateToAccount: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showJoinDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState,
        onNavigateToGroupDetails = onNavigateToGroupDetails
    )

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    if (showJoinDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                viewModel.joinGroup(code)
                showJoinDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FairSplitTopAppBar(
                title = "Мои группы",
                actions = {
                    IconButton(onClick = onNavigateToAccount) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Профиль"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading) {
                ExpandableFab(
                    isExpanded = isFabExpanded,
                    onToggle = { isFabExpanded = !isFabExpanded },
                    onCreateGroup = {
                        isFabExpanded = false
                        onNavigateToCreateGroup()
                    },
                    onJoinGroup = {
                        isFabExpanded = false
                        showJoinDialog = true
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            if (isFabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isFabExpanded = false }
                        .zIndex(1f)
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.groups.isEmpty()) {
                FairSplitEmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    icon = Icons.Default.Groups,
                    title = "Групп пока нет",
                    description = "Создайте свою первую группу, чтобы начать вести совместный бюджет с друзьями!",
                    actionLabel = "Создать группу",
                    onActionClick = onNavigateToCreateGroup
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.groups) { group ->
                        GroupItem(
                            group = group,
                            onClick = {
                                if (isFabExpanded) isFabExpanded = false
                                else onNavigateToGroupDetails(group.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableFab(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onCreateGroup: () -> Unit,
    onJoinGroup: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        label = "FabRotation"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FabOption(
            visible = isExpanded,
            text = "Вступить в группу",
            icon = Icons.Default.PersonAdd,
            onClick = onJoinGroup
        )

        FabOption(
            visible = isExpanded,
            text = "Создать группу",
            icon = Icons.Default.Add,
            onClick = onCreateGroup
        )

        FloatingActionButton(
            onClick = onToggle
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "Закрыть меню" else "Открыть меню",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
fun FabOption(
    visible: Boolean,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            SmallFloatingActionButton(
                onClick = onClick
            ) {
                Icon(imageVector = icon, contentDescription = text)
            }
        }
    }
}

fun Modifier.zIndex(zIndex: Float) = this.then(
    object : androidx.compose.ui.layout.LayoutModifier {
        override fun androidx.compose.ui.layout.MeasureScope.measure(
            measurable: androidx.compose.ui.layout.Measurable,
            constraints: androidx.compose.ui.unit.Constraints
        ): androidx.compose.ui.layout.MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.place(0, 0, zIndex = zIndex)
            }
        }
    }
)

@Composable
fun GroupItem(group: Group, onClick: () -> Unit) {
    FairSplitCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            ) {
                Text(
                    text = group.name.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (group.inviteCode != null) {
                    Text(
                        text = "Код: ${group.inviteCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = group.currency.symbol,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Вступить в группу",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Введите 6-значный код приглашения, который вам отправил администратор группы.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(16.dp))
                FairSplitTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.uppercase() },
                    label = "Код приглашения",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Например: X7Z29A"
                )
            }
        },
        confirmButton = {
            FairSplitButton(
                text = "Вступить",
                onClick = { onJoin(code) },
                enabled = code.length == 6,
                style = FairSplitButtonStyle.Primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        dismissButton = {
            FairSplitButton(
                text = "Отмена",
                onClick = onDismiss,
                style = FairSplitButtonStyle.Text,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge
    )
}