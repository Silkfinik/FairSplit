package com.silkfinik.fairsplit.core.domain.model

sealed interface AppError {

    data class General(
        val message: String,
        val exception: Throwable? = null
    ) : AppError

    sealed interface Common : AppError {
        data class Unknown(val exception: Throwable? = null) : Common
        data class Offline(val exception: Throwable? = null) : Common
        data class ServerUnavailable(val exception: Throwable? = null) : Common
        data class Timeout(val exception: Throwable? = null) : Common
        data object Cancelled : Common
    }

    sealed interface Auth : AppError {
        data object NotAuthorized : Auth
        data object UserNotFound : Auth

        data object InvalidCredentials : Auth
        data object UserCollision : Auth
        data object RecentLoginRequired : Auth
        data object UserDisabled : Auth
    }

    sealed interface Data : AppError {
        data object AlreadyExists : Data
    }

    sealed interface Payment : AppError {
        data object NotFound : Payment
        data object AmountTooLow : Payment
        data object SelfTransferForbidden : Payment
        data object StatusAlreadyChanged : Payment
    }

    sealed interface Group : AppError {
        data object NotFound : Group
    }

    sealed interface Expense : AppError {
        data object NotFound : Expense
    }

    sealed interface Member : AppError
}