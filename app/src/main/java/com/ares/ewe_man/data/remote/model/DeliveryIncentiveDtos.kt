package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryWeekNextGoalDto(
    @SerializedName("bonus_pesos") val bonusPesos: Int = 0,
    @SerializedName("hours_goal_minutes") val hoursGoalMinutes: Int = 0,
    @SerializedName("deliveries_goal") val deliveriesGoal: Int = 0,
    @SerializedName("hours_remaining_minutes") val hoursRemainingMinutes: Int = 0,
    @SerializedName("deliveries_remaining") val deliveriesRemaining: Int = 0,
    @SerializedName("message") val message: String = "",
)

data class DeliveryWeekendDto(
    @SerializedName("deliveries") val deliveries: Int = 0,
    @SerializedName("goal") val goal: Int = 12,
    @SerializedName("bonus_pesos") val bonusPesos: Int = 150,
    @SerializedName("unlocked_pesos") val unlockedPesos: Int = 0,
    @SerializedName("status") val status: String = "IN_PROGRESS",
    @SerializedName("windows_label") val windowsLabel: String = "",
)

data class DeliveryWeekDto(
    @SerializedName("week_start") val weekStart: String = "",
    @SerializedName("week_end") val weekEnd: String = "",
    @SerializedName("active_minutes") val activeMinutes: Int = 0,
    @SerializedName("active_hours_label") val activeHoursLabel: String = "0 h",
    @SerializedName("deliveries") val deliveries: Int = 0,
    @SerializedName("acceptance_rate") val acceptanceRate: Double = 100.0,
    @SerializedName("weekly_level") val weeklyLevel: String = "NONE",
    @SerializedName("weekly_level_label") val weeklyLevelLabel: String = "",
    @SerializedName("weekly_bonus_pesos") val weeklyBonusPesos: Int = 0,
    @SerializedName("weekly_status") val weeklyStatus: String = "IN_PROGRESS",
    @SerializedName("next_goal") val nextGoal: DeliveryWeekNextGoalDto? = null,
    @SerializedName("weekend") val weekend: DeliveryWeekendDto = DeliveryWeekendDto(),
)

data class DeliveryShiftClaimDto(
    @SerializedName("weekday") val weekday: Int,
    @SerializedName("slot") val slot: String,
)

data class DeliveryShiftOverTargetDto(
    @SerializedName("weekday") val weekday: Int = 0,
    @SerializedName("slot") val slot: String = "",
    @SerializedName("claimed") val claimed: Int = 0,
    @SerializedName("target") val target: Int = 0,
)

data class DeliveryShiftWeekDto(
    @SerializedName("week_start") val weekStart: String = "",
    @SerializedName("claims") val claims: List<DeliveryShiftClaimDto> = emptyList(),
    @SerializedName("over_target") val overTarget: List<DeliveryShiftOverTargetDto> = emptyList(),
)

data class DeliveryShiftSlotDto(
    @SerializedName("key") val key: String,
    @SerializedName("label") val label: String,
)

data class DeliveryShiftsDto(
    @SerializedName("week_start") val weekStart: String = "",
    @SerializedName("next_week_start") val nextWeekStart: String = "",
    @SerializedName("slots") val slots: List<DeliveryShiftSlotDto> = emptyList(),
    @SerializedName("current") val current: DeliveryShiftWeekDto = DeliveryShiftWeekDto(),
    @SerializedName("next") val next: DeliveryShiftWeekDto = DeliveryShiftWeekDto(),
)

data class SaveShiftsRequest(
    @SerializedName("week_start") val weekStart: String,
    @SerializedName("claims") val claims: List<DeliveryShiftClaimDto>,
)
