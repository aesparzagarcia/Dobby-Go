package com.ares.ewe_man.presentation.viewmodel.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.domain.model.AuthResult
import com.ares.ewe_man.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

private const val OTP_LENGTH = 6
private const val RESEND_COUNTDOWN_SECONDS = 600 // 10 minutes

data class OtpUiState(
    val digitSlots: List<String> = List(OTP_LENGTH) { "" },
    val remainingSeconds: Int = RESEND_COUNTDOWN_SECONDS,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val code: String get() = digitSlots.joinToString("")
}

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val phone: String = savedStateHandle.get<String>("phone").orEmpty()

    private val verifyMutex = Mutex()
    private var authCompletedSuccessfully = false

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _uiState.update { s ->
                    if (s.remainingSeconds > 0) {
                        s.copy(remainingSeconds = s.remainingSeconds - 1)
                    } else {
                        s
                    }
                }
            }
        }
    }

    /**
     * Updates one OTP slot from a single field. Returns the index that should receive focus next
     * (or previous on backspace), or null to leave focus where it is.
     */
    fun onSlotInput(index: Int, value: String): Int? {
        val filtered = value.filter { it.isDigit() }
        val slots = _uiState.value.digitSlots.toMutableList()

        return when {
            filtered.length >= OTP_LENGTH -> {
                filtered.take(OTP_LENGTH).forEachIndexed { i, c ->
                    slots[i] = "$c"
                }
                _uiState.update { it.copy(digitSlots = slots.toList(), errorMessage = null) }
                OTP_LENGTH - 1
            }
            filtered.length > 1 -> {
                val last = "${filtered.last()}"
                slots[index] = last
                _uiState.update { it.copy(digitSlots = slots.toList(), errorMessage = null) }
                (index + 1).coerceAtMost(OTP_LENGTH - 1)
            }
            filtered.length == 1 -> {
                slots[index] = filtered
                _uiState.update { it.copy(digitSlots = slots.toList(), errorMessage = null) }
                if (index < OTP_LENGTH - 1) index + 1 else null
            }
            else -> {
                if (slots[index].isNotEmpty()) {
                    slots[index] = ""
                    _uiState.update { it.copy(digitSlots = slots.toList(), errorMessage = null) }
                    null
                } else if (index > 0) {
                    val prev = index - 1
                    slots[prev] = ""
                    _uiState.update { it.copy(digitSlots = slots.toList(), errorMessage = null) }
                    prev
                } else {
                    _uiState.update { it.copy(errorMessage = null) }
                    null
                }
            }
        }
    }

    fun verifyCode(onVerified: () -> Unit) {
        viewModelScope.launch {
            verifyMutex.withLock {
                if (authCompletedSuccessfully) return@withLock
                val code = _uiState.value.code
                if (code.length < 4) {
                    _uiState.update { it.copy(errorMessage = "Introduce el código que recibiste") }
                    return@withLock
                }
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                when (val result = authRepository.verifyOtp(phone, code)) {
                    is AuthResult.Success -> {
                        authCompletedSuccessfully = true
                        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                        onVerified()
                    }
                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                    }
                }
            }
        }
    }
}
