package com.ares.ewe_man.domain.repository

import com.ares.ewe_man.domain.model.AuthResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val isLoggedIn: Flow<Boolean>

    suspend fun requestOtp(phone: String): AuthResult<Unit>

    suspend fun verifyOtp(phone: String, code: String): AuthResult<Unit>

    suspend fun logout()

    suspend fun syncSessionAtLaunch(): Boolean
}
