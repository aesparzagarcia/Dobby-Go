package com.ares.ewe_man.realtime

import com.ares.ewe_man.data.local.datastore.SessionManager
import com.ares.ewe_man.data.remote.api.DobbyGoApi
import com.ares.ewe_man.data.remote.model.RegisterPushDeviceRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryPushTokenRegistrar @Inject constructor(
    private val api: DobbyGoApi,
    private val sessionManager: SessionManager,
) {
    suspend fun registerCurrentToken() {
        if (!sessionManager.isLoggedIn.first()) return
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (_: Exception) {
            return
        }
        registerToken(token)
    }

    suspend fun registerToken(fcmToken: String) {
        if (!sessionManager.isLoggedIn.first()) return
        try {
            api.registerPushDevice(RegisterPushDeviceRequest(fcmToken = fcmToken))
        } catch (_: Exception) {
        }
    }

    suspend fun unregisterOnServer() {
        try {
            api.unregisterPushDevice()
        } catch (_: Exception) {
        }
    }
}
