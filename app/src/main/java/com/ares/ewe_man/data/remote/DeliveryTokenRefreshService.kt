package com.ares.ewe_man.data.remote

import com.ares.ewe_man.BuildConfig
import com.ares.ewe_man.data.local.datastore.SessionManager
import com.ares.ewe_man.data.remote.model.DeliveryRefreshRequest
import com.ares.ewe_man.data.remote.model.DeliveryRefreshResponse
import com.ares.ewe_man.di.DobbyGoNoAuthClient
import com.google.gson.Gson
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

sealed interface DeliveryCoordinatorResult {
    data object NoRefreshStored : DeliveryCoordinatorResult
    data object SessionInvalid : DeliveryCoordinatorResult
    data object TransientFailure : DeliveryCoordinatorResult
    data class UseAccess(val token: String) : DeliveryCoordinatorResult
    data class NewTokens(val token: String) : DeliveryCoordinatorResult
}

sealed interface DeliveryLaunchRefreshOutcome {
    data object Skipped : DeliveryLaunchRefreshOutcome
    data object Refreshed : DeliveryLaunchRefreshOutcome
    data object Unchanged : DeliveryLaunchRefreshOutcome
    data object SessionDead : DeliveryLaunchRefreshOutcome
}

@Singleton
class DeliveryTokenRefreshService @Inject constructor(
    @DobbyGoNoAuthClient private val noAuthClient: OkHttpClient,
) {
    private val gson = Gson()
    private val mutex = Mutex()

    suspend fun coordinateAfter401(
        requestAccessToken: String,
        sessionManager: SessionManager,
    ): DeliveryCoordinatorResult = mutex.withLock {
        val currentAccess = sessionManager.authToken.first().orEmpty()
        if (currentAccess.isNotBlank() && currentAccess != requestAccessToken.trim()) {
            return@withLock DeliveryCoordinatorResult.UseAccess(currentAccess)
        }
        val refresh = sessionManager.refreshToken.first()
        if (refresh.isNullOrBlank()) {
            return@withLock DeliveryCoordinatorResult.NoRefreshStored
        }
        when (val r = withContext(Dispatchers.IO) { executeRefresh(refresh) }) {
            is HttpRefreshResult.Success -> {
                sessionManager.saveSession(r.accessToken, r.refreshToken)
                DeliveryCoordinatorResult.NewTokens(r.accessToken)
            }
            HttpRefreshResult.SessionInvalid -> DeliveryCoordinatorResult.SessionInvalid
            HttpRefreshResult.TransientFailure -> DeliveryCoordinatorResult.TransientFailure
        }
    }

    suspend fun refreshStoredSession(sessionManager: SessionManager): DeliveryLaunchRefreshOutcome =
        mutex.withLock {
            val refresh = sessionManager.refreshToken.first()
            if (refresh.isNullOrBlank()) {
                return@withLock DeliveryLaunchRefreshOutcome.Skipped
            }
            when (val r = withContext(Dispatchers.IO) { executeRefresh(refresh) }) {
                is HttpRefreshResult.Success -> {
                    sessionManager.saveSession(r.accessToken, r.refreshToken)
                    DeliveryLaunchRefreshOutcome.Refreshed
                }
                HttpRefreshResult.SessionInvalid -> {
                    sessionManager.clearSession()
                    DeliveryLaunchRefreshOutcome.SessionDead
                }
                HttpRefreshResult.TransientFailure -> DeliveryLaunchRefreshOutcome.Unchanged
            }
        }

    private fun executeRefresh(refresh: String): HttpRefreshResult {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/auth/delivery/refresh"
        val bodyJson = gson.toJson(DeliveryRefreshRequest(refresh))
        val httpReq = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()
        return try {
            noAuthClient.newCall(httpReq).execute().use { resp ->
                when (resp.code) {
                    401, 403 -> HttpRefreshResult.SessionInvalid
                    in 500..599 -> HttpRefreshResult.TransientFailure
                    else -> {
                        if (!resp.isSuccessful) {
                            if (resp.code == 400) HttpRefreshResult.SessionInvalid
                            else HttpRefreshResult.TransientFailure
                        } else {
                            val body = resp.body?.string().orEmpty()
                            val parsed = try {
                                gson.fromJson(body, DeliveryRefreshResponse::class.java)
                            } catch (_: Exception) {
                                null
                            }
                            val access = parsed?.token
                            val nextRefresh = parsed?.refreshToken
                            if (access.isNullOrBlank() || nextRefresh.isNullOrBlank()) {
                                HttpRefreshResult.TransientFailure
                            } else {
                                HttpRefreshResult.Success(access, nextRefresh)
                            }
                        }
                    }
                }
            }
        } catch (_: IOException) {
            HttpRefreshResult.TransientFailure
        }
    }

    private sealed interface HttpRefreshResult {
        data class Success(val accessToken: String, val refreshToken: String) : HttpRefreshResult
        data object SessionInvalid : HttpRefreshResult
        data object TransientFailure : HttpRefreshResult
    }
}
