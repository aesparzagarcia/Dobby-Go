package com.ares.ewe_man.presentation.viewmodel.orderdetail

import androidx.lifecycle.SavedStateHandle
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

data class OrderDetailUiState(
    val order: DeliveryOrderDto? = null,
    val isLoading: Boolean = false,
    val isAssigning: Boolean = false,
    val isStartingDelivery: Boolean = false,
    val errorMessage: String? = null,
    val startDeliverySuccess: Boolean = false
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
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

    fun assignToMe() {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAssigning = true, errorMessage = null)
            orderRepository.assignOrder(orderId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        order = _uiState.value.order?.copy(status = "ASSIGNED"),
                        isAssigning = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isAssigning = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
        }
    }

    fun startDelivery(onSuccess: () -> Unit) {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStartingDelivery = true, errorMessage = null)
            orderRepository.startDelivery(orderId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        order = _uiState.value.order?.copy(status = "ON_DELIVERY"),
                        isStartingDelivery = false,
                        startDeliverySuccess = true
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isStartingDelivery = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
