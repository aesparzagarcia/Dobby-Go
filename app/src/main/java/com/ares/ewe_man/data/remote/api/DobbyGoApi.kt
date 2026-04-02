package com.ares.ewe_man.data.remote.api

import com.ares.ewe_man.data.remote.model.AssignOrderResponse
import com.ares.ewe_man.data.remote.model.DeliveredOrderResponse
import com.ares.ewe_man.data.remote.model.MarkArrivedResponse
import com.ares.ewe_man.data.remote.model.DeliveryOrderDto
import com.ares.ewe_man.data.remote.model.DeliveryProfileDto
import com.ares.ewe_man.data.remote.model.DeliveryStatusRequest
import com.ares.ewe_man.data.remote.model.DeliveryStatusResponse
import com.ares.ewe_man.data.remote.model.DeliveryRequestOtpRequest
import com.ares.ewe_man.data.remote.model.DeliveryRequestOtpResponse
import com.ares.ewe_man.data.remote.model.StartDeliveryResponse
import com.ares.ewe_man.data.remote.model.UpdateDeliveryEtaRequest
import com.ares.ewe_man.data.remote.model.UpdateDeliveryEtaResponse
import com.ares.ewe_man.data.remote.model.UpdateLocationRequest
import com.ares.ewe_man.data.remote.model.UpdateLocationResponse
import com.ares.ewe_man.data.remote.model.VerifyOtpRequest
import com.ares.ewe_man.data.remote.model.VerifyOtpResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path

interface DobbyGoApi {

    @POST("auth/delivery/request-otp")
    suspend fun requestOtp(@Body body: DeliveryRequestOtpRequest): DeliveryRequestOtpResponse

    @POST("auth/delivery/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): VerifyOtpResponse

    @GET("delivery/profile")
    suspend fun getDeliveryProfile(): DeliveryProfileDto

    @PATCH("delivery/status")
    suspend fun updateDeliveryStatus(@Body body: DeliveryStatusRequest): DeliveryStatusResponse

    @GET("delivery/orders")
    suspend fun getOrders(@Query("status") status: String? = "READY_FOR_PICKUP"): List<DeliveryOrderDto>

    @GET("delivery/orders/{id}")
    suspend fun getOrderById(@Path("id") orderId: String): DeliveryOrderDto

    @PATCH("delivery/orders/{id}/assign")
    suspend fun assignOrder(@Path("id") orderId: String): AssignOrderResponse

    @PATCH("delivery/orders/{id}/start")
    suspend fun startDelivery(@Path("id") orderId: String): StartDeliveryResponse

    @PATCH("delivery/orders/{id}/arrived")
    suspend fun markArrivedAtCustomer(@Path("id") orderId: String): MarkArrivedResponse

    @PATCH("delivery/orders/{id}/delivered")
    suspend fun markDelivered(@Path("id") orderId: String): DeliveredOrderResponse

    @PATCH("delivery/location")
    suspend fun updateLocation(@Body body: UpdateLocationRequest): UpdateLocationResponse

    @PATCH("delivery/orders/{id}/delivery-eta")
    suspend fun updateDeliveryEta(
        @Path("id") orderId: String,
        @Body body: UpdateDeliveryEtaRequest
    ): UpdateDeliveryEtaResponse
}
