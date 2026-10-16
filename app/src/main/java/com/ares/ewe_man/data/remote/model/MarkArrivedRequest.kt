package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class MarkArrivedRequest(
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
)
