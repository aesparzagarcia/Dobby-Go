package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class VerifyOtpResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("deliveryMan") val deliveryMan: VerifyOtpDeliveryMan?
)

data class VerifyOtpDeliveryMan(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("celphone") val celphone: String?
)
