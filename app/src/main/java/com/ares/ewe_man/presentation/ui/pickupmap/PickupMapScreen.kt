package com.ares.ewe_man.presentation.ui.pickupmap

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.HelpOutline
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe_man.R
import com.ares.ewe_man.core.theme.DobbyGoColors
import com.ares.ewe_man.presentation.viewmodel.pickupmap.PickupMapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
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
import kotlinx.coroutines.launch
import java.util.Locale

private const val FOLLOW_ZOOM = 17f
private const val NAVIGATION_TILT_DEG = 58f
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

private fun formatCustomerFullName(firstName: String?, lastName: String?): String {
    val parts = listOfNotNull(
        firstName?.trim()?.takeIf { it.isNotBlank() },
        lastName?.trim()?.takeIf { it.isNotBlank() },
    )
    return parts.joinToString(" ").ifBlank { "el cliente" }
}

private fun orderDisplayNumber(orderId: String): String {
    val suffix = orderId.takeLast(4).uppercase(Locale.getDefault())
    return "#$suffix"
}

@Composable
fun PickupMapScreen(
    onBack: () -> Unit,
    onComenzarEnvio: () -> Unit = {},
    viewModel: PickupMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
    val pickup = uiState.pickupLatLng
    val current = uiState.currentLocation
    val riderMarkerState = remember { MarkerState(LatLng(0.0, 0.0)) }
    var shopIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var deliveryIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(Unit) {
        if (shopIcon == null) {
            shopIcon = bitmapDescriptorFromRes(context, R.drawable.ic_shop)
        }
        if (deliveryIcon == null) {
            deliveryIcon = bitmapDescriptorFromRes(context, R.drawable.ic_delivery)
        }
    }

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
                        .build(),
                ),
                durationMs = 160,
            )
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
            PickupMapTopBar(
                onBack = onBack,
                onHelp = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Sigue la ruta hasta el restaurante. Cuando estés cerca, podrás comenzar el envío.",
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading && uiState.currentLocation == null && uiState.pickupLatLng == null) {
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
                    pickup?.let { latLng ->
                        Marker(
                            state = MarkerState(position = latLng),
                            title = uiState.pickupTitle ?: "Restaurante",
                            snippet = uiState.pickupAddress,
                            icon = shopIcon ?: bitmapDescriptorFromRes(context, R.drawable.ic_shop),
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

                PickupRouteInfoCard(
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
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.builder()
                                            .target(current)
                                            .zoom(FOLLOW_ZOOM)
                                            .bearing(uiState.headingDegrees)
                                            .tilt(NAVIGATION_TILT_DEG)
                                            .build(),
                                    ),
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

                if (pickup == null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = DobbyGoColors.Surface,
                        shadowElevation = 4.dp,
                    ) {
                        Text(
                            text = if (uiState.pickupAddress != null) {
                                "No se pudo geocodificar la dirección del restaurante."
                            } else {
                                "Este pedido no tiene datos de tienda para mostrar la ruta."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = DobbyGoColors.TextSecondary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                        PickupBottomPanel(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            orderId = viewModel.orderId,
                            customerName = uiState.customerName,
                            customerLastName = uiState.customerLastName,
                            canStartDelivery = uiState.canStartDelivery,
                            isStartingDelivery = uiState.isStartingDelivery,
                            onStartDelivery = {
                                viewModel.startDelivery(onSuccess = onComenzarEnvio)
                            },
                        )
                }
            }
        }
    }
}

@Composable
private fun PickupMapTopBar(
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
            text = "Ruta al restaurante",
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
private fun PickupRouteInfoCard(
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PickupStatItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccessTime,
                    label = "Tiempo estimado",
                    value = etaText,
                )
                PickupStatItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Navigation,
                    label = "Distancia",
                    value = distanceText,
                )
            }
            if (etaIsApproximate) {
                Spacer(modifier = Modifier.height(4.dp))
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
private fun PickupStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
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
                color = DobbyGoColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun PickupBottomPanel(
    orderId: String,
    customerName: String?,
    customerLastName: String?,
    canStartDelivery: Boolean,
    isStartingDelivery: Boolean,
    onStartDelivery: () -> Unit,
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

            Spacer(modifier = Modifier.height(10.dp))

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
                            text = "Indicaciones para recoger",
                            fontWeight = FontWeight.Bold,
                            color = DobbyGoColors.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val cliente = formatCustomerFullName(customerName, customerLastName)
                        val pedidoNumero = orderDisplayNumber(orderId)
                        Text(
                            text = buildAnnotatedString {
                                append("Preséntate en el mostrador y menciona que vas por el pedido ")
                                withStyle(
                                    SpanStyle(
                                        color = DobbyGoColors.Purple,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                ) {
                                    append(pedidoNumero)
                                }
                                append(" de ")
                                withStyle(
                                    SpanStyle(
                                        color = DobbyGoColors.Purple,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                ) {
                                    append(cliente)
                                }
                                append(".")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = DobbyGoColors.TextSecondary,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onStartDelivery,
                enabled = canStartDelivery && !isStartingDelivery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DobbyGoColors.Purple,
                    disabledContainerColor = DobbyGoColors.Purple.copy(alpha = 0.45f),
                ),
            ) {
                if (isStartingDelivery) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Comenzar envío",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
