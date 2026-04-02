package com.ares.ewe_man.domain.repository

import com.ares.ewe_man.data.remote.model.DeliveryProfileDto

interface DeliveryProfileRepository {
    suspend fun getProfile(): Result<DeliveryProfileDto>
    /** OFFLINE u ONLINE; ON_DELIVERY lo fija el servidor. */
    suspend fun setConnectionStatus(status: String): Result<Unit>
}
