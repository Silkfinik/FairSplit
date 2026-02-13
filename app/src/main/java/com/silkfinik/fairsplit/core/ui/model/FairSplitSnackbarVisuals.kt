package com.silkfinik.fairsplit.core.ui.model

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

data class FairSplitSnackbarVisuals(
    override val message: String,
    val isError: Boolean,
    override val actionLabel: String? = null,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    override val withDismissAction: Boolean = false
) : SnackbarVisuals