package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryStatusRequest(
    @SerializedName("status") val status: String,
)

data class DeliveryStatusResponse(
    @SerializedName("ok") val ok: Boolean = true,
    @SerializedName("status") val status: String,
)
