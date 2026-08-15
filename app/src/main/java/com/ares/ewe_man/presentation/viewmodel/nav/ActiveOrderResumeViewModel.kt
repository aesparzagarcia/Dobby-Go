package com.ares.ewe_man.presentation.viewmodel.nav

import androidx.lifecycle.ViewModel
import com.ares.ewe_man.domain.repository.OrderRepository
import com.ares.ewe_man.presentation.ui.navigation.ActiveOrderResume
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ActiveOrderResumeViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    suspend fun resolve(): ActiveOrderResume? = ActiveOrderResume.resolve(orderRepository)
}
