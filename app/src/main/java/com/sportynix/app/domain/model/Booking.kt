package com.sportynix.app.domain.model

data class Booking(
    val id: Int,
    val complexName: String,
    val sport: String,
    val courtName: String,
    val teamName: String,
    val memberCount: Int,
    val teamId: Int?,
    val playDateStart: String,
    val playDateEnd: String,
    val timeSlot: String,
    val duration: String,
    val location: String,
    val price: String,
    val slotCount: Int,
    val bookingId: Int,
    val bookedDate: String,
    val status: String,
    val isPermanent: Boolean,
    val permanentSourceId: Int?,
    val imageURL: String,
    val qrCode: Boolean,
    val qrCodeURL: String? = null,
    val venueId: Int?,
    val sportId: Int?,
    val reviewId: Int?,
    val reviewRating: Double?,
    val isChallengeBooking: Boolean,
    val opponentTeamName: String?,
    val opponentMemberCount: Int?,
    val userId: Int?,
    val canCancel: Boolean,
    val createdAt: String?
) {
    // Backwards-compatibility helper properties
    val venueIdString: String get() = venueId?.toString() ?: ""
    val venueName: String get() = complexName
    val venueImageUrl: String? get() = imageURL.ifEmpty { null }
    val sportName: String get() = sport
    val slotTime: String get() = timeSlot
    val bookingDate: String get() = playDateStart
    val totalPrice: Double get() {
        val cleaned = price.replace("LKR", "").replace("Rs.", "").replace("Rs", "").replace(",", "").trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}

enum class BookingStatus(val value: String) {
    ONGOING("Ongoing"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    NO_SHOW("No-Show");

    companion object {
        fun fromString(statusStr: String?): BookingStatus {
            return when (statusStr?.lowercase()?.trim()) {
                "playing" -> ONGOING
                "confirmed", "upcoming", "pending" -> UPCOMING
                "completed" -> COMPLETED
                "no-show", "noshow" -> NO_SHOW
                else -> CANCELLED
            }
        }
    }
}


data class QuoteBreakdown(
    val bookingTotal: String,
    val advanceRequired: Boolean,
    val advanceAmount: String,
    val gatewayAmount: String,
    val remainingBalance: String,
    val pointsDiscount: String,
    val acceptedPoints: Int,
    val paymentOption: String
)

data class SavedCard(
    val id: Long,
    val brand: String,
    val maskedNumber: String,
    val last4: String,
    val expiryMonth: Int?,
    val expiryYear: Int?,
    val isDefault: Boolean
)
