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

data class SlotDto(
    @SerializedName("start_time") val startTime: String = "",
    @SerializedName("end_time") val endTime: String = "",
    @SerializedName("duration") val duration: Int = 60,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("original_price") val originalPrice: Double? = null,
    @SerializedName("discount_amount") val discountAmount: Double? = null,
    @SerializedName("unit_price") val unitPrice: Double? = null,
    @SerializedName("rate_type") val rateType: String? = null
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
    @SerializedName(value = "venue_id", alternate = ["complex_id"]) val venueId: String,
    @SerializedName(value = "sport_id", alternate = ["game_id"]) val sportId: String,
    @SerializedName("booking_type") val bookingType: String = "Normal",
    @SerializedName(value = "date", alternate = ["booking_date"]) val date: String,
    @SerializedName("selected_days") val selectedDays: List<String> = emptyList(),
    @SerializedName("slots") val slots: List<QuoteSlotDto>,
    @SerializedName("payment_option") val paymentOption: String = "advance",
    @SerializedName("points_redeemed") val pointsRedeemed: Int = 0
)

data class QuoteResponseDto(
    @SerializedName("booking_subtotal") val bookingSubtotal: String? = null,
    @SerializedName("discount_amount") val discountAmount: String? = null,
    @SerializedName("booking_total") val bookingTotal: String? = null,
    @SerializedName("payment_required") val paymentRequired: Boolean? = null,
    @SerializedName("advance_required") val advanceRequired: Boolean? = null,
    @SerializedName("advance_amount") val advanceAmount: String? = null,
    @SerializedName("gateway_amount") val gatewayAmount: String? = null,
    @SerializedName("remaining_balance") val remainingBalance: String? = null,
    @SerializedName("points_discount") val pointsDiscount: String? = null,
    @SerializedName("accepted_points") val acceptedPoints: Int? = null,
    @SerializedName("payment_option") val paymentOption: String? = null,
    @SerializedName("payment_mode") val paymentMode: String? = null,
    @SerializedName("allowed_payment_options") val allowedPaymentOptions: List<String>? = null,
    @SerializedName("priced_slots") val pricedSlots: List<SlotDto>? = null
)

typealias BookingQuoteResponseDto = QuoteResponseDto

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
    @SerializedName(value = "venue_id", alternate = ["complex_id"]) val venueId: String,
    @SerializedName(value = "sport_id", alternate = ["game_id"]) val sportId: String,
    @SerializedName("booking_type") val bookingType: String = "Normal",
    @SerializedName(value = "date", alternate = ["booking_date"]) val date: String,
    @SerializedName("selected_days") val selectedDays: List<String> = emptyList(),
    @SerializedName("slots") val slots: List<QuoteSlotDto>,
    @SerializedName("user_name") val userName: String? = null,
    @SerializedName("user_email") val userEmail: String? = null,
    @SerializedName("user_number") val userNumber: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = "card",
    @SerializedName("payment_option") val paymentOption: String = "advance",
    @SerializedName("card_payment_mode") val cardPaymentMode: String? = null,
    @SerializedName("saved_card_id") val savedCardId: Long? = null,
    @SerializedName("save_card") val saveCard: Boolean = false,
    @SerializedName("points_to_redeem") val pointsToRedeem: Int = 0
)

data class PaymentCheckoutUrlDto(
    @SerializedName("url") val url: String?,
    @SerializedName("flow") val flow: String? = null,
    @SerializedName("saved_card") val savedCard: SavedCardDto? = null,
    @SerializedName("requires_action") val requiresAction: Boolean? = false,
    @SerializedName("next_action_html") val nextActionHtml: String? = null
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
    @SerializedName("receipt_download_url") val receiptDownloadUrl: String?,
    @SerializedName("is_demo") val isDemo: Boolean = false,
    @SerializedName("team") val team: BookingTeamDto? = null
)

data class PaymentCheckoutResponseDto(
    @SerializedName("checkout") val checkout: PaymentCheckoutUrlDto?,
    @SerializedName("payment") val payment: PaymentOrderInfoDto?,
    @SerializedName("bookings") val bookings: List<ConfirmedBookingDto>?,
    @SerializedName("reservation_expires_at") val reservationExpiresAt: String?
)

data class PaymentStatusResponseDto(
    @SerializedName("booking_id") val bookingId: String?,
    @SerializedName("payment_status") val paymentStatus: String?,
    @SerializedName("gateway_state") val gatewayState: String?,
    @SerializedName("booking_status") val bookingStatus: String?,
    @SerializedName("amount") val amount: Double?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("financial_status") val financialStatus: String?,
    @SerializedName("confirmation_bookings") val confirmationBookings: List<ConfirmedBookingDto>?,
    @SerializedName("receipt_number") val receiptNumber: String?,
    @SerializedName("receipt_download_url") val receiptDownloadUrl: String?
)

data class CancelBookingRequestDto(
    @SerializedName("reason") val reason: String = "User cancelled from cancellation review screen."
)

data class AssignTeamRequestDto(
    @SerializedName("team_id") val teamId: Int
)

data class OpeningHourEntryDto(
    @SerializedName("open") val open: String? = null,
    @SerializedName("close") val close: String? = null,
    @SerializedName(value = "is_closed", alternate = ["closed"]) val isClosed: Boolean = false
) {
    val displayString: String get() {
        if (isClosed || open.isNullOrEmpty() || close.isNullOrEmpty()) return "Closed"
        return "$open - $close"
    }
}

