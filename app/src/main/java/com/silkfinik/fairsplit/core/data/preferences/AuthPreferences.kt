package com.silkfinik.fairsplit.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class AuthPreferences @Inject constructor(
    private val context: Context
) {
    private val KEY_PENDING_EMAIL = stringPreferencesKey("pending_email_update")

    suspend fun saveEmailForNextLogin(email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PENDING_EMAIL] = email
        }
    }

    suspend fun getAndClearEmail(): String? {
        val email = context.dataStore.data
            .map { prefs -> prefs[KEY_PENDING_EMAIL] }
            .first()

        if (email != null) {
            context.dataStore.edit { prefs ->
                prefs.remove(KEY_PENDING_EMAIL)
            }
        }
        return email
    }
}