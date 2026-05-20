package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class RegisterPushDeviceRequest(
    @SerializedName("fcm_token") val fcmToken: String,
    val platform: String = "android",
)

data class FirebaseTokenResponse(
    val token: String,
)
