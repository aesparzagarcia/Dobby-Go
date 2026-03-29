package com.ares.ewe_man.di

import com.ares.ewe_man.data.session.SessionEventBus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SessionEventBusEntryPoint {
    fun sessionEventBus(): SessionEventBus
}
