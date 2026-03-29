package com.ares.ewe_man.presentation.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.domain.model.AuthResult
import com.ares.ewe_man.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtpUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    fun onCodeChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(code = digitsOnly, errorMessage = null)
    }

    fun verify(phone: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val code = _uiState.value.code.trim()
            if (code.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Ingresa el código recibido")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.verifyOtp(phone, code)) {
                is AuthResult.Success -> onSuccess()
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
