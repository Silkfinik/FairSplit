package com.silkfinik.fairsplit.features.members.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.ui.component.UserAvatar
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.features.members.ui.MembersUiState
import com.silkfinik.fairsplit.features.members.viewmodel.MembersViewModel

@Composable
fun MembersScreen(
    viewModel: MembersViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var memberToClaim by remember { mutableStateOf<Member?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onBack
    )

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name ->
                viewModel.addGhostMember(name)
                showAddMemberDialog = false
            }
        )
    }

    if (memberToClaim != null) {
        AlertDialog(
            onDismissRequest = { memberToClaim = null },
            title = { Text("Объединение профиля") },
            text = { Text("Вы действительно хотите объединить свой аккаунт с участником \"${memberToClaim?.name}\"? История трат будет сохранена за вами.") },
            confirmButton = {
                Button(
                    onClick = {
                        memberToClaim?.let { viewModel.claimGhost(it.id) }
                        memberToClaim = null
                    }
                ) {
                    Text("Объединить")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToClaim = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FairSplitTopAppBar(title = "Участники", onBackClick = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddMemberDialog = true }) {
                Icon(Icons.Default.Add, "Добавить")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                MembersUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is MembersUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is MembersUiState.Success -> {
                    MembersList(
                        members = state.members,
                        currentUserId = state.currentUserId,
                        linkedGhostIds = state.linkedGhostIds,
                        hasClaimedGhost = state.hasClaimedGhost,
                        onClaimClick = { memberToClaim = it }
                    )
                }
            }
        }
    }
}

@Composable
fun MembersList(
    members: List<Member>,
    currentUserId: String?,
    linkedGhostIds: List<String>,
    hasClaimedGhost: Boolean,
    onClaimClick: (Member) -> Unit
) {
    val visibleMembers = members.filter { it.mergedWithUid == null }

    val mergedGhostsMap = members
        .filter { it.mergedWithUid != null }
        .groupBy { it.mergedWithUid!! }
        .mapValues { entry -> entry.value.map { it.name } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(visibleMembers) { member ->
            val mergedGhosts = mergedGhostsMap[member.id] ?: emptyList()
            
            MemberItem(
                member = member,
                currentUserId = currentUserId,
                isLinked = linkedGhostIds.contains(member.id),
                mergedGhosts = mergedGhosts,
                canClaim = !hasClaimedGhost,
                onClaimClick = { onClaimClick(member) }
            )
        }
    }
}

@Composable
fun MemberItem(
    member: Member,
    currentUserId: String?,
    isLinked: Boolean,
    mergedGhosts: List<String>,
    canClaim: Boolean,
    onClaimClick: () -> Unit
) {
    val displayName = if (member.id == currentUserId) "${member.name} (Вы)" else member.name
    
    FairSplitCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                photoUrl = member.photoUrl,
                name = member.name,
                size = 40.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (mergedGhosts.isNotEmpty()) {
                    Text(
                        text = "Связанные профили: ${mergedGhosts.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (member.isGhost) {
                    Text(
                        text = "Виртуальный участник",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (member.isGhost && !isLinked && member.mergedWithUid == null && canClaim) {
                TextButton(onClick = onClaimClick) {
                    Text("Это я")
                }
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить участника") },
        text = {
            Column {
                Text(
                    text = "Введите имя участника, которого нет в приложении, чтобы делить с ним траты.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.padding(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (name.isNotBlank()) {
                                onConfirm(name)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
