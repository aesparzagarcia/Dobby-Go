package com.ares.ewe_man.presentation.ui.pickupmap

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe_man.presentation.viewmodel.pickupmap.PickupMapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

private const val FOLLOW_ZOOM = 17f
private const val NAVIGATION_TILT_DEG = 58f
private const val DRIVER_ICON_ROTATION_OFFSET_DEG = 0f
private fun formatStraightLineDistance(meters: Double): String =
    if (meters >= 1000) {
        String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
    } else {
        String.format(Locale.getDefault(), "%.0f m", meters)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupMapScreen(
    onBack: () -> Unit,
    onComenzarEnvio: () -> Unit = {},
    viewModel: PickupMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.loadData()
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val cameraPositionState = rememberCameraPositionState()
    val pickup = uiState.pickupLatLng
    val current = uiState.currentLocation
    val riderMarkerState = remember { MarkerState(LatLng(0.0, 0.0)) }

    LaunchedEffect(current?.latitude, current?.longitude, uiState.headingDegrees) {
        current?.let { latLng ->
            riderMarkerState.position = latLng
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder()
                        .target(latLng)
                        .zoom(FOLLOW_ZOOM)
                        .bearing(uiState.headingDegrees)
                        .tilt(NAVIGATION_TILT_DEG)
                        .build()
                ),
                durationMs = 160
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ruta al restaurante") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading && uiState.currentLocation == null && uiState.pickupLatLng == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = "Cargando mapa...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = true
                    ),
                ) {
                    if (uiState.routePoints.isNotEmpty()) {
                        Polyline(
                            points = uiState.routePoints,
                            color = Color(0xFF1976D2),
                            width = 12f
                        )
                    }
                    pickup?.let { latLng ->
                        Marker(
                            state = MarkerState(position = latLng),
                            title = uiState.pickupTitle ?: "Recoger pedido",
                            snippet = uiState.pickupAddress,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                        )
                    }
                    if (current != null) {
                        Marker(
                            state = riderMarkerState,
                            title = "Tu ubicación",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            rotation = uiState.headingDegrees + DRIVER_ICON_ROTATION_OFFSET_DEG,
                            flat = true
                        )
                    }
                }
                if (pickup != null && current != null &&
                    (uiState.etaText != null || uiState.remainingDistanceText != null)
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.etaText?.let { eta ->
                                Text(
                                    text = "Tiempo estimado: $eta",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (uiState.etaIsApproximate) {
                                    Text(
                                        text = "Tiempo aproximado (línea recta, sin tráfico)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            uiState.remainingDistanceText?.let { dist ->
                                Text(
                                    text = "Distancia: $dist",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                if (pickup == null) {
                    Text(
                        text = if (uiState.pickupAddress != null) {
                            "No se pudo geocodificar la dirección del restaurante. Revisa la dirección en el detalle del pedido."
                        } else {
                            "Este pedido no tiene datos de tienda para mostrar la ruta de recogida."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                } else if (current != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val distRest = uiState.distanceToPickupMeters
                            if (!uiState.canStartDelivery && distRest != null) {
                                val cliente = uiState.customerName?.takeIf { it.isNotBlank() } ?: "el cliente"
                                Text(
                                    text = "Acércate al restaurante. Te faltan aprox. ${formatStraightLineDistance(distRest)} para recoger el pedido de $cliente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.startDelivery(onSuccess = onComenzarEnvio)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState.canStartDelivery && !uiState.isStartingDelivery
                            ) {
                                if (uiState.isStartingDelivery) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(8.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Comenzar envío")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
