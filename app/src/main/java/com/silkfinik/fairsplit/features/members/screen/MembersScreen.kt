package com.silkfinik.fairsplit.features.members.screen

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.BadgeType
import com.silkfinik.fairsplit.core.ui.component.FairSplitBadge
import com.silkfinik.fairsplit.core.ui.component.FairSplitButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitButtonStyle
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitDialog
import com.silkfinik.fairsplit.core.ui.component.FairSplitEmptyState
import com.silkfinik.fairsplit.core.ui.component.FairSplitIconButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitListItem
import com.silkfinik.fairsplit.core.ui.component.FairSplitScaffold
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.core.ui.component.FairSplitUserAvatar
import com.silkfinik.fairsplit.features.members.ui.MembersUiState
import com.silkfinik.fairsplit.features.members.viewmodel.MembersViewModel
import kotlinx.coroutines.launch

@Composable
fun MembersScreen(
    viewModel: MembersViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isGeneratingCode by viewModel.isGeneratingCode.collectAsStateWithLifecycle()

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var memberToClaim by remember { mutableStateOf<Member?>(null) }
    var showClaimInfo by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onBack
    )

    FairSplitScaffold(
        topBar = {
            FairSplitTopAppBar(title = stringResource(R.string.members_title), onBackClick = onBack)
        },
        snackbarHostState = snackbarHostState,
        isLoading = uiState is MembersUiState.Loading
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is MembersUiState.Error -> {
                    FairSplitEmptyState(
                        icon = Icons.Default.PersonAdd,
                        title = stringResource(R.string.members_error_title),
                        description = state.message.asString(context),
                        actionLabel = stringResource(R.string.members_error_retry),
                        modifier = Modifier.padding(padding)
                    )
                }
                is MembersUiState.Success -> {
                    MembersListContent(
                        state = state,
                        isGeneratingCode = isGeneratingCode,
                        onGenerateCode = viewModel::generateInviteCode,
                        onCopyCode = { code ->
                            scope.launch {
                                val clipData = ClipData.newPlainText("Invite Code", code)
                                clipboard.setClipEntry(clipData.toClipEntry())
                            }
                        },
                        onClaimClick = { memberToClaim = it },
                        onEditClick = { memberToEdit = it }
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        FairSplitButton(
                            text = stringResource(R.string.members_btn_add_new),
                            onClick = { showAddMemberDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                MembersUiState.Loading -> {

                }
            }

            if (showAddMemberDialog) {
                AddMemberDialog(
                    onDismiss = { showAddMemberDialog = false },
                    onConfirm = { name ->
                        viewModel.addGhostMember(name)
                        showAddMemberDialog = false
                    }
                )
            }

            if (memberToEdit != null) {
                EditMemberNameDialog(
                    member = memberToEdit!!,
                    onDismiss = { memberToEdit = null },
                    onConfirm = { id, name ->
                        viewModel.updateMemberName(id, name)
                        memberToEdit = null
                    }
                )
            }

            if (showClaimInfo) {
                FairSplitDialog(
                    onDismissRequest = { showClaimInfo = false },
                    title = stringResource(R.string.members_dialog_claim_info_title),
                    text = stringResource(R.string.members_dialog_claim_info_message),
                    confirmLabel = stringResource(R.string.members_btn_understood),
                    onConfirmAction = { showClaimInfo = false },
                    dismissLabel = null,
                    icon = Icons.Outlined.Info
                )
            }

            if (memberToClaim != null) {
                FairSplitDialog(
                    onDismissRequest = { memberToClaim = null },
                    title = stringResource(R.string.members_dialog_claim_confirm_title),
                    text = stringResource(R.string.members_dialog_claim_confirm_message, memberToClaim?.name ?: ""),
                    confirmLabel = stringResource(R.string.members_btn_merge),
                    onConfirmAction = {
                        memberToClaim?.let { viewModel.claimGhost(it.id) }
                        memberToClaim = null
                    },
                    dismissLabel = stringResource(R.string.action_cancel),
                    onDismissAction = { memberToClaim = null },
                    icon = Icons.Default.Link
                )
            }

        }
    }
}

@Composable
private fun MembersListContent(
    state: MembersUiState.Success,
    isGeneratingCode: Boolean,
    onGenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit,
    onClaimClick: (Member) -> Unit,
    onEditClick: (Member) -> Unit
) {
    val members = state.members

    val activeMembers = remember(members, state.currentUserId) {
        members.filter { it.mergedWithUid == null && !it.isGhost || it.id == state.currentUserId }
    }
    val ghostMembers = remember(members, state.currentUserId) {
        members.filter { it.isGhost && it.mergedWithUid == null && it.id != state.currentUserId }
    }

    val mergedGhostsMap = remember(members) {
        members
            .filter { it.mergedWithUid != null }
            .groupBy { it.mergedWithUid!! }
            .mapValues { entry -> entry.value.map { it.name } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 100.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "invite_card") {
            InviteSectionCard(
                inviteCode = state.inviteCode,
                isGenerating = isGeneratingCode,
                onGenerateCode = onGenerateCode,
                onCopyCode = onCopyCode
            )
        }

        if (activeMembers.isNotEmpty()) {
            item(key = "header_active") {
                SectionHeader(
                    title = stringResource(R.string.members_header_active),
                    count = activeMembers.size,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(activeMembers, key = { it.id }) { member ->
                MemberItem(
                    member = member,
                    currentUserId = state.currentUserId,
                    isLinked = state.linkedGhostIds.contains(member.id),
                    mergedGhosts = mergedGhostsMap[member.id] ?: emptyList(),
                    canClaim = false,
                    onClaimClick = {},
                    onEditClick = {}
                )
            }
        }

        if (ghostMembers.isNotEmpty()) {
            item(key = "header_ghosts") {
                SectionHeader(
                    title = stringResource(R.string.members_header_ghosts),
                    count = ghostMembers.size,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(ghostMembers, key = { it.id }) { member ->
                MemberItem(
                    member = member,
                    currentUserId = state.currentUserId,
                    isLinked = state.linkedGhostIds.contains(member.id),
                    mergedGhosts = emptyList(),
                    canClaim = !state.hasClaimedGhost,
                    onClaimClick = { onClaimClick(member) },
                    onEditClick = { onEditClick(member) }
                )
            }
        }
    }
}
@Composable
private fun InviteSectionCard(
    inviteCode: String?,
    isGenerating: Boolean,
    onGenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FairSplitCard(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = if (inviteCode == null && !isGenerating) onGenerateCode else null
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp).padding(bottom = 12.dp)
            )

            Text(
                text = stringResource(R.string.members_invite_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (inviteCode == null) stringResource(R.string.members_invite_desc_create)
                else stringResource(R.string.members_invite_desc_share),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (inviteCode != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = inviteCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    FairSplitIconButton(
                        onClick = { onCopyCode(inviteCode) },
                        icon = Icons.Default.ContentCopy,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                FairSplitButton(
                    text = stringResource(R.string.members_invite_btn_generate),
                    onClick = onGenerateCode,
                    isLoading = isGenerating,
                    style = FairSplitButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    enableMorphingAnimation = true
                )
            }
        }
    }
}

@Composable
private fun MemberItem(
    member: Member,
    currentUserId: String?,
    isLinked: Boolean,
    mergedGhosts: List<String>,
    canClaim: Boolean,
    onClaimClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentUser = member.id == currentUserId

    FairSplitCard(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.defaultMinSize(minHeight = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            FairSplitListItem(
                modifier = Modifier.fillMaxWidth(),
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            FairSplitBadge(
                                text = stringResource(R.string.members_badge_you),
                                type = BadgeType.Secondary
                            )
                        }
                    }
                },
                leadingContent = {
                    FairSplitUserAvatar(
                        photoUrl = member.photoUrl,
                        name = member.name,
                        size = 44.dp
                    )
                },
                supportingContent = if (mergedGhosts.isNotEmpty()) {
                    {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            mergedGhosts.forEach { ghostName ->
                                FairSplitBadge(
                                    text = ghostName,
                                    type = BadgeType.Neutral
                                )
                            }
                        }
                    }
                } else null,
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canClaim && !isLinked) {
                            TextButton(
                                onClick = onClaimClick,
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.members_btn_claim_me),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (member.isGhost && member.mergedWithUid == null) {
                            FairSplitIconButton(
                                onClick = onEditClick,
                                icon = Icons.Default.Edit,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(start = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        FairSplitBadge(
            text = count.toString(),
            type = BadgeType.Primary
        )
    }
}

@Composable
private fun EditMemberNameDialog(
    member: Member,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(member.name) }

    FairSplitDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.members_dialog_edit_title),
        content = {
            FairSplitTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.members_label_member_name),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                singleLine = true
            )
        },
        confirmLabel = stringResource(R.string.action_save),
        onConfirmAction = { if (name.isNotBlank()) onConfirm(member.id, name) },
        dismissLabel = stringResource(R.string.action_cancel),
        onDismissAction = onDismiss
    )
}

@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    FairSplitDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.members_dialog_add_title),
        text = stringResource(R.string.members_dialog_add_message),
        content = {
            FairSplitTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.label_name),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                singleLine = true
            )
        },
        confirmLabel = stringResource(R.string.members_btn_add),
        onConfirmAction = { if (name.isNotBlank()) onConfirm(name) },
        dismissLabel = stringResource(R.string.action_cancel),
        onDismissAction = onDismiss,
        icon = Icons.Default.PersonAdd
    )
}