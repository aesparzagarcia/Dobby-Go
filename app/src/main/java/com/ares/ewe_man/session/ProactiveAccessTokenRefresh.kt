package com.ares.ewe_man.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ares.ewe_man.core.auth.AccessTokenJwtParser
import com.ares.ewe_man.data.local.datastore.SessionManager
import com.ares.ewe_man.data.remote.DeliveryTokenRefreshService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * While the app is in the foreground, periodically checks JWT [exp] on the access token and
 * calls [DeliveryTokenRefreshService.refreshStoredSession] before it expires so API calls rarely
 * hit 401.
 */
@Singleton
class ProactiveAccessTokenRefresh @Inject constructor(
    private val sessionManager: SessionManager,
    private val deliveryTokenRefreshService: DeliveryTokenRefreshService,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundJob: Job? = null

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        foregroundJob?.cancel()
        foregroundJob = scope.launch {
            while (isActive) {
                runCatching { refreshIfExpiringSoon() }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        foregroundJob?.cancel()
        foregroundJob = null
    }

    private suspend fun refreshIfExpiringSoon() {
        if (!sessionManager.isLoggedIn.first()) return
        val access = sessionManager.authToken.first().orEmpty()
        if (access.isBlank()) return
        val exp = AccessTokenJwtParser.expiryEpochSeconds(access) ?: return
        val now = System.currentTimeMillis() / 1000
        val secondsLeft = exp - now
        if (secondsLeft > REFRESH_WHEN_SECONDS_LEFT) return
        if (sessionManager.refreshToken.first().isNullOrBlank()) return
        deliveryTokenRefreshService.refreshStoredSession(sessionManager)
    }

    private companion object {
        /** Access JWT is 15m; refresh before this many seconds remain to avoid 401 bursts. */
        const val REFRESH_WHEN_SECONDS_LEFT = 10 * 60L
        const val POLL_INTERVAL_MS = 3 * 60 * 1000L
    }
}
