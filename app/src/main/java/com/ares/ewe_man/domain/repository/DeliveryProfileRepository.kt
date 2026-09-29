package com.ares.ewe_man.domain.repository

import com.ares.ewe_man.data.remote.model.DeliveryProfileDto
import com.ares.ewe_man.data.remote.model.DeliveryShiftClaimDto
import com.ares.ewe_man.data.remote.model.DeliveryShiftsDto

interface DeliveryProfileRepository {
    suspend fun getProfile(): Result<DeliveryProfileDto>
    /** OFFLINE u ONLINE; ON_DELIVERY lo fija el servidor. */
    suspend fun setConnectionStatus(status: String): Result<Unit>
    suspend fun heartbeat(): Result<Unit>
    suspend fun getShifts(week: String? = null): Result<DeliveryShiftsDto>
    suspend fun saveShifts(weekStart: String, claims: List<DeliveryShiftClaimDto>): Result<DeliveryShiftsDto>
}
