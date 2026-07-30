package com.sportynix.app.domain.model

data class Booking(
    val id: String,
    val venueId: String,
    val venueName: String,
    val venueImageUrl: String?,
    val sportName: String = "Badminton",
    val slotTime: String,
    val endTime: String? = null,
    val bookingDate: String,
    val totalPrice: Double,
    val status: BookingStatus,
    val financialStatus: String? = null,
    val paymentStatus: String? = null,
    val paymentAmount: Double? = null,
    val qrCodeUrl: String? = null,
    val bookingReference: String? = null,
    val teamId: Long? = null,
    val teamName: String? = null,
    val teamMembersCount: Int = 0,
    val isPermanent: Boolean = false,
    val createdAt: String = ""
) {
    val venueImage: String? get() = venueImageUrl
    val date: String get() = bookingDate
    val time: String get() = slotTime
    val price: Double get() = totalPrice
}

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
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
