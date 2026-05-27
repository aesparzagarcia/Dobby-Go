package com.ares.ewe_man.presentation.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ares.ewe_man.core.theme.DobbyGoColors
import com.ares.ewe_man.core.util.splitDeliveryAddressForDisplay
import com.ares.ewe_man.data.remote.model.DeliveryOrderDto
import com.ares.ewe_man.presentation.ui.profile.resolveProfileImageUrl
import com.ares.ewe_man.presentation.viewmodel.orders.OrdersTab
import com.ares.ewe_man.presentation.viewmodel.orders.OrdersViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private data class OrdersTabVisual(
    val tab: OrdersTab,
    val label: String,
    val icon: ImageVector,
)

private val ordersTabs = listOf(
    OrdersTabVisual(OrdersTab.OPEN, "Abiertos", Icons.Default.ShoppingBag),
    OrdersTabVisual(OrdersTab.ASSIGNED, "Asignados", Icons.Outlined.Assignment),
    OrdersTabVisual(OrdersTab.CLOSED, "Cerrados", Icons.Default.CheckCircle),
)

private fun tabSectionSubtitle(tab: OrdersTab, count: Int): String = when (tab) {
    OrdersTab.OPEN -> if (count == 1) {
        "Tienes 1 pedido disponible"
    } else {
        "Tienes $count pedidos disponibles"
    }
    OrdersTab.ASSIGNED -> if (count == 1) {
        "Tienes 1 pedido en curso"
    } else {
        "Tienes $count pedidos en curso"
    }
    OrdersTab.CLOSED -> if (count == 1) {
        "1 pedido completado"
    } else {
        "$count pedidos completados"
    }
}

private fun emptyMessage(tab: OrdersTab): String = when (tab) {
    OrdersTab.OPEN -> "No hay pedidos abiertos para recoger"
    OrdersTab.ASSIGNED -> "No tienes pedidos asignados"
    OrdersTab.CLOSED -> "No tienes pedidos cerrados"
}

private fun homeStatusLabel(status: String): String = when (status.uppercase()) {
    "ONLINE" -> "En línea"
    "ON_DELIVERY" -> "En reparto"
    "OFFLINE" -> "Desconectado"
    else -> status
}

private fun formatOrderDate(createdAt: String): String {
    return try {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val date = iso.parse(createdAt) ?: return createdAt
        SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        try {
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val date = iso.parse(createdAt) ?: return createdAt
            SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(date)
        } catch (_: Exception) {
            createdAt
        }
    }
}

private fun orderStatusLabel(status: String): String = when (status) {
    "READY_FOR_PICKUP" -> "Listo para recoger"
    "ASSIGNED" -> "Asignado"
    "ON_DELIVERY" -> "En camino"
    "DELIVERED" -> "Entregado"
    else -> status
}

private fun orderStatusColors(status: String): Pair<Color, Color> = when (status) {
    "READY_FOR_PICKUP" -> DobbyGoColors.Blue to DobbyGoColors.BlueLight
    "ASSIGNED" -> DobbyGoColors.Purple to DobbyGoColors.PurpleLight
    "ON_DELIVERY" -> DobbyGoColors.Blue to DobbyGoColors.BlueLight
    "DELIVERED" -> DobbyGoColors.Green to DobbyGoColors.GreenLight
    else -> DobbyGoColors.TextSecondary to DobbyGoColors.Border
}

private fun orderDisplayNumber(orderId: String): String {
    val suffix = orderId.takeLast(4).uppercase(Locale.getDefault())
    return "#$suffix"
}

