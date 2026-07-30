package com.sportynix.app.domain.model

data class VenueSport(
    val id: Int = 0,
    val name: String = "",
    val price: String = "",
    val imageUrl: String = "",
    val rating: Float = 0.0f,
    val reviewsCount: Int = 0
)

data class VenueReview(
    val id: Int = 0,
    val userName: String = "User",
    val userAvatar: String? = null,
    val rating: Float = 5.0f,
    val createdAt: String = "",
    val comment: String = "",
    val recommends: Boolean = true
)

data class RatingBreakdown(
    val star5: Int = 0,
    val star4: Int = 0,
    val star3: Int = 0,
    val star2: Int = 0,
    val star1: Int = 0
)

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
    val galleryImages: List<String> = emptyList(),
    val availableSlots: List<TimeSlot> = emptyList(),
    val amenities: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val distance: Double? = null,
    val distanceDisplay: String? = null,
    val sports: List<VenueSport> = emptyList(),
    val reviewsList: List<VenueReview> = emptyList(),
    val ratingBreakdown: RatingBreakdown = RatingBreakdown()
)

data class TimeSlot(
    val id: String,
    val startTime: String,
    val endTime: String,
    val price: Double,
    val isAvailable: Boolean = true
)
