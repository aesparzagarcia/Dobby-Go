package com.ares.ewe_man.data.local.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Access / refresh JWTs in EncryptedSharedPreferences (AES via Android Keystore).
 * Plaintext DataStore must not hold these secrets.
 * Writes use [SharedPreferences.Editor.commit] so tokens are durable before other
 * processes/threads (e.g. FCM) read them.
 */
internal class EncryptedTokenStore(
    context: Context,
    prefsFileName: String = "dobbygo_secure_tokens",
) {
    private val prefs: SharedPreferences = createPrefs(context.applicationContext, prefsFileName)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().run {
                if (value.isNullOrBlank()) remove(KEY_ACCESS) else putString(KEY_ACCESS, value)
                commit()
            }
        }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().run {
                if (value.isNullOrBlank()) remove(KEY_REFRESH) else putString(KEY_REFRESH, value)
                commit()
            }
        }

    fun save(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .commit()
    }

    fun clear() {
        prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).commit()
    }

    companion object {
        private const val KEY_ACCESS = "auth_token"
        private const val KEY_REFRESH = "refresh_token"

        private fun createPrefs(context: Context, fileName: String): SharedPreferences {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            return EncryptedSharedPreferences.create(
                fileName,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
