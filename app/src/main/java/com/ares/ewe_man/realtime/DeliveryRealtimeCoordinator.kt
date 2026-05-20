package com.ares.ewe_man.realtime

import com.ares.ewe_man.data.local.datastore.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryRealtimeCoordinator @Inject constructor(
    private val sessionManager: SessionManager,
    private val deliveryFirebaseAuth: DeliveryFirebaseAuth,
    private val pushTokenRegistrar: DeliveryPushTokenRegistrar,
    private val orderRealtimeListener: DeliveryOrderRealtimeListener,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun onSessionReady() {
        scope.launch {
            if (!sessionManager.isLoggedIn.first()) return@launch
            deliveryFirebaseAuth.signInWithBackendToken()
            pushTokenRegistrar.registerCurrentToken()
            orderRealtimeListener.start()
        }
    }

    fun onLogout() {
        orderRealtimeListener.stop()
        deliveryFirebaseAuth.signOut()
        scope.launch {
            pushTokenRegistrar.unregisterOnServer()
        }
    }
}
