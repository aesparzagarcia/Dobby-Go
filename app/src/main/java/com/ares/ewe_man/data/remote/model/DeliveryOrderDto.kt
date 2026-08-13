package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryOrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Double,
    @SerializedName("serviceFee") val serviceFee: Double = 0.0,
    @SerializedName("deliveryFee") val deliveryFee: Double = 0.0,
    @SerializedName("deliveryAddress") val deliveryAddress: String?,
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
    @SerializedName("pickup_code_required") val pickupCodeRequired: Boolean = true,
    @SerializedName("order_type") val orderType: String? = null,
    /** SERVICE_PAYMENT accepted but payment at service point not confirmed yet. */
    @SerializedName("service_payment_pending") val servicePaymentPending: Boolean = false,
    @SerializedName("items") val items: List<DeliveryOrderItemDto>
) {
    val isServicePayment: Boolean
        get() = orderType.equals("SERVICE_PAYMENT", ignoreCase = true) || !pickupCodeRequired

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
) {
    val displayAmount: Double get() = amount ?: price
}
