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

class GoogleSignInHelper @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    suspend fun signIn(activityContext: Context): GoogleIdTokenCredential? {
        val credentialManager = CredentialManager.create(activityContext)
        
        // Use appContext to get resource if needed, but activityContext is safer for resources too
        // But default_web_client_id is app-wide.
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
            if (credential is GoogleIdTokenCredential) {
                credential
            } else {
                null
            }
        } catch (e: GetCredentialException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
