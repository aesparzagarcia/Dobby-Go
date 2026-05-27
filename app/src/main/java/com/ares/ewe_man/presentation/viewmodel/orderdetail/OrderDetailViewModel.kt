package com.ares.ewe_man.presentation.viewmodel.orderdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.core.network.toUserFacingMessage
import com.ares.ewe_man.data.remote.model.DeliveryOrderDto
import com.ares.ewe_man.domain.repository.DeliveryProfileRepository
import com.ares.ewe_man.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MSG_FINISH_CURRENT_ORDER =
    "Finaliza el pedido en curso antes de tomar otro."

data class OrderDetailUiState(
    val order: DeliveryOrderDto? = null,
    val isLoading: Boolean = false,
    val isAssigning: Boolean = false,
    /** No puede asignar: pedido ASSIGNED/ON_DELIVERY o estado del repartidor ON_DELIVERY. */
    val assignBlocked: Boolean = false,
    val assignBlockedMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val deliveryProfileRepository: DeliveryProfileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    private suspend fun refreshAssignAvailabilitySync() {
        val assigned = orderRepository.getOrdersByStatus("ASSIGNED").getOrNull().orEmpty()
        val inProgress = orderRepository.getOrdersByStatus("ON_DELIVERY").getOrNull().orEmpty()
        val hasOrderInProgress = assigned.isNotEmpty() || inProgress.isNotEmpty()

        val profile = deliveryProfileRepository.getProfile().getOrNull()
        val statusOnDelivery = profile?.status == "ON_DELIVERY"
        val blocked = hasOrderInProgress || statusOnDelivery || profile?.hasActiveOrder == true

        _uiState.value = _uiState.value.copy(
            assignBlocked = blocked,
            assignBlockedMessage = if (blocked) MSG_FINISH_CURRENT_ORDER else null,
        )
    }

    fun loadOrder() {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            refreshAssignAvailabilitySync()
            orderRepository.getOrderById(orderId)
                .onSuccess { order ->
                    _uiState.value = _uiState.value.copy(
                        order = order,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
        }
    }

    fun assignToMe(onSuccess: () -> Unit = {}) {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            refreshAssignAvailabilitySync()
            if (_uiState.value.assignBlocked) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = MSG_FINISH_CURRENT_ORDER,
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isAssigning = true, errorMessage = null)
            orderRepository.assignOrder(orderId)
                .onSuccess {
                    refreshAssignAvailabilitySync()
                    val refreshed = orderRepository.getOrderById(orderId).getOrNull()
                    _uiState.value = _uiState.value.copy(
                        order = refreshed ?: _uiState.value.order?.copy(status = "ASSIGNED"),
                        isAssigning = false,
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isAssigning = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
