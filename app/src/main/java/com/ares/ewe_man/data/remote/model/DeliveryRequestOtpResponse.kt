package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryRequestOtpResponse(
    @SerializedName("sent") val sent: Boolean
)
