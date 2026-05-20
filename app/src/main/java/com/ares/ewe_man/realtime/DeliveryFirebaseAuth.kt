package com.ares.ewe_man.realtime

import com.ares.ewe_man.data.remote.api.DobbyGoApi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryFirebaseAuth @Inject constructor(
    private val api: DobbyGoApi,
) {
    suspend fun signInWithBackendToken() {
        val customToken = try {
            api.getFirebaseCustomToken().token
        } catch (_: Exception) {
            return
        }
        if (customToken.isBlank()) return
        try {
            FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()
        } catch (_: Exception) {
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}
