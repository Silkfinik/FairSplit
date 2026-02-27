package com.silkfinik.fairsplit.features.account.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.model.User
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.BadgeType
import com.silkfinik.fairsplit.core.ui.component.FairSplitBadge
import com.silkfinik.fairsplit.core.ui.component.FairSplitButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitButtonStyle
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitDialog
import com.silkfinik.fairsplit.core.ui.component.FairSplitDivider
import com.silkfinik.fairsplit.core.ui.component.FairSplitListItem
import com.silkfinik.fairsplit.core.ui.component.FairSplitPasswordField
import com.silkfinik.fairsplit.core.ui.component.FairSplitScaffold
import com.silkfinik.fairsplit.core.ui.component.FairSplitSwitch
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.core.ui.component.FairSplitUserAvatar
import com.silkfinik.fairsplit.features.account.viewmodel.AccountViewModel

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }

    var nameState by remember(uiState.user) { mutableStateOf(uiState.user?.displayName ?: "") }

    LaunchedEffect(uiState.user?.displayName) {
        if (uiState.user?.displayName != null && nameState.isBlank()) {
            nameState = uiState.user?.displayName ?: ""
        }
    }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState
    )

    if (showSignOutDialog) {
        FairSplitDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = stringResource(R.string.account_sign_out_dialog_title),
            text = stringResource(R.string.account_sign_out_dialog_message),
            confirmLabel = stringResource(R.string.account_sign_out_dialog_confirm),
            onConfirmAction = {
                showSignOutDialog = false
                viewModel.onSignOut()
            },
            dismissLabel = stringResource(R.string.action_cancel),
            icon = Icons.AutoMirrored.Filled.ExitToApp
        )
    }

    if (showDeleteDialog) {
        FairSplitDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = stringResource(R.string.account_delete_dialog_title),
            text = stringResource(R.string.account_delete_dialog_message),
            confirmLabel = stringResource(R.string.account_delete_dialog_confirm),
            onConfirmAction = {
                showDeleteDialog = false
                viewModel.onDeleteAccount()
            },
            isDestructive = true,
            dismissLabel = stringResource(R.string.action_cancel),
            icon = Icons.Default.Delete
        )
    }

    if (uiState.isLinkSheetVisible) {
        LinkEmailBottomSheet(
            uiState = uiState,
            onDismiss = { viewModel.showLinkEmailSheet(false) },
            onEmailChange = viewModel::onLinkEmailChange,
            onPasswordChange = viewModel::onLinkPasswordChange,
            onConfirmPasswordChange = viewModel::onLinkConfirmPasswordChange,
            onConfirm = viewModel::onLinkEmailAccount
        )
    }

    if (showChangeEmailDialog) {
        ChangeEmailDialog(
            currentEmail = uiState.user?.email ?: "",
            onDismiss = { showChangeEmailDialog = false },
            onConfirm = { newEmail ->
                showChangeEmailDialog = false
                viewModel.updateEmail(newEmail)
            }
        )
    }

    FairSplitScaffold(
        topBar = {
            FairSplitTopAppBar(
                title = if (uiState.isAnonymous) stringResource(R.string.account_title_guest) else stringResource(R.string.account_title_profile),
                onBackClick = onBack
            )
        },
        snackbarHostState = snackbarHostState,
        showProgress = uiState.isLoading
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProfileHeader(
                    user = uiState.user,
                    name = nameState,
                    isAnonymous = uiState.isAnonymous,
                    onNameChange = { nameState = it },
                    onNameSubmit = { viewModel.onNameChange(nameState) },
                    onAvatarSelected = viewModel::onAvatarSelected
                )
            }

            if (!uiState.isAnonymous && !uiState.isEmailVerified) {
                item {
                    VerificationWarningCard(
                        onResendClick = viewModel::resendVerificationEmail,
                        onCheckStatusClick = viewModel::checkVerificationStatus,
                        onChangeEmailClick = { showChangeEmailDialog = true }
                    )
                }
            }

            if (uiState.isAnonymous) {
                item {
                    LinkAccountSection(
                        onLinkGoogle = { viewModel.startGoogleAccountLink(context) },
                        onLinkEmail = { viewModel.showLinkEmailSheet(true) }
                    )
                }
            }

            item {
                SettingsSection(
                    isNotificationsEnabled = uiState.isNotificationsEnabled,
                    onToggleNotifications = viewModel::onToggleNotifications,
                    onSignOut = { showSignOutDialog = true },
                    onDeleteAccount = { showDeleteDialog = true }
                )
            }
        }
    }
}

