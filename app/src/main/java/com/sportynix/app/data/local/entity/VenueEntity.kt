package com.sportynix.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "venues")
data class VenueEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val sportType: String,
    val location: String,
    val address: String,
    val pricePerHour: Double,
    val rating: Float,
    val reviewCount: Int,
    val imageUrl: String,
    val isFeatured: Boolean
)
