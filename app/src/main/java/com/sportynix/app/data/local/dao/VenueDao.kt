package com.sportynix.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sportynix.app.data.local.entity.VenueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VenueDao {
    @Query("SELECT * FROM venues")
    fun getAllVenues(): Flow<List<VenueEntity>>

    @Query("SELECT * FROM venues WHERE id = :id")
    suspend fun getVenueById(id: String): VenueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVenues(venues: List<VenueEntity>)

    @Query("DELETE FROM venues")
    suspend fun clearAll()
}
