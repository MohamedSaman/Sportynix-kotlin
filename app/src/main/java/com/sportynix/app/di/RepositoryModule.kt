package com.sportynix.app.di

import com.sportynix.app.data.repository.AuthRepositoryImpl
import com.sportynix.app.data.repository.BookingRepositoryImpl
import com.sportynix.app.data.repository.PaymentRepositoryImpl
import com.sportynix.app.data.repository.VenueRepositoryImpl
import com.sportynix.app.domain.repository.AuthRepository
import com.sportynix.app.domain.repository.BookingRepository
import com.sportynix.app.domain.repository.PaymentRepository
import com.sportynix.app.domain.repository.VenueRepository
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
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindVenueRepository(
        impl: VenueRepositoryImpl
    ): VenueRepository

    @Binds
    @Singleton
    abstract fun bindBookingRepository(
        impl: BookingRepositoryImpl
    ): BookingRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        impl: PaymentRepositoryImpl
    ): PaymentRepository
}
