package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryMissionDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("progress") val progress: Int,
    @SerializedName("goal") val goal: Int,
    @SerializedName("completed") val completed: Boolean,
)
