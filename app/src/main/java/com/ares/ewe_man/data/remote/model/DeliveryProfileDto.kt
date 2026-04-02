package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryProfileDto(
    @SerializedName("name") val name: String,
    @SerializedName("profile_photo_url") val profilePhotoUrl: String? = null,
    /** OFFLINE | ONLINE | ON_DELIVERY */
    @SerializedName("status") val status: String = "OFFLINE",
    @SerializedName("level_key") val levelKey: String,
    @SerializedName("xp") val xp: Int,
    @SerializedName("xp_at_current_level") val xpAtCurrentLevel: Int,
    @SerializedName("xp_for_next_level") val xpForNextLevel: Int?,
    @SerializedName("rating") val rating: Double,
    @SerializedName("rating_count") val ratingCount: Int,
    @SerializedName("current_streak_days") val currentStreakDays: Int,
    @SerializedName("total_deliveries") val totalDeliveries: Int,
    @SerializedName("missions") val missions: List<DeliveryMissionDto>,
)
