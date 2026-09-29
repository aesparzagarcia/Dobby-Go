package com.ares.ewe_man.data.repository

import com.ares.ewe_man.data.remote.api.DobbyGoApi
import com.ares.ewe_man.data.remote.model.DeliveryProfileDto
import com.ares.ewe_man.data.remote.model.DeliveryStatusRequest
import com.ares.ewe_man.domain.repository.DeliveryProfileRepository
import javax.inject.Inject

class DeliveryProfileRepositoryImpl @Inject constructor(
    private val api: DobbyGoApi,
) : DeliveryProfileRepository {
    override suspend fun getProfile(): Result<DeliveryProfileDto> {
        return try {
            Result.success(api.getDeliveryProfile())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setConnectionStatus(status: String): Result<Unit> {
        return try {
            api.updateDeliveryStatus(DeliveryStatusRequest(status))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun heartbeat(): Result<Unit> {
        return try {
            api.heartbeat(emptyMap())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getShifts(week: String?): Result<com.ares.ewe_man.data.remote.model.DeliveryShiftsDto> {
        return try {
            Result.success(api.getShifts(week))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveShifts(
        weekStart: String,
        claims: List<com.ares.ewe_man.data.remote.model.DeliveryShiftClaimDto>,
    ): Result<com.ares.ewe_man.data.remote.model.DeliveryShiftsDto> {
        return try {
            Result.success(
                api.saveShifts(
                    com.ares.ewe_man.data.remote.model.SaveShiftsRequest(weekStart, claims),
                ),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
