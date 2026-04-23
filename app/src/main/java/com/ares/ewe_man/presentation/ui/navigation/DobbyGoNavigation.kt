package com.ares.ewe_man.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ares.ewe_man.di.SessionEventBusEntryPoint
import com.ares.ewe_man.presentation.ui.auth.otp.OtpScreen
import com.ares.ewe_man.presentation.ui.auth.phone.PhoneScreen
import com.ares.ewe_man.presentation.ui.deliverymap.DeliveryMapScreen
import com.ares.ewe_man.presentation.ui.main.MainScreen
import com.ares.ewe_man.presentation.ui.orderdetail.OrderDetailScreen
import com.ares.ewe_man.presentation.ui.pickupmap.PickupMapScreen
import com.ares.ewe_man.presentation.ui.splash.SplashScreen
import com.ares.ewe_man.presentation.viewmodel.nav.OrdersRefreshViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun DobbyGoNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionEventBus = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SessionEventBusEntryPoint::class.java
        ).sessionEventBus()
    }
    LaunchedEffect(sessionEventBus) {
        sessionEventBus.sessionExpired.collect {
            navController.navigate(DobbyGoScreens.Phone) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val refreshVm = hiltViewModel<OrdersRefreshViewModel>()
    NavHost(
        navController = navController,
        startDestination = DobbyGoScreens.Splash
    ) {
        composable(DobbyGoScreens.Splash) {
            SplashScreen(
                onOpenAuth = {
                    navController.navigate(DobbyGoScreens.Phone) {
                        popUpTo(DobbyGoScreens.Splash) { inclusive = true }
                    }
                },
                onOpenHome = {
                    navController.navigate(DobbyGoScreens.Main) {
                        popUpTo(DobbyGoScreens.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable(DobbyGoScreens.Phone) {
            PhoneScreen(
                onCodeSent = { phone ->
                    navController.navigate(DobbyGoScreens.otp(phone)) {
                        popUpTo(DobbyGoScreens.Phone) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = DobbyGoScreens.Otp,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phone = phone,
                onVerified = {
                    navController.navigate(DobbyGoScreens.Main) {
                        popUpTo(DobbyGoScreens.Otp) { inclusive = true }
                    }
                }
            )
        }
        composable(DobbyGoScreens.Main) {
            val refreshTrigger by refreshVm.triggerCount.collectAsState(initial = 0)
            MainScreen(
                onLogout = {
                    navController.navigate(DobbyGoScreens.Phone) {
                        popUpTo(DobbyGoScreens.Main) { inclusive = true }
                    }
                },
                onOrderClick = { orderId ->
                    navController.navigate(DobbyGoScreens.orderDetail(orderId))
                },
                refreshOrdersTrigger = refreshTrigger
            )
        }
        composable(
            route = DobbyGoScreens.OrderDetail,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) {
            OrderDetailScreen(
                onBack = {
                    refreshVm.triggerRefresh()
                    navController.popBackStack()
                },
                onOpenMap = { orderId ->
                    refreshVm.triggerRefresh()
                    navController.navigate(DobbyGoScreens.deliveryMap(orderId)) {
                        popUpTo(DobbyGoScreens.OrderDetail) { inclusive = true }
                    }
                },
                onOpenPickupMap = { orderId ->
                    refreshVm.triggerRefresh()
                    navController.navigate(DobbyGoScreens.pickupMap(orderId)) {
                        popUpTo(DobbyGoScreens.OrderDetail) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = DobbyGoScreens.PickupMap,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pickupOrderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            PickupMapScreen(
                onBack = {
                    refreshVm.triggerRefresh()
                    navController.popBackStack()
                },
                onComenzarEnvio = {
                    refreshVm.triggerRefresh()
                    navController.navigate(DobbyGoScreens.deliveryMap(pickupOrderId)) {
                        popUpTo(DobbyGoScreens.PickupMap) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = DobbyGoScreens.DeliveryMap,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) {
            DeliveryMapScreen(
                onBack = {
                    refreshVm.triggerRefresh()
                    navController.popBackStack()
                }
            )
        }
    }
}
