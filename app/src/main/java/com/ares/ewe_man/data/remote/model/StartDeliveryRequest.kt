package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class StartDeliveryRequest(
    @SerializedName("pickup_code") val pickupCode: String,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
)
