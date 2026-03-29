package com.ares.ewe_man.domain.repository

import com.ares.ewe_man.data.remote.model.DeliveryOrderDto

interface OrderRepository {

    suspend fun getOrdersReadyForPickup(): Result<List<DeliveryOrderDto>>

    /** Get orders by status. For ASSIGNED/ON_DELIVERY/DELIVERED returns only this delivery person's orders. */
    suspend fun getOrdersByStatus(status: String): Result<List<DeliveryOrderDto>>

    suspend fun getOrderById(orderId: String): Result<DeliveryOrderDto>

    suspend fun assignOrder(orderId: String): Result<Unit>

    suspend fun startDelivery(orderId: String): Result<Unit>

    suspend fun markDelivered(orderId: String): Result<Unit>

    /** Report current location to backend so the customer can see it on the tracking map. */
    suspend fun updateLocation(lat: Double, lng: Double): Result<Unit>

    /** Persist driving ETA (minutes) for the customer app while this order is ON_DELIVERY. */
    suspend fun updateDeliveryEta(orderId: String, estimatedDeliveryMinutes: Int): Result<Unit>
}
