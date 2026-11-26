package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryProfileDto(
    @SerializedName("name") val name: String,
    @SerializedName("profile_photo_url") val profilePhotoUrl: String? = null,
    /** OFFLINE | ONLINE | ON_DELIVERY */
    @SerializedName("status") val status: String = "OFFLINE",
    @SerializedName("has_active_order") val hasActiveOrder: Boolean = false,
    @SerializedName("level_key") val levelKey: String,
    @SerializedName("xp") val xp: Int,
    @SerializedName("xp_at_current_level") val xpAtCurrentLevel: Int,
    @SerializedName("xp_for_next_level") val xpForNextLevel: Int?,
    @SerializedName("rating") val rating: Double,
    @SerializedName("rating_count") val ratingCount: Int,
    @SerializedName("current_streak_days") val currentStreakDays: Int,
    @SerializedName("total_deliveries") val totalDeliveries: Int,
    @SerializedName("missions") val missions: List<DeliveryMissionDto>,
    @SerializedName("week") val week: DeliveryWeekDto? = null,
    @SerializedName("cash") val cash: DeliveryCashDto? = null,
)

data class DeliveryCashDto(
    @SerializedName("date") val date: String = "",
    @SerializedName("opening_amount") val openingAmount: Double? = null,
    @SerializedName("products_subtotal") val productsSubtotal: Double = 0.0,
    @SerializedName("service_fee_net") val serviceFeeNet: Double = 0.0,
    @SerializedName("service_fee_iva") val serviceFeeIva: Double = 0.0,
    @SerializedName("delivery_fees") val deliveryFees: Double = 0.0,
    @SerializedName("customer_total") val customerTotal: Double = 0.0,
    @SerializedName("courier_commission") val courierCommission: Double = 0.0,
    @SerializedName("courier_payout") val courierPayout: Double = 0.0,
    @SerializedName("in_route_count") val inRouteCount: Int = 0,
    @SerializedName("in_route_products") val inRouteProducts: Double = 0.0,
    @SerializedName("expected_cash") val expectedCash: Double? = null,
)
