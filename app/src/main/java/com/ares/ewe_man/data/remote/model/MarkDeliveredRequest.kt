package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class MarkDeliveredRequest(
    @SerializedName("delivery_code") val deliveryCode: String,
)
