package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class AssignOrderResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("status") val status: String
)
