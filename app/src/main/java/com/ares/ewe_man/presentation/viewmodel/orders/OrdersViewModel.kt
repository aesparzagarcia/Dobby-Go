package com.ares.ewe_man.presentation.viewmodel.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.core.network.toUserFacingMessage
import com.ares.ewe_man.data.remote.model.DeliveryOrderDto
import com.ares.ewe_man.domain.repository.DeliveryProfileRepository
import com.ares.ewe_man.domain.repository.OrderRepository
import com.ares.ewe_man.realtime.DeliveryOrderRealtimeBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OrdersTab {
    OPEN,      // READY_FOR_PICKUP - available to claim
    ASSIGNED,  // ASSIGNED + ON_DELIVERY - my active orders
    CLOSED     // DELIVERED - my completed orders
}

data class OrdersUiState(
    val deliveryManDisplayName: String? = null,
    val profilePhotoUrl: String? = null,
    val connectionStatus: String = "OFFLINE",
    val selectedTab: OrdersTab = OrdersTab.OPEN,
    val orders: List<DeliveryOrderDto> = emptyList(),
    /** True si ya tiene un pedido ASSIGNED u ON_DELIVERY — bloquea tomar otros abiertos. */
    val hasActiveOrder: Boolean = false,
    val activeOrderId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val deliveryProfileRepository: DeliveryProfileRepository,
    private val orderRealtimeBus: DeliveryOrderRealtimeBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        loadDeliveryManName()
        loadOrders()
        refreshActiveOrderLock()
        viewModelScope.launch {
            orderRealtimeBus.refreshOrders.collect {
                refresh()
                refreshActiveOrderLock()
            }
        }
    }

    fun refreshActiveOrderLock() {
        viewModelScope.launch { refreshActiveOrderLockSync() }
    }

    private suspend fun refreshActiveOrderLockSync() {
        val assigned = orderRepository.getOrdersByStatus("ASSIGNED").getOrNull().orEmpty()
        val inProgress = orderRepository.getOrdersByStatus("ON_DELIVERY").getOrNull().orEmpty()
        val active = (assigned + inProgress).sortedByDescending { it.createdAt }
        _uiState.update {
            it.copy(
                hasActiveOrder = active.isNotEmpty(),
                activeOrderId = active.firstOrNull()?.id,
            )
        }
    }

    private fun loadDeliveryManName() {
        viewModelScope.launch { refreshProfileHeaderSync() }
    }

    private suspend fun refreshProfileHeaderSync() {
        deliveryProfileRepository.getProfile()
            .onSuccess { profile ->
                val name = profile.name.trim().ifBlank { null }
                _uiState.update {
                    it.copy(
                        deliveryManDisplayName = name,
                        profilePhotoUrl = profile.profilePhotoUrl,
                        connectionStatus = profile.status,
                        hasActiveOrder = profile.hasActiveOrder,
                    )
                }
            }
    }

    fun refreshProfileHeader() {
        viewModelScope.launch { refreshProfileHeaderSync() }
    }

    fun setTab(tab: OrdersTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            refreshActiveOrderLockSync()
            loadOrdersForTab(_uiState.value.selectedTab)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        orders = list,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.toUserFacingMessage(),
                        isLoading = false,
                        isRefreshing = false
                    )
                }
        }
    }

    private suspend fun loadOrdersForTab(tab: OrdersTab): Result<List<DeliveryOrderDto>> {
        return when (tab) {
            OrdersTab.OPEN -> orderRepository.getOrdersByStatus("READY_FOR_PICKUP")
            OrdersTab.ASSIGNED -> {
                val assigned = orderRepository.getOrdersByStatus("ASSIGNED").getOrElse { return Result.failure(it) }
                val inProgress = orderRepository.getOrdersByStatus("ON_DELIVERY").getOrElse { return Result.failure(it) }
                val merged = (assigned + inProgress).sortedByDescending { it.createdAt }
                Result.success(merged)
            }
            OrdersTab.CLOSED -> orderRepository.getOrdersByStatus("DELIVERED")
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            refreshProfileHeaderSync()
            refreshActiveOrderLockSync()
            loadOrdersForTab(_uiState.value.selectedTab)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(orders = list, isRefreshing = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = e.toUserFacingMessage(),
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
