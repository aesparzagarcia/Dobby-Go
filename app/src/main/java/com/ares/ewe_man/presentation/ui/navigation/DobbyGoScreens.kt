package com.ares.ewe_man.presentation.ui.navigation

import android.net.Uri

object DobbyGoScreens {
    const val Splash = "splash"
    const val Phone = "phone"
    const val Otp = "otp/{phone}"
    const val Main = "main"
    const val OrderDetail = "orderDetail/{orderId}"
    const val DeliveryMap = "deliveryMap/{orderId}"

    fun otp(phone: String) = "otp/${Uri.encode(phone)}"
    fun orderDetail(orderId: String) = "orderDetail/${Uri.encode(orderId)}"
    fun deliveryMap(orderId: String) = "deliveryMap/${Uri.encode(orderId)}"
}
