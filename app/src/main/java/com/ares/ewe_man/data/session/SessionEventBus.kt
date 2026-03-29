package com.ares.ewe_man.data.session

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionEventBus @Inject constructor() {
    private val _sessionExpired = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    private val expiredEventPending = AtomicBoolean(false)

    fun notifySessionExpired() {
        if (expiredEventPending.compareAndSet(false, true)) {
            _sessionExpired.tryEmit(Unit)
        }
    }

    fun resetExpiredGate() {
        expiredEventPending.set(false)
    }
}
