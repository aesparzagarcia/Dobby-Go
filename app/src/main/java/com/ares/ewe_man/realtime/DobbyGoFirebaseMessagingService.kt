package com.ares.ewe_man.realtime

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DobbyGoFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushTokenRegistrar: DeliveryPushTokenRegistrar

    @Inject
    lateinit var orderRealtimeBus: DeliveryOrderRealtimeBus

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            pushTokenRegistrar.registerToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"]
        if (type == "delivery_weekend_challenge" || type == "delivery_shift_reminder") {
            val title = message.notification?.title ?: if (type == "delivery_weekend_challenge") {
                "Reto Dobbi del fin de semana"
            } else {
                "Elige tus horarios"
            }
            val body = message.notification?.body ?: message.data["body"].orEmpty()
            DeliveryOrderNotificationHelper.show(this, title, body, null)
            return
        }
        if (type != null && type != "delivery_order_available") return
        val orderId = message.data["order_id"]
        val title = message.notification?.title ?: "Pedido disponible"
        val body = message.notification?.body ?: "Hay un pedido listo para recoger."
        DeliveryOrderNotificationHelper.show(this, title, body, orderId)
        orderRealtimeBus.notifyOrdersChanged()
    }
}
