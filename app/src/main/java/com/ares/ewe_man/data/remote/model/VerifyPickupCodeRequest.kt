package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class VerifyPickupCodeRequest(
    @SerializedName("pickup_code") val pickupCode: String,
)
