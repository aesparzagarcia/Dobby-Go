package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class MarkArrivedResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("arrived_at_customer_at") val arrivedAtCustomerAt: String?
)