@Composable
fun ProfileHeader(
    user: User?,
    name: String,
    isAnonymous: Boolean,
    onNameChange: (String) -> Unit,
    onNameSubmit: () -> Unit,
    onAvatarSelected: (Uri) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onAvatarSelected(uri)
            }
        }
    )

    FairSplitCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                FairSplitUserAvatar(
                    photoUrl = user?.photoUrl,
                    name = user?.displayName ?: "?",
                    size = 100.dp,
                    fontSize = 40.sp
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = 4.dp, y = 4.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.account_cd_edit_photo),
                        modifier = Modifier.padding(6.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (user?.email != null) {
                FairSplitBadge(
                    text = user.email,
                    type = BadgeType.Neutral
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else if (isAnonymous) {
                FairSplitBadge(
                    text = stringResource(R.string.account_badge_guest_mode),
                    type = BadgeType.Info
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            FairSplitTextField(
                value = name,
                onValueChange = onNameChange,
                label = stringResource(R.string.account_input_label_name),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { onNameSubmit() }
                )
            )

            val hasChanges = name != (user?.displayName ?: "") && name.isNotBlank()

            if (hasChanges) {
                Spacer(modifier = Modifier.height(16.dp))
                FairSplitButton(
                    text = stringResource(R.string.action_save),
                    onClick = onNameSubmit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun LinkAccountSection(
    onLinkGoogle: () -> Unit,
    onLinkEmail: () -> Unit
) {
    FairSplitCard {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.account_link_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.account_link_section_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FairSplitButton(
                text = stringResource(R.string.account_btn_link_google),
                onClick = onLinkGoogle,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FairSplitButton(
                text = stringResource(R.string.account_btn_link_email),
                onClick = onLinkEmail,
                style = FairSplitButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsSection(
    isNotificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    FairSplitCard {
        Column {
            Text(
                text = stringResource(R.string.account_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            FairSplitListItem(
                headlineContent = { Text(stringResource(R.string.account_settings_notifications)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    FairSplitSwitch(
                        checked = isNotificationsEnabled,
                        onCheckedChange = onToggleNotifications
                    )
                },
                modifier = Modifier.clickable { onToggleNotifications(!isNotificationsEnabled) }
            )

            FairSplitDivider(
                startIndent = 56.dp,
                endIndent = 16.dp,
                verticalSpacing = 8.dp
            )

            FairSplitListItem(
                headlineContent = { Text(stringResource(R.string.account_settings_sign_out)) },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = onSignOut
            )

            FairSplitListItem(
                headlineContent = {
                    Text(stringResource(R.string.account_settings_delete_account), color = MaterialTheme.colorScheme.error)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = onDeleteAccount
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun VerificationWarningCard(
    onResendClick: () -> Unit,
    onCheckStatusClick: () -> Unit,
    onChangeEmailClick: () -> Unit
) {
    FairSplitCard(
        backgroundColor = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MarkEmailUnread,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.account_verification_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.account_verification_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                FairSplitButton(
                    text = stringResource(R.string.account_verification_btn_fix),
                    onClick = onChangeEmailClick,
                    style = FairSplitButtonStyle.Text,
                    modifier = Modifier.height(36.dp)
                )
                FairSplitButton(
                    text = stringResource(R.string.account_verification_btn_resend),
                    onClick = onResendClick,
                    style = FairSplitButtonStyle.Text,
                    modifier = Modifier.height(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FairSplitButton(
                text = stringResource(R.string.account_verification_btn_check),
                onClick = onCheckStatusClick,
                style = FairSplitButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkEmailBottomSheet(
    uiState: com.silkfinik.fairsplit.features.account.ui.AccountUiState,
    onDismiss: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = androidx.compose.ui.platform.LocalContext.current

    com.silkfinik.fairsplit.core.ui.component.FairSplitBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = stringResource(R.string.account_link_email_dialog_title)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            FairSplitTextField(
                value = uiState.linkEmail,
                onValueChange = onEmailChange,
                label = stringResource(R.string.label_email),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = uiState.linkEmailError != null,
                supportingText = uiState.linkEmailError?.asString(context)
            )

            val passwordState = rememberTextFieldState()
            val confirmPasswordState = rememberTextFieldState()

            LaunchedEffect(passwordState.text) {
                onPasswordChange(passwordState.text.toString())
            }

            LaunchedEffect(confirmPasswordState.text) {
                onConfirmPasswordChange(confirmPasswordState.text.toString())
            }

            FairSplitPasswordField(
                state = passwordState,
                label = stringResource(R.string.label_password),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.linkPasswordError != null,
                supportingText = uiState.linkPasswordError?.asString(context)
            )

            FairSplitPasswordField(
                state = confirmPasswordState,
                label = stringResource(R.string.account_input_label_confirm_password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            FairSplitButton(
                text = stringResource(R.string.account_btn_link_email_confirm),
                onClick = onConfirm,
                style = FairSplitButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newEmail by remember { mutableStateOf(currentEmail) }

    FairSplitDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.account_change_email_dialog_title),
        text = stringResource(R.string.account_change_email_dialog_message),
        content = {
            FairSplitTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = stringResource(R.string.account_input_label_new_email),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
        },
        confirmLabel = stringResource(R.string.action_save),
        onConfirmAction = {
            if (newEmail.isNotBlank() && newEmail != currentEmail) {
                onConfirm(newEmail)
            }
        },
        dismissLabel = stringResource(R.string.action_cancel)
    )
}