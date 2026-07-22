package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VenueDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName(value = "sport_type", alternate = ["sportType", "venue_category"]) val sportType: String?,
    @SerializedName(value = "city_name", alternate = ["location", "city"]) val location: String?,
    @SerializedName("address") val address: String?,
    @SerializedName(value = "price_per_hour", alternate = ["pricePerHour", "price"]) val pricePerHour: Double?,
    @SerializedName("rating") val rating: Float?,
    @SerializedName(value = "review_count", alternate = ["reviewCount"]) val reviewCount: Int?,
    @SerializedName(value = "image_url", alternate = ["imageUrls", "image"]) val imageUrl: String?,
    @SerializedName("image_urls") val imageUrlsList: List<String>?,
    @SerializedName(value = "available_slots", alternate = ["availableSlots"]) val availableSlots: List<TimeSlotDto>?,
    @SerializedName("amenities") val amenities: List<String>?,
    @SerializedName(value = "analytics_enabled", alternate = ["isFeatured"]) val isFeatured: Boolean?
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
