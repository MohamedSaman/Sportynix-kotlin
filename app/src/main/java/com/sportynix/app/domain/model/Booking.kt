package com.sportynix.app.domain.model

data class Booking(
    val id: String,
    val venueId: String,
    val venueName: String,
    val venueImageUrl: String?,
    val slotTime: String,
    val bookingDate: String,
    val totalPrice: Double,
    val status: BookingStatus,
    val createdAt: String
)

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
