package com.ares.ewe_man.presentation.ui.deliverymap

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
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
import com.ares.ewe_man.presentation.ui.components.SixDigitCodeField
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

@OptIn(ExperimentalLayoutApi::class)
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
            deliveryIcon = bitmapDescriptorFromRes(context, R.drawable.ic_nav_arrow, sizeDp = 40)
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
            val imeVisible = WindowInsets.isImeVisible
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
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
                                ?: bitmapDescriptorFromRes(context, R.drawable.ic_nav_arrow, sizeDp = 40),
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
                }

                val showDeliveryPanel =
                    delivery != null && (!uiState.isDelivered || uiState.showCustomerRating)

                // Recenter stays glued to the top of the bottom sheet (moves down when collapsed).
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    if (current != null && !imeVisible) {
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
                                .align(Alignment.Start)
                                .padding(start = 16.dp, bottom = 12.dp),
                            shape = CircleShape,
                            color = DobbyGoColors.Surface,
                            shadowElevation = 4.dp,
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Centrar mapa",
                                tint = DobbyGoColors.Purple,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    if (showDeliveryPanel) {
                        DeliveryBottomPanel(
                            modifier = Modifier.fillMaxWidth(),
                            deliveryAddress = uiState.deliveryAddress,
                            customerInstructions = uiState.customerInstructions,
                            hasMarkedArrived = uiState.hasMarkedArrived,
                            isNearDestination = uiState.isNearDestination,
                            isMarkingArrived = uiState.isMarkingArrived,
                            canCallCustomer = uiState.canCallCustomer,
                            customerPhone = uiState.customerPhone,
                            deliveryCodeInput = uiState.deliveryCodeInput,
                            deliveryCodeValid = uiState.deliveryCodeValid,
                            isVerifyingDeliveryCode = uiState.isVerifyingDeliveryCode,
                            isMarkingDelivered = uiState.isMarkingDelivered,
                            showCustomerRating = uiState.showCustomerRating,
                            customerRatingStars = uiState.customerRatingStars,
                            customerPunctual = uiState.customerPunctual,
                            customerPaysWell = uiState.customerPaysWell,
                            customerTipped = uiState.customerTipped,
                            customerRecommended = uiState.customerRecommended,
                            isSubmittingCustomerRating = uiState.isSubmittingCustomerRating,
                            compactForKeyboard = imeVisible,
                            onDeliveryCodeChange = { viewModel.onDeliveryCodeChange(it) },
                            onMarkArrived = { viewModel.markArrived() },
                            onMarkDelivered = { viewModel.markDelivered(onSuccess = onBack) },
                            onCallCustomer = { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                runCatching { context.startActivity(intent) }
                            },
                            onStarsChange = { viewModel.setCustomerRatingStars(it) },
                            onTogglePunctual = { viewModel.toggleCustomerPunctual() },
                            onTogglePaysWell = { viewModel.toggleCustomerPaysWell() },
                            onToggleTipped = { viewModel.toggleCustomerTipped() },
                            onToggleRecommended = { viewModel.toggleCustomerRecommended() },
                            onSkipCustomerRating = { viewModel.skipCustomerRating() },
                            onSubmitCustomerRating = { viewModel.submitCustomerRating() },
                        )
                    }
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
    canCallCustomer: Boolean,
    customerPhone: String?,
    deliveryCodeInput: String,
    deliveryCodeValid: Boolean?,
    isVerifyingDeliveryCode: Boolean,
    isMarkingDelivered: Boolean,
    showCustomerRating: Boolean,
    customerRatingStars: Int,
    customerPunctual: Boolean,
    customerPaysWell: Boolean,
    customerTipped: Boolean,
    customerRecommended: Boolean,
    isSubmittingCustomerRating: Boolean,
    onDeliveryCodeChange: (String) -> Unit,
    onMarkArrived: () -> Unit,
    onMarkDelivered: () -> Unit,
    onCallCustomer: (String) -> Unit,
    onStarsChange: (Int) -> Unit,
    onTogglePunctual: () -> Unit,
    onTogglePaysWell: () -> Unit,
    onToggleTipped: () -> Unit,
    onToggleRecommended: () -> Unit,
    onSkipCustomerRating: () -> Unit,
    onSubmitCustomerRating: () -> Unit,
    modifier: Modifier = Modifier,
    compactForKeyboard: Boolean = false,
) {
    val configuration = LocalConfiguration.current
    val maxPanelHeight = (configuration.screenHeightDp * if (compactForKeyboard) 0.50f else 0.72f).dp
    val scrollState = rememberScrollState()
    var sheetCollapsed by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(showCustomerRating) {
        if (showCustomerRating) sheetCollapsed = false
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(
                if (sheetCollapsed) Modifier else Modifier.heightIn(max = maxPanelHeight),
            ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = DobbyGoColors.Surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (sheetCollapsed) {
                        Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    } else {
                        Modifier
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    },
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(sheetCollapsed) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                when {
                                    dragAccum > 48f -> sheetCollapsed = true
                                    dragAccum < -48f -> sheetCollapsed = false
                                }
                                dragAccum = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                dragAccum += dragAmount
                            },
                        )
                    }
                    .clickable { sheetCollapsed = !sheetCollapsed },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(DobbyGoColors.Border),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (sheetCollapsed) "Mostrar detalles" else "Ocultar detalles",
                        style = MaterialTheme.typography.labelMedium,
                        color = DobbyGoColors.TextSecondary,
                    )
                    Icon(
                        imageVector = if (sheetCollapsed) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = DobbyGoColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (sheetCollapsed) {
                return@Column
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showCustomerRating) {
                CustomerRatingSection(
                    stars = customerRatingStars,
                    punctual = customerPunctual,
                    paysWell = customerPaysWell,
                    tipped = customerTipped,
                    recommended = customerRecommended,
                    isSubmitting = isSubmittingCustomerRating,
                    onStarsChange = onStarsChange,
                    onTogglePunctual = onTogglePunctual,
                    onTogglePaysWell = onTogglePaysWell,
                    onToggleTipped = onToggleTipped,
                    onToggleRecommended = onToggleRecommended,
                    onSkip = onSkipCustomerRating,
                    onSubmit = onSubmitCustomerRating,
                )
                return@Column
            }

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
                        if (!compactForKeyboard) {
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
                if (canCallCustomer && !customerPhone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onCallCustomer(customerPhone) },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DobbyGoColors.PurpleLight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Llamar al cliente",
                                tint = DobbyGoColors.Purple,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            if (!compactForKeyboard) {
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
                Text(
                    text = "Código del cliente",
                    style = MaterialTheme.typography.labelLarge,
                    color = DobbyGoColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SixDigitCodeField(
                    value = deliveryCodeInput,
                    onValueChange = onDeliveryCodeChange,
                )
                if (!compactForKeyboard) {
                    Text(
                        text = "Pide el código al cliente en la app Dobbi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DobbyGoColors.TextSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                when {
                    deliveryCodeInput.length == 6 && isVerifyingDeliveryCode -> {
                        Text(
                            text = "Verificando código…",
                            style = MaterialTheme.typography.bodySmall,
                            color = DobbyGoColors.TextSecondary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    deliveryCodeInput.length == 6 && deliveryCodeValid == false -> {
                        Text(
                            text = "Código incorrecto. Verifica con el cliente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val canConfirmDelivery = deliveryCodeValid == true && !isVerifyingDeliveryCode
                Button(
                    onClick = onMarkDelivered,
                    enabled = canConfirmDelivery && !isMarkingDelivered,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerRatingSection(
    stars: Int,
    punctual: Boolean,
    paysWell: Boolean,
    tipped: Boolean,
    recommended: Boolean,
    isSubmitting: Boolean,
    onStarsChange: (Int) -> Unit,
    onTogglePunctual: () -> Unit,
    onTogglePaysWell: () -> Unit,
    onToggleTipped: () -> Unit,
    onToggleRecommended: () -> Unit,
    onSkip: () -> Unit,
    onSubmit: () -> Unit,
) {
    Text(
        text = "¿Cómo estuvo el cliente?",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = DobbyGoColors.TextPrimary,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Opcional. Si calificas, sumas XP a tu score de repartidor.",
        style = MaterialTheme.typography.bodySmall,
        color = DobbyGoColors.TextSecondary,
    )
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..5).forEach { value ->
            val selected = stars >= value
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "$value estrellas",
                tint = if (selected) Color(0xFFF5A524) else DobbyGoColors.Border,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(enabled = !isSubmitting) { onStarsChange(value) }
                    .padding(4.dp),
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RatingTagChip("Usuario puntual", punctual, enabled = !isSubmitting, onClick = onTogglePunctual)
        RatingTagChip("Usuario paga bien", paysWell, enabled = !isSubmitting, onClick = onTogglePaysWell)
        RatingTagChip("Propina", tipped, enabled = !isSubmitting, onClick = onToggleTipped)
        RatingTagChip("Usuario recomendado", recommended, enabled = !isSubmitting, onClick = onToggleRecommended)
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onSubmit,
        enabled = !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DobbyGoColors.Purple,
            disabledContainerColor = DobbyGoColors.Purple.copy(alpha = 0.5f),
        ),
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = "Enviar calificación",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White,
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onSkip,
        enabled = !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = "Omitir",
            fontWeight = FontWeight.SemiBold,
            color = DobbyGoColors.TextSecondary,
        )
    }
}

@Composable
private fun RatingTagChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = DobbyGoColors.PurpleLight,
            selectedLabelColor = DobbyGoColors.Purple,
        ),
    )
}
