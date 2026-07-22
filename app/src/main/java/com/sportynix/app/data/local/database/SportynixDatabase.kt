package com.sportynix.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sportynix.app.data.local.dao.BookingDao
import com.sportynix.app.data.local.dao.VenueDao
import com.sportynix.app.data.local.entity.BookingEntity
import com.sportynix.app.data.local.entity.VenueEntity

@Database(
    entities = [VenueEntity::class, BookingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SportynixDatabase : RoomDatabase() {
    abstract fun venueDao(): VenueDao
    abstract fun bookingDao(): BookingDao
}
