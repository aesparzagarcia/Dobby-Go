package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class VerifyPickupCodeResponse(
    @SerializedName("valid") val valid: Boolean = false,
)
