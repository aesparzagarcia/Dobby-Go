package com.ares.ewe_man.presentation.viewmodel.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.core.network.toUserFacingMessage
import com.ares.ewe_man.data.remote.model.DeliveryOrderDto
import com.ares.ewe_man.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OrdersTab {
    OPEN,      // READY_FOR_PICKUP - available to claim
    ASSIGNED,  // ASSIGNED + ON_DELIVERY - my active orders
    CLOSED     // DELIVERED - my completed orders
}

data class OrdersUiState(
    val selectedTab: OrdersTab = OrdersTab.OPEN,
    val orders: List<DeliveryOrderDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun setTab(tab: OrdersTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
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
