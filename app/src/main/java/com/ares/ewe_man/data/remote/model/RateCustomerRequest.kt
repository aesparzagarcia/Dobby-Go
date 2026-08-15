package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class RateCustomerRequest(
    @SerializedName("stars") val stars: Int? = null,
    @SerializedName("punctual") val punctual: Boolean? = null,
    @SerializedName("pays_well") val paysWell: Boolean? = null,
    @SerializedName("tipped") val tipped: Boolean? = null,
    @SerializedName("recommended") val recommended: Boolean? = null,
)

data class RateCustomerResponse(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("xp_gained") val xpGainedSnake: Int? = null,
    @SerializedName("xpGained") val xpGainedCamel: Int? = null,
) {
    val xpGained: Int get() = xpGainedSnake ?: xpGainedCamel ?: 0
}
