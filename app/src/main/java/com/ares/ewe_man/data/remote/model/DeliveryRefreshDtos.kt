package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryRefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class DeliveryRefreshResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("refreshToken") val refreshToken: String?
)
