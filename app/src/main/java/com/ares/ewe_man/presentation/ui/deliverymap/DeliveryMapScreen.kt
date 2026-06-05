package com.ares.ewe_man.presentation.ui.deliverymap

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe_man.R
import com.ares.ewe_man.core.theme.DobbyGoColors
import com.ares.ewe_man.core.util.splitDeliveryAddressForDisplay
import com.ares.ewe_man.presentation.ui.map.ObserveMapGesturesDisableFollow
import com.ares.ewe_man.presentation.ui.map.animateToRider
import com.ares.ewe_man.presentation.viewmodel.deliverymap.DeliveryMapViewModel
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private const val DRIVER_ICON_ROTATION_OFFSET_DEG = 0f
private const val MARKER_ICON_SIZE_DP = 48

private fun bitmapDescriptorFromRes(
    context: Context,
    resId: Int,
    sizeDp: Int = MARKER_ICON_SIZE_DP,
): BitmapDescriptor? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()
    drawable.setBounds(0, 0, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun formatEtaDisplay(eta: String?): String {
    if (eta.isNullOrBlank()) return "--"
    return eta
        .replace(" mins", " min", ignoreCase = true)
        .replace(" minutos", " min", ignoreCase = true)
}

@Composable
fun DeliveryMapScreen(
    onBack: () -> Unit,
    viewModel: DeliveryMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var houseIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var deliveryIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.loadData()
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
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
    var followRider by remember { mutableStateOf(true) }
    ObserveMapGesturesDisableFollow(cameraPositionState) { followRider = it }
    val delivery = uiState.deliveryLatLng
    val current = uiState.currentLocation
    val riderMarkerState = remember { MarkerState(LatLng(0.0, 0.0)) }

    LaunchedEffect(Unit) {
        if (houseIcon == null) {
            houseIcon = bitmapDescriptorFromRes(context, R.drawable.ic_house)
        }
        if (deliveryIcon == null) {
            deliveryIcon = bitmapDescriptorFromRes(context, R.drawable.ic_delivery)
        }
    }

    LaunchedEffect(current?.latitude, current?.longitude, uiState.headingDegrees, followRider) {
        current?.let { latLng ->
            riderMarkerState.position = latLng
            if (followRider) {
                cameraPositionState.animateToRider(latLng, uiState.headingDegrees)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DobbyGoColors.Background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        topBar = {
            DeliveryMapTopBar(
                onBack = onBack,
                onHelp = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Sigue la ruta hasta el cliente. El botón se habilitará cuando estés en la ubicación.",
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading && uiState.currentLocation == null && uiState.deliveryLatLng == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DobbyGoColors.Purple)
                    Text(
                        text = "Cargando mapa...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DobbyGoColors.TextSecondary,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapType = MapType.NORMAL,
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = false,
                        compassEnabled = false,
                    ),
                ) {
                    if (uiState.routePoints.isNotEmpty()) {
                        Polyline(
                            points = uiState.routePoints,
                            color = DobbyGoColors.Purple,
                            width = 10f,
                        )
                    }
                    delivery?.let { latLng ->
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "Dirección de entrega",
                            snippet = uiState.deliveryAddress,
                            icon = houseIcon ?: bitmapDescriptorFromRes(context, R.drawable.ic_house),
                        )
                    }
                    if (current != null) {
                        Marker(
                            state = riderMarkerState,
                            title = "Tu ubicación",
                            icon = deliveryIcon
                                ?: bitmapDescriptorFromRes(context, R.drawable.ic_delivery),
                            rotation = uiState.headingDegrees + DRIVER_ICON_ROTATION_OFFSET_DEG,
                            flat = true,
                        )
                    }
                }

                DeliveryRouteInfoCard(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    etaText = formatEtaDisplay(uiState.etaText),
                    distanceText = uiState.remainingDistanceText ?: "--",
                    etaIsApproximate = uiState.etaIsApproximate,
                )

                if (current != null) {
                    Surface(
                        onClick = {
                            followRider = true
                            scope.launch {
                                cameraPositionState.animateToRider(
                                    current,
                                    uiState.headingDegrees,
                                    durationMs = 300,
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 16.dp),
                        shape = CircleShape,
                        color = DobbyGoColors.Surface,
                        shadowElevation = 4.dp,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Centrar mapa",
                            tint = DobbyGoColors.Purple,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                if (delivery == null && uiState.deliveryAddress != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = DobbyGoColors.Surface,
                        shadowElevation = 4.dp,
                    ) {
                        Text(
                            text = "Dirección sin coordenadas en el mapa",
                            style = MaterialTheme.typography.bodySmall,
                            color = DobbyGoColors.TextSecondary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else if (delivery != null && !uiState.isDelivered) {
                    DeliveryBottomPanel(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        deliveryAddress = uiState.deliveryAddress,
                        customerInstructions = uiState.customerInstructions,
                        hasMarkedArrived = uiState.hasMarkedArrived,
                        isNearDestination = uiState.isNearDestination,
                        isMarkingArrived = uiState.isMarkingArrived,
                        isMarkingDelivered = uiState.isMarkingDelivered,
                        onMarkArrived = { viewModel.markArrived() },
                        onMarkDelivered = { viewModel.markDelivered(onSuccess = onBack) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryMapTopBar(
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DobbyGoColors.Surface)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = DobbyGoColors.TextPrimary,
            )
        }
        Text(
            text = "Envío en camino",
            modifier = Modifier.align(Alignment.Center),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DobbyGoColors.TextPrimary,
        )
        IconButton(
            onClick = onHelp,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = "Ayuda",
                tint = DobbyGoColors.TextSecondary,
            )
        }
    }
    HorizontalDivider(color = DobbyGoColors.Border, thickness = 1.dp)
}

@Composable
private fun DeliveryRouteInfoCard(
    etaText: String,
    distanceText: String,
    etaIsApproximate: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyGoColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DeliveryRouteStatRow(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccessTime,
                    label = "Llegada estimada",
                    value = etaText,
                    valueColor = DobbyGoColors.Purple,
                )
                DeliveryRouteStatRow(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Navigation,
                    label = "Distancia restante",
                    value = distanceText,
                    valueColor = DobbyGoColors.TextPrimary,
                )
            }
            if (etaIsApproximate) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tiempo aproximado (sin tráfico)",
                    style = MaterialTheme.typography.labelSmall,
                    color = DobbyGoColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun DeliveryRouteStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DobbyGoColors.PurpleLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DobbyGoColors.Purple,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DobbyGoColors.TextSecondary,
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = valueColor,
            )
        }
    }
}

@Composable
private fun DeliveryBottomPanel(
    deliveryAddress: String?,
    customerInstructions: String?,
    hasMarkedArrived: Boolean,
    isNearDestination: Boolean,
    isMarkingArrived: Boolean,
    isMarkingDelivered: Boolean,
    onMarkArrived: () -> Unit,
    onMarkDelivered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = DobbyGoColors.Surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DobbyGoColors.Border),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DobbyGoColors.PurpleLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = DobbyGoColors.Purple,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dirección de entrega",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DobbyGoColors.TextPrimary,
                    )
                    deliveryAddress?.takeIf { it.isNotBlank() }?.let { raw ->
                        val (streetLine, regionLine) = splitDeliveryAddressForDisplay(raw)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = streetLine,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DobbyGoColors.TextPrimary,
                            lineHeight = 20.sp,
                        )
                        regionLine?.let { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                color = DobbyGoColors.TextSecondary,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }

            customerInstructions?.trim()?.takeIf { it.isNotBlank() }?.let { instructions ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DobbyGoColors.PurpleLight,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DobbyGoColors.Purple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = DobbyGoColors.Purple,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Indicación del cliente",
                                fontWeight = FontWeight.Bold,
                                color = DobbyGoColors.Purple,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = instructions,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DobbyGoColors.TextSecondary,
                                lineHeight = 20.sp,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!hasMarkedArrived) {
                val canMarkArrived = isNearDestination && !isMarkingArrived
                Button(
                    onClick = onMarkArrived,
                    enabled = canMarkArrived,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DobbyGoColors.Purple,
                        disabledContainerColor = DobbyGoColors.Border,
                        contentColor = Color.White,
                        disabledContentColor = DobbyGoColors.TextSecondary,
                    ),
                ) {
                    if (isMarkingArrived) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Llegué al destino",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                }
                if (!canMarkArrived && !isMarkingArrived) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "El botón se habilitará cuando estés en la ubicación del cliente.",
                        style = MaterialTheme.typography.labelSmall,
                        color = DobbyGoColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Button(
                    onClick = onMarkDelivered,
                    enabled = !isMarkingDelivered,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DobbyGoColors.Purple,
                        disabledContainerColor = DobbyGoColors.Purple.copy(alpha = 0.5f),
                    ),
                ) {
                    if (isMarkingDelivered) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confirmar entrega",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
