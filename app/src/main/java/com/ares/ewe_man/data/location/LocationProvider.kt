package com.ares.ewe_man.data.location

interface LocationProvider {
    suspend fun getCurrentLocation(): Result<LocationUpdate>
}
