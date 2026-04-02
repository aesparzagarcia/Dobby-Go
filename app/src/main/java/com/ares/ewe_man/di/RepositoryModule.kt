package com.ares.ewe_man.di

import com.ares.ewe_man.data.repository.AuthRepositoryImpl
import com.ares.ewe_man.data.repository.DeliveryProfileRepositoryImpl
import com.ares.ewe_man.data.repository.DirectionsRepositoryImpl
import com.ares.ewe_man.data.repository.OrderRepositoryImpl
import com.ares.ewe_man.domain.repository.AuthRepository
import com.ares.ewe_man.domain.repository.DeliveryProfileRepository
import com.ares.ewe_man.domain.repository.DirectionsRepository
import com.ares.ewe_man.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryProfileRepository(impl: DeliveryProfileRepositoryImpl): DeliveryProfileRepository

    @Binds
    @Singleton
    abstract fun bindDirectionsRepository(impl: DirectionsRepositoryImpl): DirectionsRepository
}
