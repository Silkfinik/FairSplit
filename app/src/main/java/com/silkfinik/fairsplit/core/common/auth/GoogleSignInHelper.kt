package com.silkfinik.fairsplit.core.common.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.silkfinik.fairsplit.core.common.util.Result

class GoogleSignInHelper @Inject constructor(
    @param:ApplicationContext private val appContext: Context
) {
    suspend fun signIn(activityContext: Context): Result<GoogleIdTokenCredential> {
        val credentialManager = CredentialManager.create(activityContext)
        val webClientId = appContext.getString(com.silkfinik.fairsplit.R.string.default_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = withContext(Dispatchers.Main) {
                 credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )
            }
            
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Result.Success(googleIdTokenCredential)
                } catch (e: Exception) {
                    Result.Error("Ошибка обработки данных Google", e)
                }
            } else {
                Result.Error("Неизвестный тип учетных данных")
            }
        } catch (e: GetCredentialCancellationException) {
            Result.Error("Вход отменен", e)
        } catch (e: NoCredentialException) {
            Result.Error("Учетные данные не найдены", e)
        } catch (e: GetCredentialException) {
            Result.Error("Ошибка входа: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Неизвестная ошибка: ${e.message}", e)
        }
    }
}
