package com.ares.ewe_man.presentation.ui.orderdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ares.ewe_man.core.theme.DobbyGoColors
import com.ares.ewe_man.core.util.absoluteUploadUrl
import com.ares.ewe_man.core.util.splitDeliveryAddressForDisplay
import com.ares.ewe_man.data.remote.model.DeliveryOrderDto
import com.ares.ewe_man.data.remote.model.DeliveryOrderItemDto
import com.ares.ewe_man.presentation.viewmodel.orderdetail.OrderDetailViewModel
import com.ares.ewe_man.core.util.OrderDateFormat
import java.util.Locale

private fun formatOrderDate(createdAt: String): String = OrderDateFormat.format(createdAt)

private fun statusLabel(status: String): String = when (status) {
    "READY_FOR_PICKUP" -> "Listo para recoger"
    "ASSIGNED" -> "Asignado"
    "ON_DELIVERY" -> "En camino"
    "DELIVERED" -> "Entregado"
    else -> status
}

private fun formatCustomerFullName(order: DeliveryOrderDto): String? {
    val parts = listOfNotNull(
        order.customerName?.trim()?.takeIf { it.isNotBlank() },
        order.customerLastName?.trim()?.takeIf { it.isNotBlank() },
    )
    return parts.joinToString(" ").takeIf { it.isNotBlank() }
}

private fun orderStatusColors(status: String): Pair<Color, Color> = when (status) {
    "READY_FOR_PICKUP" -> DobbyGoColors.Blue to DobbyGoColors.BlueLight
    "ASSIGNED" -> DobbyGoColors.Purple to DobbyGoColors.PurpleLight
    "ON_DELIVERY" -> DobbyGoColors.Blue to DobbyGoColors.BlueLight
    "DELIVERED" -> DobbyGoColors.Green to DobbyGoColors.GreenLight
    else -> DobbyGoColors.TextSecondary to DobbyGoColors.Border
}

@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onOpenMap: (orderId: String) -> Unit = {},
    onOpenPickupMap: (orderId: String) -> Unit = {},
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DobbyGoColors.Background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            OrderDetailTopBar(onBack = onBack)
        },
        bottomBar = {
            uiState.order?.let { order ->
                OrderDetailBottomActions(
                    order = order,
                    isAssigning = uiState.isAssigning,
                    assignEnabled = !uiState.assignBlocked,
                    assignBlockedMessage = uiState.assignBlockedMessage,
                    onAssignToMe = {
                        viewModel.assignToMe(
                            onSuccess = { onOpenPickupMap(viewModel.orderId) },
                        )
                    },
                    onOpenPickupMap = { onOpenPickupMap(viewModel.orderId) },
                    onOpenMap = { onOpenMap(viewModel.orderId) },
                )
            }
        },
    ) { padding ->
        when {
            uiState.isLoading && uiState.order == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = DobbyGoColors.Purple)
                }
            }
            uiState.order == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No se pudo cargar el pedido",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DobbyGoColors.TextSecondary,
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    OrderDetailCard(
                        order = uiState.order!!,
                        assignBlocked = uiState.assignBlocked,
                        assignBlockedMessage = uiState.assignBlockedMessage,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailTopBar(onBack: () -> Unit) {
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
            text = "Detalle del pedido",
            modifier = Modifier.align(Alignment.Center),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DobbyGoColors.TextPrimary,
        )
    }
    HorizontalDivider(color = DobbyGoColors.Border, thickness = 1.dp)
}

