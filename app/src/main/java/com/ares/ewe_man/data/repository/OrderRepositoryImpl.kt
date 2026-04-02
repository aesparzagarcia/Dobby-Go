package com.ares.ewe_man.data.repository

import com.ares.ewe_man.data.remote.api.DobbyGoApi
import com.ares.ewe_man.data.remote.model.DeliveryOrderDto
import com.ares.ewe_man.data.remote.model.UpdateDeliveryEtaRequest
import com.ares.ewe_man.domain.repository.OrderRepository
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val api: DobbyGoApi
) : OrderRepository {

    override suspend fun getOrdersReadyForPickup(): Result<List<DeliveryOrderDto>> {
        return getOrdersByStatus("READY_FOR_PICKUP")
    }

    override suspend fun getOrdersByStatus(status: String): Result<List<DeliveryOrderDto>> {
        return try {
            val list = api.getOrders(status)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrderById(orderId: String): Result<DeliveryOrderDto> {
        return try {
            Result.success(api.getOrderById(orderId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun assignOrder(orderId: String): Result<Unit> {
        return try {
            api.assignOrder(orderId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startDelivery(orderId: String): Result<Unit> {
        return try {
            api.startDelivery(orderId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markArrivedAtCustomer(orderId: String): Result<Unit> {
        return try {
            api.markArrivedAtCustomer(orderId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markDelivered(orderId: String): Result<Unit> {
        return try {
            api.markDelivered(orderId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLocation(lat: Double, lng: Double): Result<Unit> {
        return try {
            api.updateLocation(com.ares.ewe_man.data.remote.model.UpdateLocationRequest(lat = lat, lng = lng))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDeliveryEta(orderId: String, estimatedDeliveryMinutes: Int): Result<Unit> {
        return try {
            api.updateDeliveryEta(orderId, UpdateDeliveryEtaRequest(estimatedDeliveryMinutes))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
