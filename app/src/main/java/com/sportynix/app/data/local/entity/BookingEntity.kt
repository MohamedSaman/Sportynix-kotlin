package com.sportynix.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val venueId: String,
    val venueName: String,
    val venueImageUrl: String?,
    val slotTime: String,
    val bookingDate: String,
    val totalPrice: Double,
    val status: String,
    val createdAt: String
)