@Composable
fun OrdersScreen(
    onOrderClick: (orderId: String) -> Unit,
    refreshTrigger: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            viewModel.refresh()
            viewModel.refreshProfileHeader()
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.orders.size) {
        val msg = uiState.errorMessage ?: return@LaunchedEffect
        if (uiState.orders.isNotEmpty()) {
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    val firstName = uiState.deliveryManDisplayName
        ?.trim()
        ?.substringBefore(' ')
        ?.takeIf { it.isNotBlank() }
        ?: "Repartidor"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DobbyGoColors.Background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.isLoading && uiState.orders.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = DobbyGoColors.Purple)
                }
            }
            uiState.errorMessage != null && uiState.orders.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .padding(horizontal = 24.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadOrders() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    item {
                        OrdersHomeHeader(
                            firstName = firstName,
                            profilePhotoUrl = uiState.profilePhotoUrl,
                            connectionStatus = uiState.connectionStatus,
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refresh() },
                        )
                    }
                    item {
                        OrdersTabRow(
                            selectedTab = uiState.selectedTab,
                            onTabSelected = viewModel::setTab,
                        )
                    }
                    item {
                        OrdersSectionHeader(
                            subtitle = tabSectionSubtitle(uiState.selectedTab, uiState.orders.size),
                        )
                    }
                    if (uiState.orders.isEmpty()) {
                        item {
                            Text(
                                text = emptyMessage(uiState.selectedTab),
                                style = MaterialTheme.typography.bodyLarge,
                                color = DobbyGoColors.TextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 32.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        items(uiState.orders, key = { it.id }) { order ->
                            DeliveryOrderCard(
                                order = order,
                                onClick = { onOrderClick(order.id) },
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersHomeHeader(
    firstName: String,
    profilePhotoUrl: String?,
    connectionStatus: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val isOnline = connectionStatus.uppercase() in setOf("ONLINE", "ON_DELIVERY")
    val statusDotColor = if (isOnline) DobbyGoColors.Green else DobbyGoColors.TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            name = firstName,
            photoUrl = profilePhotoUrl,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "¡Hola, $firstName! 👋",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DobbyGoColors.PurpleDark,
            )
            Text(
                text = "Aquí tienes tus pedidos",
                style = MaterialTheme.typography.bodyMedium,
                color = DobbyGoColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = DobbyGoColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DobbyGoColors.Border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusDotColor),
                    )
                    Text(
                        text = homeStatusLabel(connectionStatus),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = DobbyGoColors.TextPrimary,
                    )
                }
            }
        }
        Surface(
            shape = CircleShape,
            color = DobbyGoColors.PurpleLight,
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onRefresh),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = DobbyGoColors.Purple,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refrescar",
                        tint = DobbyGoColors.Purple,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, photoUrl: String?) {
    val resolvedUrl = resolveProfileImageUrl(photoUrl)
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    if (resolvedUrl != null) {
        AsyncImage(
            model = resolvedUrl,
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(2.dp, DobbyGoColors.PurpleLight, CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(DobbyGoColors.PurpleLight)
                .border(2.dp, DobbyGoColors.Purple.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = DobbyGoColors.Purple,
            )
        }
    }
}

@Composable
private fun OrdersTabRow(
    selectedTab: OrdersTab,
    onTabSelected: (OrdersTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        ordersTabs.forEach { tabVisual ->
            val selected = selectedTab == tabVisual.tab
            val contentColor = if (selected) DobbyGoColors.Purple else DobbyGoColors.TextSecondary

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tabVisual.tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = tabVisual.icon,
                    contentDescription = tabVisual.label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tabVisual.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(if (selected) 3.dp else 0.dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(DobbyGoColors.Purple),
                )
            }
        }
    }
    HorizontalDivider(color = DobbyGoColors.Border, thickness = 1.dp)
}

@Composable
private fun OrdersSectionHeader(subtitle: String) {
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = DobbyGoColors.TextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun DeliveryOrderCard(
    order: DeliveryOrderDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(order.id) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "orderCardChevron",
    )
    val (statusTextColor, statusBg) = orderStatusColors(order.status)
    val (addressLine1, addressLine2) = order.deliveryAddress?.let { splitDeliveryAddressForDisplay(it) }
        ?: ("Sin dirección" to null)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyGoColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = DobbyGoColors.PurpleLight,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = DobbyGoColors.Purple,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = formatOrderDate(order.createdAt),
                                style = MaterialTheme.typography.labelMedium,
                                color = DobbyGoColors.PurpleDark,
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusBg,
                    ) {
                        Text(
                            text = orderStatusLabel(order.status),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusTextColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DobbyGoColors.PurpleLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = DobbyGoColors.Purple,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = order.shopName ?: "Tienda",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DobbyGoColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (expanded) "Colapsar" else "Expandir",
                        tint = DobbyGoColors.TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = orderDisplayNumber(order.id),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = DobbyGoColors.TextSecondary,
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))

                    DottedDivider(modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Total del pedido",
                        style = MaterialTheme.typography.bodySmall,
                        color = DobbyGoColors.TextSecondary,
                    )
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%.2f", order.total)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DobbyGoColors.TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DobbyGoColors.PurpleLight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = DobbyGoColors.Purple,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = addressLine1,
                                fontWeight = FontWeight.SemiBold,
                                color = DobbyGoColors.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (addressLine2 != null) {
                                Text(
                                    text = addressLine2,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DobbyGoColors.TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DobbyGoColors.Purple),
                    ) {
                        Text(
                            text = "Ver detalles del pedido",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DottedDivider(modifier: Modifier = Modifier) {
    val color = DobbyGoColors.Border
    Box(
        modifier = modifier
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
                )
            },
    )
}
