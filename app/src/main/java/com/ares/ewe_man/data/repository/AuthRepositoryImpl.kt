package com.ares.ewe_man.data.repository

import com.ares.ewe_man.data.local.datastore.SessionManager
import com.ares.ewe_man.data.session.SessionEventBus
import com.ares.ewe_man.data.remote.DeliveryLaunchRefreshOutcome
import com.ares.ewe_man.data.remote.DeliveryTokenRefreshService
import com.ares.ewe_man.data.remote.api.DobbyGoApi
import com.ares.ewe_man.data.remote.model.DeliveryRequestOtpRequest
import com.ares.ewe_man.data.remote.model.ErrorResponse
import com.ares.ewe_man.data.remote.model.VerifyOtpRequest
import com.ares.ewe_man.domain.model.AuthResult
import com.ares.ewe_man.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: DobbyGoApi,
    private val sessionManager: SessionManager,
    private val deliveryTokenRefreshService: DeliveryTokenRefreshService,
    private val sessionEventBus: SessionEventBus,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    override suspend fun requestOtp(phone: String): AuthResult<Unit> {
        return try {
            val normalized = phone.trim()
            if (normalized.isBlank()) {
                return AuthResult.Error("Ingresa tu número de teléfono")
            }
            api.requestOtp(DeliveryRequestOtpRequest(phone = normalized))
            AuthResult.Success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al enviar el código"
            AuthResult.Error(message)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Error al enviar el código")
        }
    }

    override suspend fun verifyOtp(phone: String, code: String): AuthResult<Unit> {
        return try {
            val phoneNorm = phone.trim()
            val codeStr = code.trim()
            if (phoneNorm.isBlank() || codeStr.isBlank()) {
                return AuthResult.Error("Teléfono y código son requeridos")
            }
            val response = api.verifyOtp(VerifyOtpRequest(phone = phoneNorm, code = codeStr))
            val token = response.token
            val refresh = response.refreshToken
            if (token.isNullOrBlank() || refresh.isNullOrBlank()) {
                return AuthResult.Error("Respuesta inválida. Asegúrate de usar el teléfono registrado en el panel web.")
            }
            response.deliveryMan?.id?.let { deliveryManId ->
                sessionManager.saveSession(token, refresh, deliveryManId)
            } ?: sessionManager.saveSession(token, refresh, null)
            sessionEventBus.resetExpiredGate()
            AuthResult.Success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Código inválido o expirado"
            AuthResult.Error(message)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Error al verificar el código")
        }
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }

    override suspend fun syncSessionAtLaunch(): Boolean {
        if (!sessionManager.isLoggedIn.first()) return false
        return when (deliveryTokenRefreshService.refreshStoredSession(sessionManager)) {
            DeliveryLaunchRefreshOutcome.SessionDead -> false
            DeliveryLaunchRefreshOutcome.Skipped,
            DeliveryLaunchRefreshOutcome.Refreshed,
            DeliveryLaunchRefreshOutcome.Unchanged -> true
        }
    }

    private fun parseErrorBody(e: HttpException): String? {
        return try {
            e.response()?.errorBody()?.string()?.let { body ->
                com.google.gson.Gson().fromJson(body, ErrorResponse::class.java)?.error
            }
        } catch (_: Exception) {
            null
        }
    }
}
