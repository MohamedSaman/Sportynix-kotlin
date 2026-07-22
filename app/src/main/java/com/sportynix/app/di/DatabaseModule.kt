package com.sportynix.app.di

import android.content.Context
import androidx.room.Room
import com.sportynix.app.data.local.dao.BookingDao
import com.sportynix.app.data.local.dao.VenueDao
import com.sportynix.app.data.local.database.SportynixDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SportynixDatabase {
        return Room.databaseBuilder(
            context,
            SportynixDatabase::class.java,
            "sportynix_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideVenueDao(db: SportynixDatabase): VenueDao = db.venueDao()

    @Provides
    @Singleton
    fun provideBookingDao(db: SportynixDatabase): BookingDao = db.bookingDao()
}
