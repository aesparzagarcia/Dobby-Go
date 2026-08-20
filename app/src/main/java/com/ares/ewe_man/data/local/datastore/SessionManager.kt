package com.ares.ewe_man.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val tokenStore = EncryptedTokenStore(context)
    private val migrationMutex = Mutex()
    @Volatile private var migrated = false

    private val _authToken = MutableStateFlow(tokenStore.accessToken)
    private val _refreshToken = MutableStateFlow(tokenStore.refreshToken)

    val authToken: Flow<String?> = flow {
        migrateLegacyTokensIfNeeded()
        _authToken.value = tokenStore.accessToken
        emitAll(_authToken)
    }

    val refreshToken: Flow<String?> = flow {
        migrateLegacyTokensIfNeeded()
        _refreshToken.value = tokenStore.refreshToken
        emitAll(_refreshToken)
    }

    val isLoggedIn: Flow<Boolean> = combine(authToken, refreshToken) { access, refresh ->
        !access.isNullOrBlank() || !refresh.isNullOrBlank()
    }

    private suspend fun migrateLegacyTokensIfNeeded() {
        if (migrated) return
        migrationMutex.withLock {
            if (migrated) return
            val hasSecure =
                !tokenStore.accessToken.isNullOrBlank() || !tokenStore.refreshToken.isNullOrBlank()
            if (!hasSecure) {
                val prefs = context.dataStore.data.first()
                val legacyAccess = prefs[Keys.AUTH_TOKEN]
                val legacyRefresh = prefs[Keys.REFRESH_TOKEN]
                if (!legacyAccess.isNullOrBlank() || !legacyRefresh.isNullOrBlank()) {
                    tokenStore.save(
                        accessToken = legacyAccess.orEmpty(),
                        refreshToken = legacyRefresh.orEmpty(),
                    )
                    _authToken.value = tokenStore.accessToken
                    _refreshToken.value = tokenStore.refreshToken
                }
            }
            context.dataStore.edit { prefs ->
                prefs.remove(Keys.AUTH_TOKEN)
                prefs.remove(Keys.REFRESH_TOKEN)
            }
            migrated = true
        }
    }

    suspend fun saveSession(accessToken: String, refreshToken: String, deliveryManId: String? = null) {
        migrateLegacyTokensIfNeeded()
        tokenStore.save(accessToken, refreshToken)
        _authToken.value = accessToken
        _refreshToken.value = refreshToken
        if (deliveryManId != null) {
            context.dataStore.edit { prefs ->
                prefs[Keys.DELIVERY_MAN_ID] = deliveryManId
            }
        }
    }

    suspend fun clearSession() {
        migrateLegacyTokensIfNeeded()
        tokenStore.clear()
        _authToken.value = null
        _refreshToken.value = null
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.DELIVERY_MAN_ID)
        }
    }

    suspend fun prepareSession() {
        migrateLegacyTokensIfNeeded()
        _authToken.value = tokenStore.accessToken
        _refreshToken.value = tokenStore.refreshToken
    }
}
