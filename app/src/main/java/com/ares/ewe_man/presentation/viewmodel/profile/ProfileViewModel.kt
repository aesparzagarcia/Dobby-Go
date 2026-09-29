package com.ares.ewe_man.presentation.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.core.network.toUserFacingMessage
import com.ares.ewe_man.data.remote.model.DeliveryProfileDto
import com.ares.ewe_man.data.remote.model.DeliveryShiftClaimDto
import com.ares.ewe_man.data.remote.model.DeliveryShiftsDto
import com.ares.ewe_man.domain.repository.DeliveryProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: DeliveryProfileDto? = null,
    val isLoading: Boolean = false,
    val statusUpdating: Boolean = false,
    val errorMessage: String? = null,
    val showShifts: Boolean = false,
    val shifts: DeliveryShiftsDto? = null,
    val shiftsLoading: Boolean = false,
    val shiftsSaving: Boolean = false,
    val editingNextWeek: Boolean = false,
    val draftClaims: Set<String> = emptySet(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val deliveryProfileRepository: DeliveryProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _connectionStatusUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val connectionStatusUpdated: SharedFlow<Unit> = _connectionStatusUpdated.asSharedFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                val status = _uiState.value.profile?.status
                if (status == "ONLINE" || status == "ON_DELIVERY") {
                    deliveryProfileRepository.heartbeat()
                }
                delay(45_000)
            }
        }
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
                    _uiState.value = _uiState.value.copy(
                        profile = dto,
                        isLoading = false,
                        errorMessage = null,
                    )
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
                    _connectionStatusUpdated.tryEmit(Unit)
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

    fun openShifts() {
        _uiState.value = _uiState.value.copy(showShifts = true, editingNextWeek = false)
        loadShifts()
    }

    fun closeShifts() {
        _uiState.value = _uiState.value.copy(showShifts = false)
    }

    fun selectWeek(next: Boolean) {
        val shifts = _uiState.value.shifts ?: return
        val week = if (next) shifts.next else shifts.current
        _uiState.value = _uiState.value.copy(
            editingNextWeek = next,
            draftClaims = week.claims.map { "${it.weekday}-${it.slot}" }.toSet(),
        )
    }

    fun toggleClaim(weekday: Int, slot: String) {
        val key = "$weekday-$slot"
        val next = _uiState.value.draftClaims.toMutableSet()
        if (!next.add(key)) next.remove(key)
        _uiState.value = _uiState.value.copy(draftClaims = next)
    }

    fun saveShifts() {
        val shifts = _uiState.value.shifts ?: return
        val weekStart = if (_uiState.value.editingNextWeek) shifts.nextWeekStart else shifts.weekStart
        val claims = _uiState.value.draftClaims.mapNotNull { raw ->
            val parts = raw.split("-")
            if (parts.size < 2) null else DeliveryShiftClaimDto(parts[0].toInt(), parts.drop(1).joinToString("-"))
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(shiftsSaving = true)
            deliveryProfileRepository.saveShifts(weekStart, claims).fold(
                onSuccess = { dto ->
                    val week = if (_uiState.value.editingNextWeek) dto.next else dto.current
                    _uiState.value = _uiState.value.copy(
                        shifts = dto,
                        shiftsSaving = false,
                        draftClaims = week.claims.map { "${it.weekday}-${it.slot}" }.toSet(),
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        shiftsSaving = false,
                        errorMessage = e.toUserFacingMessage(),
                    )
                },
            )
        }
    }

    private fun loadShifts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(shiftsLoading = true)
            deliveryProfileRepository.getShifts(null).fold(
                onSuccess = { dto ->
                    val week = if (_uiState.value.editingNextWeek) dto.next else dto.current
                    _uiState.value = _uiState.value.copy(
                        shifts = dto,
                        shiftsLoading = false,
                        draftClaims = week.claims.map { "${it.weekday}-${it.slot}" }.toSet(),
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        shiftsLoading = false,
                        errorMessage = e.toUserFacingMessage(),
                    )
                },
            )
        }
    }
}