data class SlotData(
    @SerializedName("start") val startTime: String? = null,
    @SerializedName("end") val endTime: String? = null,
    @SerializedName("raw_start") val rawStart: String? = null,
    @SerializedName("raw_end") val rawEnd: String? = null,
    @SerializedName("slot_key") val slotKey: String? = null,
    @SerializedName("available") val available: Boolean? = true,
    @SerializedName("is_past_time") val isPastTime: Boolean? = false,
    @SerializedName("is_fully_booked") val isFullyBooked: Boolean? = false,
    @SerializedName("is_held") val isHeld: Boolean? = false,
    @SerializedName("disabled_reason") val disabledReason: String? = null,
    @SerializedName("duration") val duration: Int? = 60,
    @SerializedName("held_by_current_user") val heldByCurrentUser: Boolean? = false,
    @SerializedName("is_payment_reserved") val isPaymentReserved: Boolean? = false,
    @SerializedName("available_courts") val availableCourts: Any? = null,
    @SerializedName("total_courts") val totalCourts: Any? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("unit_price") val unitPrice: Double? = null,
    @SerializedName("rate_type") val rateType: String? = null
)

data class AvailableSlotsResponse(
    @SerializedName("available_slots") val availableSlots: List<SlotData>? = null,
    @SerializedName("slots") val slots: List<SlotData>? = null
)

data class PermanentSlotAvailability(
    @SerializedName("available") val available: Boolean = false,
    @SerializedName("days_remaining") val daysRemaining: Int = 0,
    @SerializedName("booked_count") val bookedCount: Int = 0,
    @SerializedName("first_available_date") val firstAvailableDate: String? = null,
    @SerializedName("total_days_checked") val totalDaysChecked: Int = 0,
    @SerializedName("max_courts") val maxCourts: Int? = null,
    @SerializedName("total_occupied_by_others") val totalOccupiedByOthers: Int? = null
)

data class APIBookingTeamInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("members_count") val membersCount: Int? = 0
)

data class APIPermanentSource(
    @SerializedName("id") val id: Int
)

data class APIBookingReview(
    @SerializedName("id") val id: Int,
    @SerializedName("rating") val rating: Double? = null
)

data class RefundPolicyDto(
    @SerializedName("eligible") val eligible: Boolean? = false,
    @SerializedName("refund_amount") val refundAmount: Double? = 0.0,
    @SerializedName("deadline") val deadline: String? = null,
    @SerializedName("message") val message: String? = null
)

data class APIBooking(
    @SerializedName("id") val id: Int,
    @SerializedName("venue") val venue: String? = null,
    @SerializedName("venue_name") val venueName: String? = null,
    @SerializedName("complex_name") val complexName: String? = null,
    @SerializedName("sport") val sport: String? = null,
    @SerializedName("sport_name") val sportName: String? = null,
    @SerializedName("game_name") val gameName: String? = null,
    @SerializedName("court") val court: String? = null,
    @SerializedName("players") val players: String? = null,
    @SerializedName("team_info") val teamInfo: APIBookingTeamInfo? = null,
    @SerializedName("opponent_team_info") val opponentTeamInfo: APIBookingTeamInfo? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName(value = "booked_date", alternate = ["booking_date"]) val bookedDate: String? = null,
    @SerializedName("time") val time: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("price") val price: String? = null,
    @SerializedName("online_paid_amount") val onlinePaidAmount: Double? = null,
    @SerializedName("amount_paid") val amountPaid: Double? = null,
    @SerializedName("advance_amount") val advanceAmount: Double? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("is_permanent") val isPermanent: Boolean? = false,
    @SerializedName("permanent_source_id") val permanentSourceId: Int? = null,
    @SerializedName("permanent_source") val permanentSource: APIPermanentSource? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("qr_code") val qrCode: Any? = null,
    @SerializedName("venue_id") val venueId: Int? = null,
    @SerializedName("sport_id") val sportId: Int? = null,
    @SerializedName("review") val review: APIBookingReview? = null,
    @SerializedName("review_id") val reviewId: Int? = null,
    @SerializedName("review_rating") val reviewRating: Double? = null,
    @SerializedName("is_challenge_booking") val isChallengeBooking: Boolean? = false,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("can_cancel") val canCancel: Boolean? = false,
    @SerializedName("refund_policy") val refundPolicy: RefundPolicyDto? = null,
    @SerializedName("booked_at") val bookedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class BookingSlotInfo(
    val startTime: String,
    val endTime: String,
    val displayStart: String,
    val displayEnd: String,
    val duration: Int = 60,
    val price: Double
)

data class BookingPayload(
    val sportId: Int,
    val sportName: String,
    val sportPrice: String,
    val sportImageURL: String,
    val venueId: Int,
    val venueName: String,
    val venueAddress: String,
    val bookingType: String,
    val bookingDate: String,
    val selectedDays: List<String>,
    val slots: List<BookingSlotInfo>,
    val totalPrice: Double
)

data class BookingTeamData(
    val id: Int,
    val name: String? = null,
    val memberCount: Int? = 0
)

data class ConfirmedBookingData(
    val id: Int,
    val qrCode: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val price: Double? = null,
    val duration: Int? = null,
    val bookingDate: String? = null,
    val bookingReference: String? = null,
    val isDemo: Boolean = false,
    val team: BookingTeamData? = null,
    val receiptDownloadUrl: String? = null,
    val receiptNumber: String? = null,
    val paymentStatus: String? = null,
    val paymentAmount: Double? = null,
    val paymentCurrency: String? = "LKR"
)
