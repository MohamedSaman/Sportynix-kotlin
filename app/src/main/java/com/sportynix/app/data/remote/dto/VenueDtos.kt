package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VenueGalleryImageDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("image_url") val imageUrl: String?
)

data class VenueReviewDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("user_avatar") val userAvatar: String?,
    @SerializedName("rating") val rating: Float?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("comment") val comment: String?,
    @SerializedName("recommends") val recommends: Boolean?
)

data class RatingBreakdownDto(
    @SerializedName("5") val star5: Int? = 0,
    @SerializedName("4") val star4: Int? = 0,
    @SerializedName("3") val star3: Int? = 0,
    @SerializedName("2") val star2: Int? = 0,
    @SerializedName("1") val star1: Int? = 0
)

data class VenueSportDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("price") val price: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("rating") val rating: Float?,
    @SerializedName("reviews") val reviews: Int?
)

data class VenueDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName(value = "sport_type", alternate = ["sportType", "venue_category"]) val sportType: String?,
    @SerializedName(value = "city_name", alternate = ["location", "city"]) val location: String?,
    @SerializedName("address") val address: String?,
    @SerializedName(value = "price_per_hour", alternate = ["pricePerHour", "price"]) val pricePerHour: Double?,
    @SerializedName("rating") val rating: Float?,
    @SerializedName(value = "review_count", alternate = ["reviewCount", "reviews"]) val reviewCount: Int?,
    @SerializedName(value = "image_url", alternate = ["imageUrls", "image"]) val imageUrl: String?,
    @SerializedName("image_urls") val imageUrlsList: List<String>?,
    @SerializedName("gallery_images") val galleryImagesList: List<VenueGalleryImageDto>?,
    @SerializedName(value = "available_slots", alternate = ["availableSlots"]) val availableSlots: List<TimeSlotDto>?,
    @SerializedName("amenities") val amenities: List<String>?,
    @SerializedName(value = "analytics_enabled", alternate = ["isFeatured"]) val isFeatured: Boolean?,
    @SerializedName("distance") val distance: Double?,
    @SerializedName(value = "distance_display", alternate = ["distanceText"]) val distanceDisplay: String?,
    @SerializedName("sports") val sports: List<VenueSportDto>?,
    @SerializedName("reviews_list") val reviewsList: List<VenueReviewDto>?,
    @SerializedName("rating_breakdown") val ratingBreakdown: RatingBreakdownDto?
)

data class TimeSlotDto(
    @SerializedName("id") val id: String,
    @SerializedName(value = "start_time", alternate = ["startTime"]) val startTime: String,
    @SerializedName(value = "end_time", alternate = ["endTime"]) val endTime: String,
    @SerializedName("price") val price: Double?,
    @SerializedName(value = "is_available", alternate = ["isAvailable"]) val isAvailable: Boolean?
)

data class SlotHoldResponseDto(
    @SerializedName("status") val status: String?,
    @SerializedName("hold_token") val holdToken: String?
)

data class ReviewRequestDto(
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String,
    @SerializedName("recommends") val recommends: Boolean = true
)
