package com.ares.ewe_man.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dobbygo_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val DELIVERY_MAN_ID = stringPreferencesKey("delivery_man_id")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTH_TOKEN]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.REFRESH_TOKEN]
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[Keys.AUTH_TOKEN].isNullOrBlank() || !prefs[Keys.REFRESH_TOKEN].isNullOrBlank()
    }

    suspend fun saveSession(accessToken: String, refreshToken: String, deliveryManId: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            deliveryManId?.let { prefs[Keys.DELIVERY_MAN_ID] = it }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.DELIVERY_MAN_ID)
        }
    }
}
