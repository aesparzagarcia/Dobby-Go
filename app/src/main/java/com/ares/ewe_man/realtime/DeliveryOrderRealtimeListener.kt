package com.ares.ewe_man.realtime

import com.ares.ewe_man.data.local.datastore.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryOrderRealtimeListener @Inject constructor(
    private val sessionManager: SessionManager,
    private val deliveryFirebaseAuth: DeliveryFirebaseAuth,
    private val orderRealtimeBus: DeliveryOrderRealtimeBus,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var registration: ListenerRegistration? = null

    fun start() {
        scope.launch {
            if (!sessionManager.isLoggedIn.first()) {
                stop()
                return@launch
            }
            deliveryFirebaseAuth.signInWithBackendToken()
            if (registration != null) return@launch

            registration = FirebaseFirestore.getInstance()
                .collection("delivery_available")
                .addSnapshotListener { _, _ ->
                    orderRealtimeBus.notifyOrdersChanged()
                }
        }
    }

    fun stop() {
        registration?.remove()
        registration = null
    }
}
