package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class UpdateDeliveryEtaResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("estimatedDeliveryMinutes") val estimatedDeliveryMinutes: Int? = null
)
