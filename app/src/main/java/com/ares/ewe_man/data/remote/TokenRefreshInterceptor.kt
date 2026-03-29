package com.ares.ewe_man.data.remote

import com.ares.ewe_man.data.local.datastore.SessionManager
import com.ares.ewe_man.data.session.SessionEventBus
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

private const val HEADER_AUTH_RETRY = "X-Dobby-Auth-Retry"

class TokenRefreshInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val deliveryTokenRefreshService: DeliveryTokenRefreshService,
    private val sessionEventBus: SessionEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 401) return response

        if (request.header(HEADER_AUTH_RETRY) != null) {
            return response
        }

        if (shouldSkipRefresh(request)) {
            return response
        }

        response.close()

        val requestAccess = request.header("Authorization").orEmpty()
            .removePrefix("Bearer ")
            .trim()

        val result = runBlocking {
            deliveryTokenRefreshService.coordinateAfter401(requestAccess, sessionManager)
        }

        when (result) {
            is DeliveryCoordinatorResult.NoRefreshStored -> {
                runBlocking { sessionManager.clearSession() }
                sessionEventBus.notifySessionExpired()
                return chain.proceed(
                    request.newBuilder()
                        .header(HEADER_AUTH_RETRY, "1")
                        .removeHeader("Authorization")
                        .build()
                )
            }
            is DeliveryCoordinatorResult.SessionInvalid -> {
                runBlocking { sessionManager.clearSession() }
                sessionEventBus.notifySessionExpired()
                return chain.proceed(
                    request.newBuilder()
                        .header(HEADER_AUTH_RETRY, "1")
                        .removeHeader("Authorization")
                        .build()
                )
            }
            is DeliveryCoordinatorResult.TransientFailure -> {
                throw IOException("No se pudo renovar la sesión. Comprueba tu conexión e inténtalo de nuevo.")
            }
            is DeliveryCoordinatorResult.UseAccess,
            is DeliveryCoordinatorResult.NewTokens -> {
                val access = when (result) {
                    is DeliveryCoordinatorResult.UseAccess -> result.token
                    is DeliveryCoordinatorResult.NewTokens -> result.token
                    else -> error("unreachable")
                }
                val retry = request.newBuilder()
                    .header("Authorization", "Bearer $access")
                    .header(HEADER_AUTH_RETRY, "1")
                    .build()
                val retryResp = chain.proceed(retry)
                if (retryResp.code == 401) {
                    retryResp.close()
                    runBlocking { sessionManager.clearSession() }
                    sessionEventBus.notifySessionExpired()
                    return chain.proceed(
                        request.newBuilder()
                            .header(HEADER_AUTH_RETRY, "1")
                            .removeHeader("Authorization")
                            .build()
                    )
                }
                return retryResp
            }
        }
    }

    private fun shouldSkipRefresh(request: Request): Boolean {
        val u = request.url.toString()
        return u.contains("auth/delivery/request-otp") ||
            u.contains("auth/delivery/verify-otp") ||
            u.contains("auth/delivery/refresh")
    }
}
