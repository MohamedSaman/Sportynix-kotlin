package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VenueGalleryImageDto(
    @SerializedName("id") val id: Int,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("image_url_secure") val imageUrlSecure: String? = null,
    @SerializedName("caption") val caption: String? = null
)

data class RatingBreakdownDto(
    @SerializedName("cleanliness") val cleanliness: Float? = null,
    @SerializedName("facilities") val facilities: Float? = null,
    @SerializedName("value") val value: Float? = null,
    @SerializedName("staff") val staff: Float? = null,
    @SerializedName("star5") val star5: Int? = 0,
    @SerializedName("star4") val star4: Int? = 0,
    @SerializedName("star3") val star3: Int? = 0,
    @SerializedName("star2") val star2: Int? = 0,
    @SerializedName("star1") val star1: Int? = 0
)

data class VenueReviewPhotoDto(
    @SerializedName("id") val id: Int,
    @SerializedName("image") val image: String? = null,
    @SerializedName("image_url_secure") val imageUrlSecure: String? = null
)

data class VenueReviewDto(
    @SerializedName("id") val id: Int,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("user_avatar") val userAvatar: String?,
    @SerializedName("user_avatar_secure") val userAvatarSecure: String? = null,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String?,
    @SerializedName("recommends") val recommends: Boolean? = true,
    @SerializedName("would_recommend") val wouldRecommend: Boolean? = null,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("review_photos") val reviewPhotos: List<VenueReviewPhotoDto>? = emptyList(),
    @SerializedName("categories") val categories: List<String>? = emptyList()
)

data class VenueSportDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("price") val price: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("image_secure") val imageSecure: String?,
    @SerializedName(value = "average_rating", alternate = ["rating"]) val averageRating: Float?,
    @SerializedName(value = "reviews_count", alternate = ["reviews", "reviewsCount"]) val reviewsCount: Int?,
    @SerializedName("opening_hours") val openingHours: Map<String, OpeningHourEntryDto>?,
    @SerializedName("advance_payment_required") val advancePaymentRequired: Boolean? = null,
    @SerializedName("advance_payment_type") val advancePaymentType: String? = null,
    @SerializedName("advance_payment_value") val advancePaymentValue: String? = null,
    @SerializedName("advance_payment_required_override") val advancePaymentRequiredOverride: Boolean? = null,
    @SerializedName("advance_payment_type_override") val advancePaymentTypeOverride: String? = null,
    @SerializedName("advance_payment_value_override") val advancePaymentValueOverride: String? = null,
    @SerializedName("booking_payment_mode_override") val bookingPaymentModeOverride: String? = null,
    @SerializedName("payment_mode") val paymentMode: String? = null
) {
    val rating: Float? get() = averageRating
    val reviews: Int? get() = reviewsCount
}

data class PaymentBadgeDto(
    val label: String,
    val tone: String // warning, success, neutral
)

