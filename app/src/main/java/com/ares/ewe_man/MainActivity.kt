package com.ares.ewe_man

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.ares.ewe_man.core.theme.DobbyGoTheme
import com.ares.ewe_man.presentation.ui.navigation.DobbyGoNavigation
import com.ares.ewe_man.realtime.DeliveryOrderNotificationHelper
import com.ares.ewe_man.realtime.DeliveryRealtimeCoordinator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var deliveryRealtimeCoordinator: DeliveryRealtimeCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var pendingOrderId by mutableStateOf<String?>(null)

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        syncRealtime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeOrderIdFromIntent(intent)
        enableEdgeToEdge()
        // Light app UI: dark status/nav bar icons even when the phone is in system dark mode
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        requestNotifPermissionIfNeeded()
        setContent {
            DobbyGoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DobbyGoNavigation(
                        pendingOrderId = pendingOrderId,
                        onPendingOrderNavigated = { pendingOrderId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeOrderIdFromIntent(intent)
    }

    private fun consumeOrderIdFromIntent(intent: Intent?) {
        if (intent == null) return
        val id = intent.getStringExtra(DeliveryOrderNotificationHelper.EXTRA_ORDER_ID)
            ?: intent.extras?.getString("order_id")
        val trimmed = id?.trim()?.takeIf { it.isNotEmpty() } ?: return
        pendingOrderId = trimmed
        intent.removeExtra(DeliveryOrderNotificationHelper.EXTRA_ORDER_ID)
        intent.extras?.remove("order_id")
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            syncRealtime()
            return
        }
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED -> syncRealtime()
            else -> notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun syncRealtime() {
        scope.launch {
            deliveryRealtimeCoordinator.onSessionReady()
        }
    }
}
