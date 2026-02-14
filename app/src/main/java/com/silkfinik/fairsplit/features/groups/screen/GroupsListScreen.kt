package com.silkfinik.fairsplit.features.groups.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.BadgeType
import com.silkfinik.fairsplit.core.ui.component.FairSplitBadge
import com.silkfinik.fairsplit.core.ui.component.FairSplitBottomSheet
import com.silkfinik.fairsplit.core.ui.component.FairSplitButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitDialog
import com.silkfinik.fairsplit.core.ui.component.FairSplitEmptyState
import com.silkfinik.fairsplit.core.ui.component.FairSplitListItem
import com.silkfinik.fairsplit.core.ui.component.FairSplitScaffold
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.core.ui.component.FairSplitUserAvatar
import com.silkfinik.fairsplit.features.groups.viewmodel.GroupsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsListScreen(
    viewModel: GroupsViewModel = hiltViewModel(),
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToGroupDetails: (String) -> Unit,
    onNavigateToAccount: () -> Unit
) {

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showJoinDialog by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState,
        onNavigateToGroupDetails = onNavigateToGroupDetails
    )

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { uiText ->
            snackbarHostState.showSnackbar(uiText.asString(context))
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

    if (showAddMenu) {
        FairSplitBottomSheet(
            onDismissRequest = { showAddMenu = false },
            title = stringResource(R.string.groups_sheet_add_title)
        ) {
            Column(
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                FairSplitListItem(
                    headlineContent = { Text(stringResource(R.string.groups_sheet_action_create)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        showAddMenu = false
                        onNavigateToCreateGroup()
                    }
                )

                FairSplitListItem(
                    headlineContent = { Text(stringResource(R.string.groups_sheet_action_join)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        showAddMenu = false
                        showJoinDialog = true
                    }
                )
            }
        }
    }

    FairSplitScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            FairSplitTopAppBar(
                title = stringResource(R.string.groups_title),
                actions = {
                    IconButton(onClick = onNavigateToAccount) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.groups_cd_profile)
                        )
                    }
                }
            )
        },
        isLoading = uiState.isLoading
    ) {
        Box(modifier = Modifier
            .fillMaxSize()) {

            if (uiState.groups.isEmpty() && !uiState.isLoading) {
                FairSplitEmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    icon = Icons.Default.Groups,
                    title = stringResource(R.string.groups_empty_title),
                    description = stringResource(R.string.groups_empty_desc),
                    actionLabel = stringResource(R.string.groups_empty_action),
                    onActionClick = onNavigateToCreateGroup
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.groups, key = { it.id }) { group ->
                        GroupItem(
                            group = group,
                            onClick = { onNavigateToGroupDetails(group.id) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    FairSplitButton(
                        text = stringResource(R.string.groups_btn_add),
                        onClick = { showAddMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun GroupItem(group: Group, onClick: () -> Unit) {
    FairSplitCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FairSplitUserAvatar(
                photoUrl = null,
                name = group.name,
                size = 56.dp,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (group.inviteCode != null) {
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text(
                        text = stringResource(R.string.groups_item_code_format, group.inviteCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FairSplitBadge(
                text = group.currency.symbol,
                type = BadgeType.Secondary,
                shape = CircleShape
            )
        }
    }
}

@Composable
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    FairSplitDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.groups_join_dialog_title),
        content = {
            Column {
                Text(
                    text = stringResource(R.string.groups_join_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.padding(12.dp))
                FairSplitTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.uppercase() },
                    label = stringResource(R.string.groups_join_input_label),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.groups_join_input_placeholder)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
            }
        },
        confirmLabel = stringResource(R.string.groups_join_btn_confirm),
        onConfirmAction = { if (code.length == 6) onJoin(code) },
        dismissLabel = stringResource(R.string.action_cancel),
        onDismissAction = onDismiss
    )
}