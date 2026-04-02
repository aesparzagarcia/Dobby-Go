package com.ares.ewe_man.presentation.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.core.network.toUserFacingMessage
import com.ares.ewe_man.data.remote.model.DeliveryProfileDto
import com.ares.ewe_man.domain.repository.DeliveryProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: DeliveryProfileDto? = null,
    val isLoading: Boolean = false,
    val statusUpdating: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val deliveryProfileRepository: DeliveryProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val hadProfile = _uiState.value.profile != null
            _uiState.value = _uiState.value.copy(
                isLoading = !hadProfile,
                errorMessage = null,
            )
            deliveryProfileRepository.getProfile().fold(
                onSuccess = { dto ->
                    _uiState.value = ProfileUiState(profile = dto, isLoading = false, errorMessage = null)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.toUserFacingMessage(),
                    )
                },
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /** Conectado = ONLINE; desconectado = OFFLINE. No aplicar si el servidor marcó ON_DELIVERY. */
    fun setConnectionStatus(wantOnline: Boolean) {
        val target = if (wantOnline) "ONLINE" else "OFFLINE"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusUpdating = true, errorMessage = null)
            deliveryProfileRepository.setConnectionStatus(target).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(statusUpdating = false)
                    loadProfile()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        statusUpdating = false,
                        errorMessage = e.toUserFacingMessage(),
                    )
                },
            )
        }
    }
}
