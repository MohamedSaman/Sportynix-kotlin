package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BookingDto(
    @SerializedName("id") val id: String,
    @SerializedName("venue_id") val venueId: String? = null,
    @SerializedName("venue_name") val venueName: String? = null,
    @SerializedName("venue_image_url") val venueImageUrl: String? = null,
    @SerializedName("sport_name") val sportName: String? = null,
    @SerializedName("slot_time") val slotTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    @SerializedName("booking_date") val bookingDate: String? = null,
    @SerializedName("total_price") val totalPrice: Double? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("financial_status") val financialStatus: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("payment_amount") val paymentAmount: Double? = null,
    @SerializedName("qr_code_url") val qrCodeUrl: String? = null,
    @SerializedName("booking_reference") val bookingReference: String? = null,
    @SerializedName("team") val team: BookingTeamDto? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class BookingTeamDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("members_count") val membersCount: Int = 0
)

data class BookingQuoteRequestDto(
    @SerializedName("game_id") val gameId: Long = 1L,
    @SerializedName("complex_id") val complexId: Long = 1L,
    @SerializedName("booking_date") val bookingDate: String = "",
    @SerializedName("slots") val slots: List<SlotDto> = emptyList(),
    @SerializedName("user_name") val userName: String? = null,
    @SerializedName("user_email") val userEmail: String? = null,
    @SerializedName("user_number") val userNumber: String? = null
)

data class SlotDto(
    @SerializedName("start_time") val startTime: String = "",
    @SerializedName("end_time") val endTime: String = "",
    @SerializedName("duration") val duration: Int = 60,
    @SerializedName("price") val price: Double = 0.0
)

data class BookingQuoteResponseDto(
    @SerializedName("booking_total") val bookingTotal: String? = "0.00",
    @SerializedName("advance_required") val advanceRequired: Boolean? = false,
    @SerializedName("advance_amount") val advanceAmount: String? = "0.00",
    @SerializedName("gateway_amount") val gatewayAmount: String? = "0.00",
    @SerializedName("remaining_balance") val remainingBalance: String? = "0.00",
    @SerializedName("points_discount") val pointsDiscount: String? = "0.00",
    @SerializedName("accepted_points") val acceptedPoints: Int? = 0,
    @SerializedName("payment_option") val paymentOption: String? = "advance"
)

data class InitiatePaymentResponseDto(
    @SerializedName("order_id") val orderId: String? = null,
    @SerializedName("checkout_url") val checkoutUrl: String? = null,
    @SerializedName("amount") val amount: Double? = 0.0
)

data class SavedCardDto(
    @SerializedName("id") val id: Long,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("masked_number") val maskedNumber: String? = null,
    @SerializedName("last4") val last4: String? = null,
    @SerializedName("expiry_month") val expiryMonth: Int? = null,
    @SerializedName("expiry_year") val expiryYear: Int? = null,
    @SerializedName("is_default") val isDefault: Boolean = false
)

data class QrCodeResponseDto(
    @SerializedName("qr_code") val qrCode: String? = null
)

data class QuoteSlotDto(
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("duration") val duration: Int = 60,
    @SerializedName("price") val price: Double = 0.0
)

data class QuoteRequestDto(
    @SerializedName("venue_id") val venueId: String,
    @SerializedName("sport_id") val sportId: String,
    @SerializedName("booking_type") val bookingType: String = "Normal",
    @SerializedName("date") val date: String,
    @SerializedName("selected_days") val selectedDays: List<String> = emptyList(),
    @SerializedName("slots") val slots: List<QuoteSlotDto>,
    @SerializedName("payment_option") val paymentOption: String = "advance",
    @SerializedName("points_redeemed") val pointsRedeemed: Int = 0
)

