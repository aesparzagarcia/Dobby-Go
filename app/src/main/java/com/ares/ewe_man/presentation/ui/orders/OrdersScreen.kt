package com.ares.ewe_man.presentation.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ares.ewe_man.data.remote.model.DeliveryOrderDto
import com.ares.ewe_man.presentation.viewmodel.orders.OrdersTab
import com.ares.ewe_man.presentation.viewmodel.orders.OrdersViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private fun tabTitle(tab: OrdersTab): String = when (tab) {
    OrdersTab.OPEN -> "Pedidos"
    OrdersTab.ASSIGNED -> "Mis asignados"
    OrdersTab.CLOSED -> "Cerrados"
}

private fun tabLabel(tab: OrdersTab): String = when (tab) {
    OrdersTab.OPEN -> "Abiertos"
    OrdersTab.ASSIGNED -> "Asignados"
    OrdersTab.CLOSED -> "Cerrados"
}

private fun emptyMessage(tab: OrdersTab): String = when (tab) {
    OrdersTab.OPEN -> "No hay pedidos abiertos para recoger"
    OrdersTab.ASSIGNED -> "No tienes pedidos asignados"
    OrdersTab.CLOSED -> "No tienes pedidos cerrados"
}

private fun formatOrderDate(createdAt: String): String {
    return try {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val date = iso.parse(createdAt) ?: return createdAt
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        createdAt
    }
}

private fun orderStatusLabel(status: String): String = when (status) {
    "READY_FOR_PICKUP" -> "Listo para recoger"
    "ASSIGNED" -> "Asignado"
    "ON_DELIVERY" -> "En camino"
    "DELIVERED" -> "Entregado"
    else -> status
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onOrderClick: (orderId: String) -> Unit,
    refreshTrigger: Int = 0,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) viewModel.refresh()
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text(tabTitle(uiState.selectedTab)) },
                    actions = {
                        IconButton(
                            onClick = { viewModel.refresh() },
                            enabled = !uiState.isRefreshing
                        ) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(8.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                            }
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OrdersTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            text = { Text(tabLabel(tab)) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoading && uiState.orders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.orders.isEmpty()) {
                item {
                    Text(
                        text = emptyMessage(uiState.selectedTab),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            } else {
                items(uiState.orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onClick = { onOrderClick(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: DeliveryOrderDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatOrderDate(order.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = order.shopName?.let { "$it · ${orderStatusLabel(order.status)}" }
                        ?: orderStatusLabel(order.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${String.format("%.2f", order.total)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            order.deliveryAddress?.let { addr ->
                if (addr.isNotBlank()) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = addr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
