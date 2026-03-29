package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class VerifyOtpRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("code") val code: String
)
