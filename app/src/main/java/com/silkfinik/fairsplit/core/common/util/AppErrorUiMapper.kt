package com.silkfinik.fairsplit.core.common.util

import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.domain.model.AppError

fun AppError.asUiText(): UiText {
    return when (this) {
        is AppError.Common.Offline -> UiText.StringResource(R.string.error_no_internet)
        is AppError.Common.ServerUnavailable -> UiText.StringResource(R.string.error_server_unavailable)
        is AppError.Common.Timeout -> UiText.StringResource(R.string.error_timeout)
        is AppError.Common.Cancelled -> UiText.StringResource(R.string.action_cancelled)
        is AppError.Common.Unknown -> {
            val msg = exception?.localizedMessage
            if (msg != null) UiText.DynamicString(msg)
            else UiText.StringResource(R.string.error_unknown)
        }

        is AppError.Auth.InvalidCredentials -> UiText.StringResource(R.string.error_invalid_credentials)
        is AppError.Auth.UserCollision -> UiText.StringResource(R.string.error_user_collision)
        is AppError.Auth.UserNotFound -> UiText.StringResource(R.string.error_user_not_found)
        is AppError.Auth.NotAuthorized -> UiText.StringResource(R.string.error_not_authorized)
        is AppError.Auth.RecentLoginRequired -> UiText.StringResource(R.string.error_requires_relogin)
        is AppError.Auth.UserDisabled -> UiText.StringResource(R.string.error_account_disabled)

        is AppError.Payment.AmountTooLow -> UiText.StringResource(R.string.error_payment_amount_low)
        is AppError.Payment.SelfTransferForbidden -> UiText.StringResource(R.string.error_payment_self_transfer)
        is AppError.Payment.StatusAlreadyChanged -> UiText.StringResource(R.string.error_payment_status_changed)

        is AppError.General -> UiText.DynamicString(message)

        else -> UiText.StringResource(R.string.error_generic_operation_failed)
    }
}