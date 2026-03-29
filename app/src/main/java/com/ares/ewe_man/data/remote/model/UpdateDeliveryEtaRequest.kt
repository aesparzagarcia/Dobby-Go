package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class UpdateDeliveryEtaRequest(
    @SerializedName("estimatedDeliveryMinutes") val estimatedDeliveryMinutes: Int
)
