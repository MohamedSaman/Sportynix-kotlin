package com.sportynix.app.domain.model

data class Venue(
    val id: String,
    val name: String,
    val description: String,
    val sportType: String,
    val location: String,
    val address: String,
    val pricePerHour: Double,
    val rating: Float,
    val reviewCount: Int,
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val availableSlots: List<TimeSlot> = emptyList(),
    val amenities: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val distance: Double? = null
)

data class TimeSlot(
    val id: String,
    val startTime: String,
    val endTime: String,
    val price: Double,
    val isAvailable: Boolean = true
)
