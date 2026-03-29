package com.ares.ewe_man.presentation.viewmodel.nav

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Shared ViewModel used to trigger orders list refresh when returning from order detail.
 * Scope should be the navigation host (e.g. Activity) so it survives navigation.
 */
@HiltViewModel
class OrdersRefreshViewModel @Inject constructor() : ViewModel() {

    private val _triggerCount = MutableStateFlow(0)
    val triggerCount: StateFlow<Int> = _triggerCount.asStateFlow()

    fun triggerRefresh() {
        _triggerCount.value = _triggerCount.value + 1
    }
}
