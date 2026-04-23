package com.ares.ewe_man.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryOrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Double,
    @SerializedName("deliveryAddress") val deliveryAddress: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("arrivedAtCustomerAt") val arrivedAtCustomerAt: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("shopName") val shopName: String?,
    @SerializedName("shopAddress") val shopAddress: String? = null,
    @SerializedName("shopLat") val shopLat: Double? = null,
    @SerializedName("shopLng") val shopLng: Double? = null,
    @SerializedName("customerName") val customerName: String?,
    @SerializedName("items") val items: List<DeliveryOrderItemDto>
)

data class DeliveryOrderItemDto(
    @SerializedName("productId") val productId: String,
    @SerializedName("productName") val productName: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Double
)
