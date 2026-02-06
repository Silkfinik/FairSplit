package com.silkfinik.fairsplit.core.data.mapper

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.silkfinik.fairsplit.core.domain.model.AppError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

fun Throwable.toAppError(): AppError {
    return when (this) {
        is SocketTimeoutException, is TimeoutException -> AppError.Common.Timeout(this)

        is UnknownHostException, is IOException -> AppError.Common.Offline(this)

        is ApiException -> {
            when (statusCode) {
                12501, CommonStatusCodes.CANCELED -> AppError.Common.Cancelled
                else -> AppError.Common.Unknown(this)
            }
        }

        is FirebaseAuthInvalidCredentialsException -> AppError.Auth.InvalidCredentials
        is FirebaseAuthUserCollisionException -> AppError.Auth.UserCollision
        is FirebaseAuthRecentLoginRequiredException -> AppError.Auth.RecentLoginRequired
        is FirebaseAuthInvalidUserException -> {
            if (errorCode == "ERROR_USER_DISABLED") AppError.Auth.UserDisabled
            else AppError.Auth.UserNotFound
        }

        is FirebaseFirestoreException -> {
            when (code) {
                FirebaseFirestoreException.Code.UNAVAILABLE -> AppError.Common.ServerUnavailable(this)
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Auth.NotAuthorized
                FirebaseFirestoreException.Code.ALREADY_EXISTS -> AppError.Data.AlreadyExists
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Common.Timeout(this)
                else -> AppError.Common.Unknown(this)
            }
        }

        else -> AppError.Common.Unknown(this)
    }
}