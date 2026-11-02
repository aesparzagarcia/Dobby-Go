package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryOrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Double,
    @SerializedName("serviceFee") val serviceFee: Double = 0.0,
    @SerializedName("deliveryFee") val deliveryFee: Double = 0.0,
    @SerializedName("courierCommission") val courierCommission: Double = 0.0,
    @SerializedName("courierCommissionRate") val courierCommissionRate: Double = 0.0,
    @SerializedName("courierPayout") val courierPayout: Double = 0.0,
    @SerializedName("deliveryAddress") val deliveryAddress: String?,
    @SerializedName("deliveryAddressLabel") val deliveryAddressLabel: String? = null,
    @SerializedName("delivery_address_label") val deliveryAddressLabelSnake: String? = null,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("arrivedAtCustomerAt") val arrivedAtCustomerAt: String? = null,
    @SerializedName("deliveredAt") val deliveredAt: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("shopName") val shopName: String?,
    @SerializedName("shopAddress") val shopAddress: String? = null,
    @SerializedName("shopLat") val shopLat: Double? = null,
    @SerializedName("shopLng") val shopLng: Double? = null,
    @SerializedName("customerName") val customerName: String?,
    @SerializedName("customerLastName") val customerLastName: String? = null,
    @SerializedName("customerPhone") val customerPhone: String? = null,
    @SerializedName("customer_phone") val customerPhoneSnake: String? = null,
    @SerializedName("pickup_code_required") val pickupCodeRequired: Boolean = true,
    @SerializedName("order_type") val orderType: String? = null,
    /** SERVICE_PAYMENT accepted but payment at service point not confirmed yet. */
    @SerializedName("service_payment_pending") val servicePaymentPending: Boolean = false,
    @SerializedName("items") val items: List<DeliveryOrderItemDto>
) {
    val showsCourierCommission: Boolean
        get() = courierCommission > 0.0

    val courierCommissionPercentText: String
        get() {
            val pct = if (courierCommissionRate > 1.0) courierCommissionRate else courierCommissionRate * 100.0
            if (pct <= 0.0) return "Comisión de la app"
            val rounded = kotlin.math.round(pct * 10.0) / 10.0
            return if (rounded % 1.0 == 0.0) {
                "Comisión de la app (${rounded.toInt()}%)"
            } else {
                "Comisión de la app (${String.format(java.util.Locale.US, "%.1f", rounded)}%)"
            }
        }

    val courierTakeHome: Double
        get() = if (courierPayout > 0.0) courierPayout else deliveryFee - courierCommission

    val isServicePayment: Boolean
        get() = orderType.equals("SERVICE_PAYMENT", ignoreCase = true) || !pickupCodeRequired

    val resolvedAddressLabel: String?
        get() = deliveryAddressLabel?.trim()?.takeIf { it.isNotEmpty() }
            ?: deliveryAddressLabelSnake?.trim()?.takeIf { it.isNotEmpty() }

    /** Prefer camelCase; fall back to snake_case from API. */
    val resolvedCustomerPhone: String?
        get() = customerPhone?.trim()?.takeIf { it.isNotEmpty() }
            ?: customerPhoneSnake?.trim()?.takeIf { it.isNotEmpty() }

    /** Hora a mostrar: entrega si ya está entregado; si no, creación del pedido. */
    fun displayAt(): String {
        val delivered = deliveredAt?.trim().orEmpty()
        return if (status.equals("DELIVERED", ignoreCase = true) && delivered.isNotEmpty()) {
            delivered
        } else {
            createdAt
        }
    }
}

data class DeliveryOrderItemDto(
    @SerializedName("productId") val productId: String,
    @SerializedName("productName") val productName: String?,
    @SerializedName("service_number") val serviceNumber: String? = null,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Double,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("choicesLabel") val choicesLabel: String? = null,
) {
    val displayAmount: Double get() = amount ?: price
}
