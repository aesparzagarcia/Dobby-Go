package com.ares.ewe_man.presentation.ui.deliverymap

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe_man.presentation.viewmodel.deliverymap.DeliveryMapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.ares.ewe_man.R
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

private const val FOLLOW_ZOOM = 17f
/** Perspective similar to navigation apps (Waze / Google Maps driving). */
private const val NAVIGATION_TILT_DEG = 58f
/** If [ic_delivery] is drawn facing “right” in the PNG instead of “up”, set e.g. -90f. */
private const val DRIVER_ICON_ROTATION_OFFSET_DEG = 0f
private const val MARKER_ICON_SIZE_DP = 48

private fun bitmapDescriptorFromRes(context: Context, resId: Int, sizeDp: Int = MARKER_ICON_SIZE_DP): BitmapDescriptor? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()
    drawable.setBounds(0, 0, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryMapScreen(
    onBack: () -> Unit,
    viewModel: DeliveryMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var houseIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var deliveryIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
            || permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
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
    val delivery = uiState.deliveryLatLng
    val current = uiState.currentLocation
    val riderMarkerState = remember { MarkerState(LatLng(0.0, 0.0)) }

    // Heading-up: camera bearing tracks smoothed course so “forward” stays toward the top of the phone.
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
                title = { Text("Envío en camino") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading && uiState.currentLocation == null && uiState.deliveryLatLng == null) {
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
                    onMapLoaded = {
                        if (houseIcon == null) {
                            houseIcon = bitmapDescriptorFromRes(context, R.drawable.ic_house)
                        }
                        if (deliveryIcon == null) {
                            deliveryIcon = bitmapDescriptorFromRes(context, R.drawable.ic_delivery)
                        }
                    }
                ) {
                    if (uiState.routePoints.isNotEmpty()) {
                        Polyline(
                            points = uiState.routePoints,
                            color = Color(0xFF1976D2),
                            width = 12f
                        )
                    }
                    delivery?.let { latLng ->
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "Dirección de entrega",
                            snippet = uiState.deliveryAddress,
                            icon = houseIcon
                        )
                    }
                    if (current != null) {
                        Marker(
                            state = riderMarkerState,
                            title = "Tu ubicación",
                            icon = deliveryIcon,
                            rotation = uiState.headingDegrees + DRIVER_ICON_ROTATION_OFFSET_DEG,
                            flat = true
                        )
                    }
                }
                if (delivery != null && current != null &&
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
                                    text = "Llegada estimada: $eta",
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
                                    text = "Distancia restante: $dist",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                if (delivery == null && uiState.deliveryAddress != null) {
                    Text(
                        text = "Dirección sin coordenadas en el mapa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
                if (delivery != null && !uiState.isDelivered) {
                    Button(
                        onClick = { viewModel.markDelivered(onSuccess = onBack) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = uiState.isNearDestination && !uiState.isMarkingDelivered
                    ) {
                        if (uiState.isMarkingDelivered) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(24.dp).padding(8.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (uiState.isNearDestination)
                                    "Llegué con tu pedido"
                                else
                                    "Acércate a la dirección para notificar"
                            )
                        }
                    }
                }
            }
        }
    }
}
