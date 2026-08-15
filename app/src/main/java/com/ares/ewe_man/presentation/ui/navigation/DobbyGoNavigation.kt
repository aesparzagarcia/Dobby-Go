package com.ares.ewe_man.presentation.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ares.ewe_man.core.crash.TrackNavDestination
import com.ares.ewe_man.di.SessionEventBusEntryPoint
import com.ares.ewe_man.presentation.ui.auth.otp.OtpScreen
import com.ares.ewe_man.presentation.ui.auth.phone.PhoneScreen
import com.ares.ewe_man.presentation.ui.deliverymap.DeliveryMapScreen
import com.ares.ewe_man.presentation.ui.main.MainScreen
import com.ares.ewe_man.presentation.ui.orderdetail.OrderDetailScreen
import com.ares.ewe_man.presentation.ui.pickupmap.PickupMapScreen
import com.ares.ewe_man.presentation.ui.splash.SplashScreen
import com.ares.ewe_man.presentation.viewmodel.nav.ActiveOrderResumeViewModel
import com.ares.ewe_man.presentation.viewmodel.nav.OrdersRefreshViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
@Composable
fun DobbyGoNavigation(
    pendingOrderId: String? = null,
    onPendingOrderNavigated: () -> Unit = {},
) {
    val navController = rememberNavController()
    TrackNavDestination(navController)
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
    val resumeVm = hiltViewModel<ActiveOrderResumeViewModel>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(pendingOrderId) {
        val orderId = pendingOrderId ?: return@LaunchedEffect
        refreshVm.triggerRefresh()
        snapshotFlow { navController.currentBackStackEntry?.destination?.route }
            .filter { route ->
                route != null &&
                    route != DobbyGoScreens.Splash &&
                    route != DobbyGoScreens.Phone &&
                    !route.startsWith("otp")
            }
            .first()
        navController.navigate(DobbyGoScreens.orderDetail(orderId)) {
            launchSingleTop = true
        }
        onPendingOrderNavigated()
    }

    suspend fun openHomeResumingActiveOrder(skipResume: Boolean) {
        navController.navigate(DobbyGoScreens.Main) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
        if (skipResume) return
        when (val resume = resumeVm.resolve()) {
            is ActiveOrderResume.PickupMap -> {
                navController.navigate(DobbyGoScreens.pickupMap(resume.orderId)) {
                    launchSingleTop = true
                }
            }
            is ActiveOrderResume.DeliveryMap -> {
                navController.navigate(DobbyGoScreens.deliveryMap(resume.orderId)) {
                    launchSingleTop = true
                }
            }
            null -> Unit
        }
    }

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
                    // Notification deep-link owns navigation when present.
                    openHomeResumingActiveOrder(skipResume = pendingOrderId != null)
                }
            )
        }
        composable(DobbyGoScreens.Phone) {
            PhoneScreen(
                onCodeSent = { phone ->
                    navController.navigate(DobbyGoScreens.otp(phone)) {
                        popUpTo(DobbyGoScreens.Phone) { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = DobbyGoScreens.Otp,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) {
            OtpScreen(
                onBack = { navController.popBackStack() },
                onVerified = {
                    scope.launch {
                        openHomeResumingActiveOrder(skipResume = false)
                    }
                }
            )
        }
        composable(DobbyGoScreens.Main) {
            val activity = LocalActivity.current
            BackHandler {
                activity?.finish()
            }
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
                refreshOrdersTrigger = refreshTrigger,
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
