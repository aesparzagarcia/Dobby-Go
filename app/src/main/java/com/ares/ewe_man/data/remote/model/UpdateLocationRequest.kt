package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class UpdateLocationRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)
