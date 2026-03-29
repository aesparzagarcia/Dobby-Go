package com.ares.ewe_man.presentation.viewmodel.splash

import androidx.lifecycle.ViewModel
import com.ares.ewe_man.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    suspend fun shouldOpenHomeAfterSplash(): Boolean {
        if (!authRepository.isLoggedIn.first()) return false
        return authRepository.syncSessionAtLaunch()
    }
}