@Composable
private fun OrderDetailCard(
    order: DeliveryOrderDto,
    assignBlocked: Boolean = false,
    assignBlockedMessage: String? = null,
) {
    val (statusTextColor, statusBg) = orderStatusColors(order.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyGoColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DobbyGoColors.Background,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = DobbyGoColors.TextSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = formatOrderDate(order.createdAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = DobbyGoColors.TextSecondary,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBg,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = statusTextColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = statusLabel(order.status),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusTextColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
            }

            formatCustomerFullName(order)?.let { customer ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Cliente: ")
                        withStyle(SpanStyle(color = DobbyGoColors.Purple, fontWeight = FontWeight.SemiBold)) {
                            append(customer)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = DobbyGoColors.TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DobbyGoColors.Border, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            order.deliveryAddress?.takeIf { it.isNotBlank() }?.let { raw ->
                val (streetLine, regionLine) = splitDeliveryAddressForDisplay(raw)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = DobbyGoColors.Purple,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Dirección de entrega",
                        fontWeight = FontWeight.SemiBold,
                        color = DobbyGoColors.Purple,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = streetLine,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DobbyGoColors.TextPrimary,
                    lineHeight = 20.sp,
                )
                regionLine?.let { line ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DobbyGoColors.TextSecondary,
                        lineHeight = 20.sp,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = DobbyGoColors.Purple,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Productos",
                    fontWeight = FontWeight.SemiBold,
                    color = DobbyGoColors.Purple,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, DobbyGoColors.Border, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                order.items.forEach { item ->
                    OrderProductRow(item = item)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            DottedDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Envío",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DobbyGoColors.TextSecondary,
                )
                Text(
                    text = "$${String.format(Locale.getDefault(), "%.2f", order.deliveryFee)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DobbyGoColors.TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total del pedido",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DobbyGoColors.TextSecondary,
                )
                Text(
                    text = "$${String.format(Locale.getDefault(), "%.2f", order.total)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DobbyGoColors.Purple,
                )
            }

            if (order.status == "READY_FOR_PICKUP") {
                Spacer(modifier = Modifier.height(10.dp))
                if (assignBlocked && !assignBlockedMessage.isNullOrBlank()) {
                    AssignBlockedInfoBox(message = assignBlockedMessage)
                } else {
                    OrderReminderBox()
                }
            }
        }
    }
}

@Composable
private fun AssignBlockedInfoBox(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DobbyGoColors.Orange.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, DobbyGoColors.Orange.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = DobbyGoColors.Orange,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = DobbyGoColors.TextPrimary,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun OrderProductRow(item: DeliveryOrderItemDto) {
    val name = item.productName ?: "Producto"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DobbyGoColors.PurpleLight),
            contentAlignment = Alignment.Center,
        ) {
            val imageUrl = item.imageUrl?.takeIf { it.isNotBlank() }
            if (imageUrl != null) {
                AsyncImage(
                    model = absoluteUploadUrl(imageUrl),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = DobbyGoColors.Purple.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName ?: "Producto",
                fontWeight = FontWeight.SemiBold,
                color = DobbyGoColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "x${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = DobbyGoColors.TextSecondary,
            )
        }
        Text(
            text = "$${String.format(Locale.getDefault(), "%.2f", item.price * item.quantity)}",
            fontWeight = FontWeight.SemiBold,
            color = DobbyGoColors.TextPrimary,
        )
    }
}

@Composable
private fun OrderReminderBox() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DobbyGoColors.PurpleLight,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DobbyGoColors.Purple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = DobbyGoColors.Purple,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    text = "Recuerda",
                    fontWeight = FontWeight.Bold,
                    color = DobbyGoColors.PurpleDark,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Verifica los productos y la dirección antes de asignar el pedido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DobbyGoColors.TextSecondary,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun OrderDetailBottomActions(
    order: DeliveryOrderDto,
    isAssigning: Boolean,
    assignEnabled: Boolean,
    assignBlockedMessage: String?,
    onAssignToMe: () -> Unit,
    onOpenPickupMap: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val (label, onClick, showCheckIcon) = when (order.status) {
        "READY_FOR_PICKUP" -> Triple("Asignar a mí", onAssignToMe, true)
        "ASSIGNED" -> Triple("Ruta al restaurante", onOpenPickupMap, false)
        "ON_DELIVERY" -> Triple("Ver mapa", onOpenMap, false)
        else -> return
    }
    val actionEnabled = when (order.status) {
        "READY_FOR_PICKUP" -> assignEnabled && !isAssigning
        else -> !isAssigning
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DobbyGoColors.Surface)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
    ) {
        if (order.status == "READY_FOR_PICKUP" && !assignEnabled && !assignBlockedMessage.isNullOrBlank()) {
            Text(
                text = assignBlockedMessage,
                style = MaterialTheme.typography.bodySmall,
                color = DobbyGoColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Button(
            onClick = onClick,
            enabled = actionEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DobbyGoColors.Purple,
                disabledContainerColor = DobbyGoColors.Purple.copy(alpha = 0.5f),
            ),
        ) {
            if (isAssigning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                if (showCheckIcon) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = DobbyGoColors.Purple,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
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
