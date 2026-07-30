package com.sportynix.app.presentation.booking

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.remote.websocket.SlotAvailabilityWebSocketManager
import com.sportynix.app.data.repository.ProfileRepository
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class BookingDetailDisplayModel(
    val id: String,
    val venueName: String = "Sportynix Complex",
    val venueImage: String = "",
    val sportName: String = "Football",
    val status: String = "Confirmed",
    val bookingReference: String = "SPN-2026-88412",
    val date: String = "2026-07-29",
    val timeSlot: String = "07:00 AM - 08:00 AM",
    val totalPrice: Double = 800.0,
    val amountPaid: Double = 400.0,
    val balanceDue: Double = 400.0,
    val isPermanent: Boolean = false
)

data class BookingMainUiState(
    val venueId: Int = 0,
    val sportId: Int = 0,
    val sportName: String = "",
    val sportPrice: String = "",
    val sportImageURL: String = "",
    val complexName: String = "",
    val complexLocation: String = "",
    val complexRating: Double = 4.5,
    val complexReviews: Int = 0,
    val venueOpeningHours: Map<String, OpeningHourEntryDto>? = null,
    val sportOpeningHours: Map<String, OpeningHourEntryDto>? = null,

    val bookingType: Int = 0, // 0 = Normal, 1 = Permanent
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val selectedSlots: List<SlotData> = emptyList(),
    val selectedDays: List<String> = emptyList(),

    val apiSlots: List<SlotData> = emptyList(),
    val isLoadingSlots: Boolean = false,
    val slotsError: String? = null,

    val availableSports: List<VenueSportDto> = emptyList(),
    val isLoadingSportsList: Boolean = false,
    val showSportPicker: Boolean = false,
    val showDatePickerSheet: Boolean = false,
    val showMaxSlotsAlert: Boolean = false,
    val processingSlotKeys: Set<String> = emptySet(),

    val permanentAvailability: Map<String, PermanentSlotAvailability> = emptyMap(),
    val loadingPermanentAvailability: Boolean = false,
    val userExistingPermanentBookings: Set<String> = emptySet(),

    // Backwards-compatibility fields
    val quoteResponse: QuoteResponseDto? = null,
    val selectedBookingDetail: BookingDetailDisplayModel? = null,
    val userBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = false,

    // Phone verification
    val showPhoneVerificationSheet: Boolean = false,
    val phoneNumber: String = "",
    val phoneOTP: String = "",
    val phoneChallengeId: Int? = null,
    val isPhoneSending: Boolean = false,
    val isPhoneVerifying: Boolean = false,
    val phoneVerificationError: String? = null,
    val navigateToSummary: Boolean = false,
    val payload: BookingPayload? = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val profileRepository: ProfileRepository,
    private val slotWebSocketManager: SlotAvailabilityWebSocketManager
) : ViewModel() {

    var uiState by mutableStateOf(BookingMainUiState())
        private set

    val state: BookingMainUiState get() = uiState

    val currentUser = profileRepository.currentUser

    init {
        observeWebSocket()
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            slotWebSocketManager.connectedFlow.collectLatest {
                if (uiState.bookingType == 0) {
                    slotWebSocketManager.requestSlotAvailability(uiState.selectedDate, uiState.venueId)
                } else {
                    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    val dayNumbers = uiState.selectedDays.mapNotNull { day -> daysOfWeek.indexOf(day).takeIf { it >= 0 } }
                    slotWebSocketManager.requestPermanentAvailability(dayNumbers)
                }
            }
        }

        viewModelScope.launch {
            slotWebSocketManager.slotAvailabilityFlow.collectLatest { payload ->
                if (uiState.bookingType == 0) {
                    if (payload.date == null || payload.date == uiState.selectedDate) {
                        mergeIncomingSlots(payload.slots)
                        syncHeldSlotsFromAPI()
                        uiState = uiState.copy(isLoadingSlots = false)
                    }
                }
            }
        }

        viewModelScope.launch {
            slotWebSocketManager.permanentAvailabilityFlow.collectLatest { payload ->
                if (uiState.bookingType == 1) {
                    if (payload.type == "related_slot_change" || payload.type == "permanent_availability_refresh") {
                        fetchPermanentAvailability()
                    } else {
                        payload.availability?.let { avail ->
                            uiState = uiState.copy(permanentAvailability = avail, loadingPermanentAvailability = false)
                        }
                        payload.slots?.let { slots ->
                            mergeIncomingSlots(slots)
                        }
                    }
                }
            }
        }
    }

    fun initBooking(
        sportId: Int,
        venueId: Int,
        sportName: String,
        sportPrice: String,
        sportImageURL: String,
        complexName: String,
        complexLocation: String,
        complexRating: Double,
        complexReviews: Int,
        sportOpeningHours: Map<String, OpeningHourEntryDto>?,
        venueOpeningHours: Map<String, OpeningHourEntryDto>?
    ) {
        val dateToday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        uiState = uiState.copy(
            sportId = sportId,
            venueId = venueId,
            sportName = sportName,
            sportPrice = sportPrice,
            sportImageURL = sportImageURL,
            complexName = complexName,
            complexLocation = complexLocation,
            complexRating = complexRating,
            complexReviews = complexReviews,
            sportOpeningHours = sportOpeningHours,
            venueOpeningHours = venueOpeningHours,
            selectedDate = dateToday
        )

        val token = "authenticated_user_token"
        slotWebSocketManager.connect(sportId, token, "https://api.sportynix.com")

        if (uiState.bookingType == 0) {
            fetchSlots()
        }
    }

    fun loadVenueAndSlots(venueId: String, sportId: String, date: String) {
        val vId = venueId.toIntOrNull() ?: uiState.venueId
        val sId = sportId.toIntOrNull() ?: uiState.sportId
        uiState = uiState.copy(venueId = vId, sportId = sId, selectedDate = date)
        fetchSlots()
    }

    fun fetchQuote(
        venueId: String,
        sportId: String,
        date: String,
        slotIds: String,
        bookingType: String,
        selectedDays: String,
        paymentOption: String
    ) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val slots = slotIds.split(",").filter { it.isNotEmpty() }.map {
                QuoteSlotDto(startTime = "07:00", endTime = "08:00", duration = 60, price = 400.0)
            }
            val req = QuoteRequestDto(
                venueId = venueId,
                sportId = sportId,
                bookingType = bookingType,
                date = date,
                selectedDays = selectedDays.split(",").filter { it.isNotEmpty() },
                slots = slots,
                paymentOption = paymentOption
            )
            when (val res = bookingRepository.getQuote(req)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(quoteResponse = res.data, isLoading = false)
                }
                else -> {
                    uiState = uiState.copy(isLoading = false)
                }
            }
        }
    }

    fun confirmBookingOrCheckout(
        venueId: String,
        sportId: String,
        date: String,
        slotIds: String,
        bookingType: String,
        selectedDays: String,
        paymentOption: String,
        onPaymentCheckoutReady: (checkoutUrl: String, orderId: String, amount: Double) -> Unit,
        onDirectConfirmationReady: () -> Unit
    ) {
        viewModelScope.launch {
            val slots = slotIds.split(",").filter { it.isNotEmpty() }.map {
                CreateBookingSlotDto(startTime = "07:00", endTime = "08:00", duration = 60, price = 400.0)
            }
            val daysList = selectedDays.split(",").filter { it.isNotEmpty() }

            val checkoutReq = PaymentCheckoutRequestDto(
                venueId = venueId,
                sportId = sportId,
                bookingType = bookingType,
                date = date,
                selectedDays = daysList,
                slots = slots,
                paymentOption = paymentOption
            )

            when (val res = bookingRepository.createPaymentCheckout(checkoutReq)) {
                is ApiResult.Success -> {
                    val url = res.data.checkout?.url ?: "https://api.sportynix.com"
                    val orderId = res.data.payment?.orderId ?: "ORD-88412"
                    val amt = res.data.payment?.amount ?: 400.0
                    onPaymentCheckoutReady(url, orderId, amt)
                }
                else -> {
                    val createReq = CreateBookingRequestDto(
                        venueId = venueId,
                        sportId = sportId,
                        bookingType = bookingType,
                        date = date,
                        selectedDays = daysList,
                        slots = slots,
                        paymentOption = paymentOption
                    )
                    bookingRepository.createBooking(createReq)
                    onDirectConfirmationReady()
                }
            }
        }
    }

    fun pollPaymentStatus(
        orderId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = bookingRepository.getPaymentStatus(orderId)) {
                is ApiResult.Success -> {
                    val st = (res.data.paymentStatus ?: res.data.gatewayState ?: "").lowercase()
                    if (st in listOf("succeeded", "confirmed", "success", "completed")) {
                        bookingRepository.clearPendingPaymentSession()
                        onSuccess()
                    } else if (st in listOf("failed", "cancelled", "canceled", "declined")) {
                        onFailure("Payment was declined or cancelled. Please try again.")
                    } else {
                        onFailure("Verifying payment status with bank gateway...")
                    }
                }
                else -> {
                    onFailure("Verifying payment status...")
                }
            }
        }
    }

    fun loadBookingDetail(bookingId: String) {
        val bIdInt = bookingId.toIntOrNull() ?: 0
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            when (val res = bookingRepository.fetchBookingDetails(bIdInt)) {
                is ApiResult.Success -> {
                    val b = res.data
                    uiState = uiState.copy(
                        selectedBookingDetail = BookingDetailDisplayModel(
                            id = b.id.toString(),
                            venueName = b.complexName,
                            venueImage = b.imageURL,
                            sportName = b.sport,
                            status = b.status,
                            bookingReference = "SPN-${b.id}",
                            date = b.playDateStart,
                            timeSlot = b.timeSlot,
                            totalPrice = b.totalPrice,
                            amountPaid = b.totalPrice * 0.5,
                            balanceDue = b.totalPrice * 0.5,
                            isPermanent = b.isPermanent
                        ),
                        isLoading = false
                    )
                }
                else -> {
                    uiState = uiState.copy(
                        selectedBookingDetail = BookingDetailDisplayModel(id = bookingId),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun cancelBooking(
        bookingId: String,
        isSeries: Boolean = false,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        val bIdInt = bookingId.toIntOrNull() ?: return
        viewModelScope.launch {
            val res = if (isSeries) bookingRepository.cancelSeriesInt(bIdInt) else bookingRepository.cancelBookingInt(bIdInt)
            if (res is ApiResult.Success) onSuccess() else onFailure()
        }
    }

    fun setBookingType(type: Int) {
        releaseAllHolds()
        uiState = uiState.copy(
            bookingType = type,
            selectedSlots = emptyList(),
            selectedDays = emptyList(),
            permanentAvailability = emptyMap()
        )
        if (type == 0) {
            slotWebSocketManager.requestSlotAvailability(uiState.selectedDate, uiState.venueId)
            fetchSlots()
        } else {
            fetchPermanentAvailability()
        }
    }

    fun selectDate(date: String) {
        releaseAllHolds(targetDate = uiState.selectedDate)
        uiState = uiState.copy(selectedDate = date, apiSlots = emptyList(), selectedSlots = emptyList())
        slotWebSocketManager.requestSlotAvailability(date, uiState.venueId)
        fetchSlots()
    }

    fun toggleDaySelection(day: String) {
        val currentDays = uiState.selectedDays
        val newDays = if (currentDays.contains(day)) emptyList() else listOf(day)
        releaseAllHolds()
        uiState = uiState.copy(selectedDays = newDays, selectedSlots = emptyList())
        if (newDays.isNotEmpty()) {
            fetchPermanentAvailability()
        }
    }

    fun selectSport(sport: VenueSportDto) {
        releaseAllHolds()
        uiState = uiState.copy(
            sportId = sport.id ?: uiState.sportId,
            sportName = sport.name ?: uiState.sportName,
            sportPrice = sport.price ?: uiState.sportPrice,
            sportImageURL = sport.image ?: uiState.sportImageURL,
            selectedSlots = emptyList(),
            apiSlots = emptyList(),
            showSportPicker = false
        )
        slotWebSocketManager.disconnect()
        val token = "authenticated_user_token"
        slotWebSocketManager.connect(uiState.sportId, token, "https://api.sportynix.com")
        if (uiState.bookingType == 0) {
            fetchSlots()
        } else {
            fetchPermanentAvailability()
        }
    }

    fun setShowSportPicker(show: Boolean) {
        uiState = uiState.copy(showSportPicker = show)
    }

    fun setShowDatePickerSheet(show: Boolean) {
        uiState = uiState.copy(showDatePickerSheet = show)
    }

    fun setShowMaxSlotsAlert(show: Boolean) {
        uiState = uiState.copy(showMaxSlotsAlert = show)
    }

    fun fetchSlots() {
        uiState = uiState.copy(isLoadingSlots = true, slotsError = null)
        viewModelScope.launch {
            when (val res = bookingRepository.fetchAvailableSlots(uiState.sportId, uiState.venueId, uiState.selectedDate)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(apiSlots = res.data, isLoadingSlots = false)
                    syncHeldSlotsFromAPI()
                }
                is ApiResult.ServerError -> {
                    uiState = uiState.copy(slotsError = res.message, isLoadingSlots = false)
                }
                is ApiResult.Error -> {
                    uiState = uiState.copy(slotsError = res.message, isLoadingSlots = false)
                }
                else -> {
                    uiState = uiState.copy(isLoadingSlots = false)
                }
            }
        }
        if (uiState.bookingType == 0) {
            slotWebSocketManager.requestSlotAvailability(uiState.selectedDate, uiState.venueId)
        }
    }

    fun fetchPermanentAvailability() {
        if (uiState.selectedDays.isEmpty()) return
        uiState = uiState.copy(loadingPermanentAvailability = true)
        viewModelScope.launch {
            val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            val dayNumbers = uiState.selectedDays.mapNotNull { day -> daysOfWeek.indexOf(day).takeIf { it >= 0 } }
            when (val res = bookingRepository.fetchPermanentAvailability(uiState.sportId, dayNumbers)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(permanentAvailability = res.data, loadingPermanentAvailability = false)
                }
                else -> {
                    uiState = uiState.copy(loadingPermanentAvailability = false)
                }
            }
        }
    }

    fun handleSlotTap(slotKey: String, rawStart: String, rawEnd: String, displayStart: String, displayEnd: String, duration: Int) {
        val isSelectedLocally = uiState.selectedSlots.any { normalizedSlotKey(it) == slotKey }
        val isHeldByCurrentUser = uiState.apiSlots.any { normalizedSlotKey(it) == slotKey && it.heldByCurrentUser == true }

        if (isSelectedLocally || isHeldByCurrentUser) {
            releaseSlot(slotKey, rawStart, rawEnd)
        } else {
            if (uiState.selectedSlots.size >= 4) {
                uiState = uiState.copy(showMaxSlotsAlert = true)
                return
            }
            holdSlot(slotKey, rawStart, rawEnd, displayStart, displayEnd, duration)
        }
    }

    private fun holdSlot(slotKey: String, rawStart: String, rawEnd: String, displayStart: String, displayEnd: String, duration: Int) {
        val isPermanent = uiState.bookingType == 1
        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val dayNumbers = uiState.selectedDays.mapNotNull { day -> daysOfWeek.indexOf(day).takeIf { it >= 0 } }

        val newProcessing = uiState.processingSlotKeys + slotKey
        val newSlot = SlotData(
            startTime = displayStart,
            endTime = displayEnd,
            rawStart = rawStart,
            rawEnd = rawEnd,
            slotKey = slotKey,
            available = true,
            isPastTime = false,
            isFullyBooked = false,
            isHeld = false,
            disabledReason = null,
            duration = duration,
            heldByCurrentUser = true
        )
        val newSelected = uiState.selectedSlots + newSlot
        uiState = uiState.copy(processingSlotKeys = newProcessing, selectedSlots = newSelected)

        viewModelScope.launch {
            val res = bookingRepository.holdSlot(
                sportId = uiState.sportId,
                date = uiState.selectedDate,
                startTime = rawStart,
                endTime = rawEnd,
                isPermanent = isPermanent,
                selectedDays = if (isPermanent) dayNumbers else emptyList()
            )
            val updatedProcessing = uiState.processingSlotKeys - slotKey
            if (res is ApiResult.Success) {
                uiState = uiState.copy(processingSlotKeys = updatedProcessing)
                if (uiState.bookingType == 0) {
                    slotWebSocketManager.requestSlotAvailability(uiState.selectedDate, uiState.venueId)
                } else {
                    slotWebSocketManager.requestPermanentAvailability(dayNumbers)
                }
            } else {
                val revertedSelected = uiState.selectedSlots.filterNot { normalizedSlotKey(it) == slotKey }
                uiState = uiState.copy(processingSlotKeys = updatedProcessing, selectedSlots = revertedSelected)
                if (uiState.bookingType == 0) {
                    slotWebSocketManager.requestSlotAvailability(uiState.selectedDate, uiState.venueId)
                } else {
                    slotWebSocketManager.requestPermanentAvailability(dayNumbers)
                }
            }
        }
    }

    private fun releaseSlot(slotKey: String, rawStart: String, rawEnd: String) {
        val isPermanent = uiState.bookingType == 1
        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val dayNumbers = uiState.selectedDays.mapNotNull { day -> daysOfWeek.indexOf(day).takeIf { it >= 0 } }

        val newSelected = uiState.selectedSlots.filterNot { normalizedSlotKey(it) == slotKey }
        uiState = uiState.copy(selectedSlots = newSelected)

        if (uiState.bookingType == 0) {
            optimisticMarkReleased(slotKey)
        }

        viewModelScope.launch {
            bookingRepository.releaseSlot(
                sportId = uiState.sportId,
                date = uiState.selectedDate,
                startTime = rawStart,
                endTime = rawEnd,
                isPermanent = isPermanent,
                selectedDays = if (isPermanent) dayNumbers else emptyList()
            )
            if (uiState.bookingType == 0) {
                slotWebSocketManager.requestSlotAvailability(uiState.selectedDate, uiState.venueId)
                fetchSlots()
            } else {
                slotWebSocketManager.requestPermanentAvailability(dayNumbers)
                fetchPermanentAvailability()
            }
        }
    }

    fun releaseAllHolds(targetDate: String? = null) {
        if (uiState.selectedSlots.isEmpty()) return
        val slotsToRelease = uiState.selectedSlots
        uiState = uiState.copy(selectedSlots = emptyList())
        val isPermanent = uiState.bookingType == 1
        val dateToRelease = targetDate ?: uiState.selectedDate
        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val dayNumbers = uiState.selectedDays.mapNotNull { day -> daysOfWeek.indexOf(day).takeIf { it >= 0 } }

        viewModelScope.launch {
            for (slot in slotsToRelease) {
                val rs = slot.rawStart ?: ""
                val re = slot.rawEnd ?: ""
                if (rs.isNotEmpty() && re.isNotEmpty()) {
                    bookingRepository.releaseSlot(
                        sportId = uiState.sportId,
                        date = dateToRelease,
                        startTime = rs,
                        endTime = re,
                        isPermanent = isPermanent,
                        selectedDays = if (isPermanent) dayNumbers else emptyList()
                    )
                }
            }
            if (uiState.bookingType == 0) {
                slotWebSocketManager.requestSlotAvailability(dateToRelease, uiState.venueId)
            } else {
                slotWebSocketManager.requestPermanentAvailability(dayNumbers)
            }
        }
    }

    fun checkPhoneVerificationAndProceed() {
        val user = currentUser.value
        val hasPhoneVerification = !user?.phoneVerifiedAt.isNullOrEmpty()

        if (hasPhoneVerification) {
            preparePayloadAndNavigate()
        } else {
            val phone = user?.phoneNumber?.trim() ?: ""
            uiState = uiState.copy(
                phoneNumber = phone,
                phoneOTP = "",
                phoneChallengeId = null,
                phoneVerificationError = null,
                showPhoneVerificationSheet = true
            )
        }
    }

    fun updatePhoneNumber(phone: String) {
        uiState = uiState.copy(phoneNumber = phone)
    }

    fun updatePhoneOTP(otp: String) {
        uiState = uiState.copy(phoneOTP = otp)
    }

    fun dismissPhoneVerificationSheet() {
        uiState = uiState.copy(
            showPhoneVerificationSheet = false,
            phoneNumber = "",
            phoneOTP = "",
            phoneChallengeId = null,
            phoneVerificationError = null
        )
    }

    fun sendPhoneOTP() {
        val cleaned = uiState.phoneNumber.trim()
        if (cleaned.length != 10) {
            uiState = uiState.copy(phoneVerificationError = "Please enter a valid phone number in format 07XXXXXXXX")
            return
        }
        uiState = uiState.copy(isPhoneSending = true, phoneVerificationError = null)
        viewModelScope.launch {
            val res = profileRepository.sendPhoneOtp(cleaned)
            res.onSuccess { dto ->
                uiState = uiState.copy(phoneChallengeId = dto.challengeId, isPhoneSending = false)
            }.onFailure { err ->
                uiState = uiState.copy(phoneVerificationError = err.localizedMessage, isPhoneSending = false)
            }
        }
    }

    fun verifyPhoneOTP() {
        val challengeId = uiState.phoneChallengeId ?: return
        val otpCode = uiState.phoneOTP.trim()
        if (otpCode.length != 6 || !otpCode.all { it.isDigit() }) {
            uiState = uiState.copy(phoneVerificationError = "Please enter a valid 6-digit OTP")
            return
        }
        uiState = uiState.copy(isPhoneVerifying = true, phoneVerificationError = null)
        viewModelScope.launch {
            val res = profileRepository.verifyPhoneOtp(challengeId, otpCode)
            res.onSuccess {
                uiState = uiState.copy(
                    isPhoneVerifying = false,
                    showPhoneVerificationSheet = false,
                    phoneNumber = "",
                    phoneOTP = "",
                    phoneChallengeId = null
                )
                preparePayloadAndNavigate()
            }.onFailure { err ->
                uiState = uiState.copy(phoneVerificationError = err.localizedMessage, isPhoneVerifying = false)
            }
        }
    }

    private fun preparePayloadAndNavigate() {
        val pricePerHour = parsePrice(uiState.sportPrice)
        val base = uiState.selectedSlots.size * pricePerHour
        val totalPrice = if (uiState.bookingType == 1) base * uiState.selectedDays.size * 4.0 else base

        val slotInfos = uiState.selectedSlots.map { slot ->
            val rs = slot.rawStart ?: ""
            val re = slot.rawEnd ?: ""
            BookingSlotInfo(
                startTime = rs,
                endTime = re,
                displayStart = slot.startTime ?: formatTo12Hour(rs),
                displayEnd = slot.endTime ?: formatTo12Hour(re),
                duration = slot.duration ?: 60,
                price = pricePerHour
            )
        }

        val payload = BookingPayload(
            sportId = uiState.sportId,
            sportName = uiState.sportName,
            sportPrice = uiState.sportPrice,
            sportImageURL = uiState.sportImageURL,
            venueId = uiState.venueId,
            venueName = uiState.complexName,
            venueAddress = uiState.complexLocation,
            bookingType = if (uiState.bookingType == 0) "Normal" else "Permanent",
            bookingDate = uiState.selectedDate,
            selectedDays = uiState.selectedDays,
            slots = slotInfos,
            totalPrice = totalPrice
        )

        uiState = uiState.copy(payload = payload, navigateToSummary = true)
    }

    fun clearNavigation() {
        uiState = uiState.copy(navigateToSummary = false)
    }

    private fun parsePrice(priceStr: String): Double {
        var cleaned = priceStr.replace("Rs.", "").replace("Rs", "").replace("/hour", "").replace(",", "").trim()
        cleaned.toDoubleOrNull()?.let { return it }
        val digits = cleaned.filter { it.isDigit() || it == '.' }
        return digits.toDoubleOrNull() ?: 500.0
    }

    private fun formatTo12Hour(time24: String): String {
        val parts = time24.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.size < 2) return time24
        val h = parts[0]
        val m = parts[1]
        val period = if (h >= 12) "PM" else "AM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format("%02d:%02d %s", h12, m, period)
    }

    private fun normalizedSlotKey(slot: SlotData): String {
        slot.slotKey?.let { if (it.isNotEmpty()) return it.replace("-24:00", "-00:00") }
        val rs = slot.rawStart ?: slot.startTime ?: ""
        val reRaw = slot.rawEnd ?: slot.endTime ?: ""
        val re = reRaw.replace("24:00", "00:00")
        return "$rs-$re"
    }

    private fun mergeIncomingSlots(incoming: List<SlotData>) {
        val map = mutableMapOf<String, SlotData>()
        for (slot in uiState.apiSlots) {
            map[normalizedSlotKey(slot)] = slot
        }
        for (slot in incoming) {
            map[normalizedSlotKey(slot)] = slot
        }
        val sorted = map.values.sortedBy { it.rawStart ?: it.startTime ?: "" }
        uiState = uiState.copy(apiSlots = sorted)
    }

    private fun optimisticMarkReleased(slotKey: String) {
        var didUpdate = false
        val updated = uiState.apiSlots.map { slot ->
            if (normalizedSlotKey(slot) == slotKey) {
                didUpdate = true
                slot.copy(
                    available = true,
                    isFullyBooked = false,
                    isHeld = false,
                    disabledReason = null,
                    heldByCurrentUser = false,
                    availableCourts = (slot.availableCourts ?: 0) + 1
                )
            } else slot
        }
        uiState = if (didUpdate) {
            uiState.copy(apiSlots = updated)
        } else {
            val parts = slotKey.split("-")
            if (parts.size == 2) {
                val newSlot = SlotData(
                    startTime = formatTo12Hour(parts[0]),
                    endTime = formatTo12Hour(parts[1]),
                    rawStart = parts[0],
                    rawEnd = parts[1],
                    slotKey = slotKey,
                    available = true,
                    isPastTime = false,
                    isFullyBooked = false,
                    isHeld = false,
                    disabledReason = null,
                    duration = 60,
                    heldByCurrentUser = false,
                    availableCourts = 1
                )
                uiState.copy(apiSlots = uiState.apiSlots + newSlot)
            } else uiState
        }
    }

    private fun syncHeldSlotsFromAPI() {
        val currentSelected = uiState.selectedSlots.toMutableList()
        for (apiSlot in uiState.apiSlots) {
            if (apiSlot.heldByCurrentUser == true) {
                val key = normalizedSlotKey(apiSlot)
                if (currentSelected.none { normalizedSlotKey(it) == key }) {
                    currentSelected.add(apiSlot)
                }
            }
        }
        uiState = uiState.copy(selectedSlots = currentSelected)
    }

    override fun onCleared() {
        super.onCleared()
        slotWebSocketManager.disconnect()
        if (!uiState.navigateToSummary) {
            releaseAllHolds()
        }
    }
}