data class VenueDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName(value = "sport_type", alternate = ["sportType", "venue_category"]) val sportType: String?,
    @SerializedName(value = "city_name", alternate = ["location", "city"]) val location: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("county") val county: String?,
    @SerializedName("postal_code") val postalCode: String?,
    @SerializedName("contact_number") val contactNumber: String?,
    @SerializedName("email_address") val emailAddress: String?,
    @SerializedName("website") val website: String?,
    @SerializedName(value = "price_per_hour", alternate = ["pricePerHour", "price"]) val pricePerHour: Double?,
    @SerializedName("rating") val rating: Float?,
    @SerializedName(value = "review_count", alternate = ["reviewCount", "reviews"]) val reviewCount: Int?,
    @SerializedName(value = "image_url", alternate = ["imageUrls", "image"]) val imageUrl: String?,
    @SerializedName("image_url_secure") val imageUrlSecure: String?,
    @SerializedName("image_urls") val imageUrlsList: List<String>?,
    @SerializedName("gallery_images") val galleryImagesList: List<VenueGalleryImageDto>?,
    @SerializedName(value = "available_slots", alternate = ["availableSlots"]) val availableSlots: List<TimeSlotDto>?,
    @SerializedName("amenities") val amenities: List<String>?,
    @SerializedName("terms") val terms: String?,
    @SerializedName(value = "analytics_enabled", alternate = ["isFeatured"]) val isFeatured: Boolean?,
    @SerializedName("distance") val distance: Double?,
    @SerializedName(value = "distance_display", alternate = ["distanceText"]) val distanceDisplay: String?,
    @SerializedName("sports") val sports: List<VenueSportDto>?,
    @SerializedName("opening_hours") val openingHours: Map<String, OpeningHourEntryDto>?,
    @SerializedName("reviews_list") val reviewsList: List<VenueReviewDto>?,
    @SerializedName("rating_breakdown") val ratingBreakdown: RatingBreakdownDto?,

    // Payment capability policy flags matching backend
    @SerializedName("online_payments_enabled") val onlinePaymentsEnabled: Boolean? = true,
    @SerializedName("cash_payments_enabled") val cashPaymentsEnabled: Boolean? = true,
    @SerializedName("venue_card_payments_enabled") val venueCardPaymentsEnabled: Boolean? = true,
    @SerializedName("bank_transfer_payments_enabled") val bankTransferPaymentsEnabled: Boolean? = false,
    @SerializedName("points_redemption_enabled") val pointsRedemptionEnabled: Boolean? = true,
    @SerializedName("advance_payment_required") val advancePaymentRequired: Boolean? = null,
    @SerializedName("advance_payment_type") val advancePaymentType: String? = null,
    @SerializedName("advance_payment_value") val advancePaymentValue: String? = null,
    @SerializedName("booking_payment_mode") val bookingPaymentMode: String? = null
) {
    val formattedLocationLine: String get() {
        val parts = mutableListOf<String>()
        location?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        county?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        return parts.joinToString(", ")
    }

    val formattedDistanceText: String get() {
        if (!distanceDisplay.isNull_or_empty()) return distanceDisplay!!
        val d = distance ?: return "N/A"
        return if (d < 1.0) "${(d * 1000).toInt()}m away" else String.format("%.1f km away", d)
    }

    fun buildPaymentBadges(sport: VenueSportDto? = null): List<PaymentBadgeDto> {
        val badges = mutableListOf<PaymentBadgeDto>()

        val overrideValue = sport?.advancePaymentValueOverride?.toDoubleOrNull()
        val hasExplicitOverridePolicy = sport?.advancePaymentRequiredOverride == false ||
                !sport?.advancePaymentTypeOverride.isNull_or_empty() ||
                (overrideValue != null && overrideValue > 0.0)

        val advanceReq = if (hasExplicitOverridePolicy) {
            sport?.advancePaymentRequiredOverride
        } else {
            sport?.advancePaymentRequired ?: advancePaymentRequired ?: false
        } ?: false

        val advanceType = sport?.advancePaymentTypeOverride ?: sport?.advancePaymentType ?: advancePaymentType
        val advanceVal = sport?.advancePaymentValueOverride ?: sport?.advancePaymentValue ?: advancePaymentValue
        val paymentMode = sport?.paymentMode ?: sport?.bookingPaymentModeOverride ?: bookingPaymentMode ?: (if (advanceReq) "advance_or_full" else "no_payment")

        val advanceLabel = when {
            advanceType == "percentage" && !advanceVal.isNull_or_empty() -> "$advanceVal% advance"
            !advanceVal.isNull_or_empty() -> "Rs. $advanceVal advance"
            else -> "Advance required"
        }

        when (paymentMode) {
            "full_only" -> badges.add(PaymentBadgeDto("Full payment required", "warning"))
            "advance_or_full" -> badges.add(PaymentBadgeDto("$advanceLabel or full", "warning"))
            "advance_only" -> badges.add(PaymentBadgeDto(advanceLabel, "warning"))
        }

        if (pointsRedemptionEnabled == true) badges.add(PaymentBadgeDto("Points available", "success"))
        if (onlinePaymentsEnabled == true) badges.add(PaymentBadgeDto("Online pay", "success"))
        if (cashPaymentsEnabled == true) badges.add(PaymentBadgeDto("Cash", "neutral"))
        if (venueCardPaymentsEnabled == true) badges.add(PaymentBadgeDto("Card at venue", "neutral"))

        return badges.take(4)
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}

data class DiscoverVenueResponseDto(
    @SerializedName("next") val next: String?,
    @SerializedName("results") val results: List<VenueDto>?
)

data class FavoriteDto(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String,
    @SerializedName("venue_id") val venueId: Int?,
    @SerializedName("sport_id") val sportId: Int?,
    @SerializedName("venue") val venue: VenueDto?,
    @SerializedName("sport") val sport: VenueSportDto?
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
