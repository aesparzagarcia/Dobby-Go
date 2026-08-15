package com.ares.ewe_man.presentation.ui.navigation

import com.ares.ewe_man.data.remote.model.DeliveryOrderDto
import com.ares.ewe_man.domain.repository.OrderRepository

/** Where to send the courier when reopening the app with an in-progress order. */
sealed class ActiveOrderResume {
    data class PickupMap(val orderId: String) : ActiveOrderResume()
    data class DeliveryMap(val orderId: String) : ActiveOrderResume()

    companion object {
        /**
         * Prefers ON_DELIVERY, then ASSIGNED. Opens the same map as Order Detail’s primary CTA.
         */
        suspend fun resolve(orderRepository: OrderRepository): ActiveOrderResume? {
            val candidates = buildList {
                orderRepository.getOrdersByStatus("ON_DELIVERY").getOrNull()?.let { addAll(it) }
                orderRepository.getOrdersByStatus("ASSIGNED").getOrNull()?.let { addAll(it) }
            }
            val order = candidates.firstOrNull() ?: return null
            return destination(order)
        }

        fun destination(order: DeliveryOrderDto): ActiveOrderResume? {
            return when (order.status.uppercase()) {
                "ASSIGNED" -> PickupMap(order.id)
                "ON_DELIVERY" -> {
                    if (order.isServicePayment && order.servicePaymentPending) {
                        PickupMap(order.id)
                    } else {
                        DeliveryMap(order.id)
                    }
                }
                else -> null
            }
        }
    }
}
