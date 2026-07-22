package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateBookingRequestDto(
    @SerializedName(value = "venue_id", alternate = ["venueId"]) val venueId: String,
    @SerializedName(value = "slot_id", alternate = ["slotId"]) val slotId: String,
    @SerializedName(value = "booking_date", alternate = ["bookingDate"]) val bookingDate: String
)

data class BookingDto(
    @SerializedName("id") val id: String,
    @SerializedName(value = "venue_id", alternate = ["venueId"]) val venueId: String,
    @SerializedName(value = "venue_name", alternate = ["venueName"]) val venueName: String?,
    @SerializedName(value = "venue_image_url", alternate = ["venueImageUrl", "image_url"]) val venueImageUrl: String?,
    @SerializedName(value = "slot_time", alternate = ["slotTime"]) val slotTime: String?,
    @SerializedName(value = "booking_date", alternate = ["bookingDate"]) val bookingDate: String?,
    @SerializedName(value = "total_price", alternate = ["totalPrice", "amount"]) val totalPrice: Double?,
    @SerializedName("status") val status: String?,
    @SerializedName(value = "created_at", alternate = ["createdAt"]) val createdAt: String?
)

data class QrCodeResponseDto(
    @SerializedName("qr_code") val qrCodeUrl: String?,
    @SerializedName("booking_code") val bookingCode: String?
)
