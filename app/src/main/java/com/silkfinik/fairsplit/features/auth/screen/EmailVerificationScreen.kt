package com.silkfinik.fairsplit.features.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.ui.component.FairSplitButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitButtonStyle
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitScaffold
import com.silkfinik.fairsplit.features.auth.viewmodel.EmailVerificationViewModel

@Composable
fun EmailVerificationScreen(
    onVerificationConfirmed: () -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    FairSplitScaffold(
        snackbarHostState = snackbarHostState,
        isLoading = uiState.isLoading
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            FairSplitCard {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailUnread,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.verify_email_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.verify_email_message),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.message != null || uiState.error != null) {
                        Spacer(modifier = Modifier.height(24.dp))

                        val isError = uiState.error != null
                        val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer
                        val icon = if (isError) Icons.Default.Error else Icons.Default.CheckCircle
                        val text = uiState.error?.asString(context) ?: uiState.message?.asString(context) ?: ""

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(containerColor)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    FairSplitButton(
                        text = stringResource(R.string.verify_email_btn_check),
                        onClick = { viewModel.checkVerificationStatus(onVerificationConfirmed) },
                        isLoading = uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FairSplitButton(
                        text = stringResource(R.string.verify_email_btn_resend),
                        onClick = { viewModel.sendVerificationEmail() },
                        style = FairSplitButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FairSplitButton(
                text = stringResource(R.string.verify_email_btn_logout),
                onClick = { viewModel.signOut() },
                style = FairSplitButtonStyle.Text,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )
        }
    }
}