data class QuoteResponseDto(
    @SerializedName("booking_total") val bookingTotal: String?,
    @SerializedName("payment_required") val paymentRequired: Boolean?,
    @SerializedName("advance_required") val advanceRequired: Boolean?,
    @SerializedName("advance_amount") val advanceAmount: String?,
    @SerializedName("gateway_amount") val gatewayAmount: String?,
    @SerializedName("remaining_balance") val remainingBalance: String?,
    @SerializedName("points_discount") val pointsDiscount: String?,
    @SerializedName("accepted_points") val acceptedPoints: Int?,
    @SerializedName("payment_option") val paymentOption: String?,
    @SerializedName("payment_mode") val paymentMode: String?,
    @SerializedName("allowed_payment_options") val allowedPaymentOptions: List<String>?
)

data class CreateBookingSlotDto(
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("duration") val duration: Int = 60,
    @SerializedName("price") val price: Double = 0.0
)

data class CreateBookingRequestDto(
    @SerializedName("venue_id") val venueId: String,
    @SerializedName("sport_id") val sportId: String,
    @SerializedName("booking_type") val bookingType: String = "Normal",
    @SerializedName("date") val date: String,
    @SerializedName("selected_days") val selectedDays: List<String> = emptyList(),
    @SerializedName("slots") val slots: List<CreateBookingSlotDto>,
    @SerializedName("payment_option") val paymentOption: String = "advance",
    @SerializedName("points_redeemed") val pointsRedeemed: Int = 0,
    @SerializedName("user_name") val userName: String? = null,
    @SerializedName("user_email") val userEmail: String? = null,
    @SerializedName("user_number") val userNumber: String? = null
)

data class PaymentCheckoutRequestDto(
    @SerializedName("venue_id") val venueId: String,
    @SerializedName("sport_id") val sportId: String,
    @SerializedName("booking_type") val bookingType: String = "Normal",
    @SerializedName("date") val date: String,
    @SerializedName("selected_days") val selectedDays: List<String> = emptyList(),
    @SerializedName("slots") val slots: List<CreateBookingSlotDto>,
    @SerializedName("payment_option") val paymentOption: String = "advance",
    @SerializedName("points_redeemed") val pointsRedeemed: Int = 0,
    @SerializedName("user_name") val userName: String? = null,
    @SerializedName("user_email") val userEmail: String? = null,
    @SerializedName("user_number") val userNumber: String? = null
)

data class PaymentCheckoutResponseDto(
    @SerializedName("checkout") val checkout: PaymentCheckoutUrlDto?,
    @SerializedName("payment") val payment: PaymentOrderInfoDto?,
    @SerializedName("bookings") val bookings: List<ConfirmedBookingDto>?,
    @SerializedName("reservation_expires_at") val reservationExpiresAt: String?
)

data class PaymentCheckoutUrlDto(
    @SerializedName("url") val url: String?
)

data class PaymentOrderInfoDto(
    @SerializedName("order_id") val orderId: String?,
    @SerializedName("amount") val amount: Double?,
    @SerializedName("currency") val currency: String? = "LKR",
    @SerializedName("purpose") val purpose: String?
)

data class ConfirmedBookingDto(
    @SerializedName("id") val id: String,
    @SerializedName("qr_code") val qrCode: String?,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?,
    @SerializedName("price") val price: Double?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("booking_date") val bookingDate: String?,
    @SerializedName("booking_reference") val bookingReference: String?,
    @SerializedName("payment_status") val paymentStatus: String?,
    @SerializedName("payment_amount") val paymentAmount: Double?,
    @SerializedName("payment_currency") val paymentCurrency: String? = "LKR",
    @SerializedName("receipt_number") val receiptNumber: String?,
    @SerializedName("receipt_download_url") val receiptDownloadUrl: String?
)

data class PaymentStatusResponseDto(
    @SerializedName("booking_id") val bookingId: String?,
    @SerializedName("payment_status") val paymentStatus: String?,
    @SerializedName("gateway_state") val gatewayState: String?,
    @SerializedName("booking_status") val bookingStatus: String?,
    @SerializedName("amount") val amount: Double?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("financial_status") val financialStatus: String?,
    @SerializedName("confirmation_bookings") val confirmationBookings: List<ConfirmedBookingDto>?
)

data class CancelBookingRequestDto(
    @SerializedName("reason") val reason: String = "Cancelled by user"
)

data class AssignTeamRequestDto(
    @SerializedName("team_id") val teamId: Int
)
