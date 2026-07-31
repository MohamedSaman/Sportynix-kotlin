package com.sportynix.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.local.dao.BookingDao
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.repository.BookingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: BookingApiService,
    private val bookingDao: BookingDao
) : BookingRepository {

    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences("booking_pending_session_prefs", Context.MODE_PRIVATE)

    override fun getBookingsStream(): Flow<List<Booking>> {
        return bookingDao.getAllBookings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun fetchUserBookings(bookingType: String?, status: String?): ApiResult<List<Booking>> {
        return fetchBookings()
    }

    override suspend fun getBookingDetail(bookingId: String): ApiResult<Booking> {
        val idInt = bookingId.toIntOrNull() ?: return ApiResult.Error(message = "Invalid booking ID")
        return fetchBookingDetails(idInt)
    }

    override suspend fun getQuote(request: QuoteRequestDto): ApiResult<QuoteResponseDto> {
        return try {
            val response = apiService.getQuote(request)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                val err = response.errorBody()?.string() ?: "Failed to retrieve quote"
                ApiResult.ServerError(response.code(), err)
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error fetching quote")
        }
    }

    override suspend fun createBooking(request: CreateBookingRequestDto): ApiResult<List<ConfirmedBookingDto>> {
        return try {
            val response = apiService.createBookingRaw(
                mapOf(
                    "game_id" to request.sportId,
                    "complex_id" to request.venueId,
                    "booking_date" to request.date,
                    "slots" to request.slots.map { mapOf("start_time" to it.startTime, "end_time" to it.endTime, "duration" to it.duration, "price" to it.price) },
                    "user_name" to (request.userName ?: ""),
                    "user_email" to (request.userEmail ?: ""),
                    "user_number" to (request.userNumber ?: "")
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                val dtos = parseConfirmedBookingsJson(json)
                ApiResult.Success(dtos)
            } else {
                val err = response.errorBody()?.string() ?: "Booking creation failed"
                ApiResult.ServerError(response.code(), err)
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Booking creation exception")
        }
    }

    override suspend fun createSimpleBooking(venueId: String, slotId: String, date: String): ApiResult<Booking> {
        val req = CreateBookingRequestDto(
            venueId = venueId,
            sportId = "1",
            date = date,
            slots = listOf(CreateBookingSlotDto(startTime = "07:00", endTime = "08:00"))
        )
        return when (val res = createBooking(req)) {
            is ApiResult.Success -> {
                val first = res.data.firstOrNull()
                if (first != null) {
                    ApiResult.Success(
                        Booking(
                            id = first.id.toIntOrNull() ?: 0,
                            complexName = "Venue",
                            sport = "Badminton",
                            courtName = "Court 1",
                            teamName = "Personal",
                            memberCount = 0,
                            teamId = null,
                            playDateStart = date,
                            playDateEnd = date,
                            timeSlot = first.startTime ?: "07:00 AM",
                            duration = "60 mins",
                            location = "N/A",
                            price = "LKR ${first.price ?: 400.0}",
                            slotCount = 1,
                            bookingId = first.id.toIntOrNull() ?: 0,
                            bookedDate = date,
                            status = "Upcoming",
                            isPermanent = false,
                            permanentSourceId = null,
                            imageURL = "",
                            qrCode = !first.qrCode.isNullOrEmpty(),
                            qrCodeURL = first.qrCode,
                            venueId = venueId.toIntOrNull(),
                            sportId = 1,
                            reviewId = null,
                            reviewRating = null,
                            isChallengeBooking = false,
                            opponentTeamName = null,
                            opponentMemberCount = null,
                            userId = null,
                            canCancel = true,
                            createdAt = date
                        )
                    )
                } else ApiResult.Error(message = "No booking created")
            }
            is ApiResult.ServerError -> ApiResult.ServerError(res.code, res.message)
            is ApiResult.Error -> ApiResult.Error(res.code, res.message)
            else -> ApiResult.Error(message = "Failed to create simple booking")
        }
    }

    override suspend fun createPaymentCheckout(request: PaymentCheckoutRequestDto): ApiResult<PaymentCheckoutResponseDto> {
        return try {
            val response = apiService.createPaymentCheckout(request)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                val err = response.errorBody()?.string() ?: "Checkout initiation failed"
                ApiResult.ServerError(response.code(), err)
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Payment checkout exception")
        }
    }

    override suspend fun getPaymentStatus(orderId: String): ApiResult<PaymentStatusResponseDto> {
        return try {
            val response = apiService.getPaymentStatus(orderId)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                val err = response.errorBody()?.string() ?: "Payment status check failed"
                ApiResult.ServerError(response.code(), err)
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Payment status exception")
        }
    }

    override suspend fun cancelBooking(bookingId: String, reason: String, isSeries: Boolean): ApiResult<Unit> {
        val idInt = bookingId.toIntOrNull() ?: return ApiResult.Error(message = "Invalid booking ID")
        return if (isSeries) cancelSeriesInt(idInt) else cancelBookingInt(idInt)
    }

    override suspend fun assignTeam(bookingId: String, teamId: Int): ApiResult<Unit> {
        val idInt = bookingId.toIntOrNull() ?: return ApiResult.Error(message = "Invalid booking ID")
        return assignTeamInt(idInt, teamId)
    }

    override suspend fun savePendingPaymentSession(sessionJson: String) {
        prefs.edit().putString("pending_payment_session", sessionJson).apply()
    }

    override suspend fun getPendingPaymentSession(): String? {
        return prefs.getString("pending_payment_session", null)
    }

    override suspend fun clearPendingPaymentSession() {
        prefs.edit().remove("pending_payment_session").apply()
    }

    // ── Live Swift Equivalent Method Implementations ──

    override suspend fun fetchAvailableSlots(sportId: Int, venueId: Int, date: String): ApiResult<List<SlotData>> {
        return try {
            val response = apiService.fetchAvailableSlots(sportId, venueId, date)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val list = body.availableSlots ?: body.slots ?: emptyList()
                val normalized = list.map { slot ->
                    val rs = slot.rawStart ?: slot.startTime?.take(5)
                    val reCandidate = slot.rawEnd ?: slot.endTime?.take(5)
                    val re = reCandidate?.replace("24:00", "00:00")
                    val keyCandidate = slot.slotKey ?: if (rs != null && re != null) "$rs-$re" else null
                    val normalizedKey = keyCandidate?.replace("-24:00", "-00:00")
                    slot.copy(rawStart = rs, rawEnd = re, slotKey = normalizedKey)
                }
                ApiResult.Success(normalized)
            } else {
                ApiResult.ServerError(response.code(), response.errorBody()?.string() ?: "Failed to fetch slots")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Failed to fetch available slots")
        }
    }

    override suspend fun fetchPermanentAvailability(sportId: Int, selectedDays: List<Int>): ApiResult<Map<String, PermanentSlotAvailability>> {
        return try {
            val body = mapOf("selected_days" to selectedDays)
            val response = apiService.fetchPermanentAvailability(sportId, body)
            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                val result = mutableMapOf<String, PermanentSlotAvailability>()
                if (json.isJsonObject && json.asJsonObject.has("availability")) {
                    val availObj = json.asJsonObject.get("availability")
                    val type = object : TypeToken<Map<String, PermanentSlotAvailability>>() {}.type
                    val map: Map<String, PermanentSlotAvailability> = gson.fromJson(availObj, type)
                    map.forEach { (k, v) ->
                        result[k.replace("-24:00", "-00:00")] = v
                    }
                }
                ApiResult.Success(result)
            } else {
                ApiResult.ServerError(response.code(), "Failed to fetch permanent availability")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error fetching permanent availability")
        }
    }

    override suspend fun holdSlot(sportId: Int, date: String, startTime: String, endTime: String, isPermanent: Boolean, selectedDays: List<Int>): ApiResult<Unit> {
        return try {
            val body = mutableMapOf<String, Any>(
                "sport_id" to sportId,
                "date" to date,
                "start_time" to startTime,
                "end_time" to endTime
            )
            if (isPermanent) {
                body["is_permanent"] = true
                body["selected_days"] = selectedDays
            }
            val response = apiService.holdSlot(body)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), response.errorBody()?.string() ?: "Failed to hold slot")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error holding slot")
        }
    }

    override suspend fun releaseSlot(sportId: Int, date: String, startTime: String, endTime: String, isPermanent: Boolean, selectedDays: List<Int>): ApiResult<Unit> {
        return try {
            val body = mutableMapOf<String, Any>(
                "sport_id" to sportId,
                "date" to date,
                "start_time" to startTime,
                "end_time" to endTime
            )
            if (isPermanent) {
                body["is_permanent"] = true
                body["selected_days"] = selectedDays
            }
            val response = apiService.releaseSlot(body)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), response.errorBody()?.string() ?: "Failed to release slot")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error releasing slot")
        }
    }

    override suspend fun convertHoldsToBookings(sportId: Int, bookingType: String, slots: List<Map<String, Any>>, selectedDays: List<String>): ApiResult<Unit> {
        return try {
            val body = mapOf(
                "sport_id" to sportId,
                "booking_type" to bookingType,
                "slots" to slots,
                "selected_days" to selectedDays
            )
            val response = apiService.convertHoldsToBookings(body)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), response.errorBody()?.string() ?: "Failed to convert holds")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error converting holds")
        }
    }

    override suspend fun createBooking(payload: BookingPayload, userName: String, userEmail: String, userPhone: String): ApiResult<List<ConfirmedBookingData>> {
        return try {
            val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            val dayNumbers = payload.selectedDays.mapNotNull { day -> daysOfWeek.indexOf(day).takeIf { it >= 0 } }

            val slotsArray = payload.slots.map { slot ->
                mapOf(
                    "start_time" to slot.startTime,
                    "end_time" to slot.endTime,
                    "duration" to slot.duration,
                    "price" to slot.price
                )
            }

            val body = mutableMapOf<String, Any>(
                "game_id" to payload.sportId,
                "complex_id" to payload.venueId,
                "booking_date" to payload.bookingDate,
                "slots" to slotsArray,
                "user_name" to userName,
                "user_email" to userEmail,
                "user_number" to userPhone,
                "payment_method" to "Card",
                "recurring_type" to if (payload.bookingType == "Permanent") "weekly" else "none",
                "recurring_days" to if (payload.bookingType == "Permanent") dayNumbers else emptyList<Int>(),
                "points_to_redeem" to 0
            )

            val response = apiService.createBookingRaw(body)
            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                val confirmed = parseConfirmedBookingsCustom(json)
                ApiResult.Success(confirmed)
            } else {
                ApiResult.ServerError(response.code(), response.errorBody()?.string() ?: "Failed to create booking")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error creating booking")
        }
    }

    override suspend fun fetchBookings(): ApiResult<List<Booking>> {
        return try {
            val response = apiService.getUserBookings(page = 1, pageSize = 100)
            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                val apiBookings = parseAPIBookingsJson(json)
                val domainList = apiBookings.map { it.toDomain() }
                ApiResult.Success(domainList)
            } else {
                ApiResult.ServerError(response.code(), response.errorBody()?.string() ?: "Failed to fetch bookings")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error fetching bookings")
        }
    }

    override suspend fun fetchBookingDetails(id: Int): ApiResult<Booking> {
        return try {
            val response = apiService.getBookingDetail(id)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.toDomain())
            } else {
                ApiResult.ServerError(response.code(), "Booking not found")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Failed to load booking details")
        }
    }

    override suspend fun fetchBookingQRCode(id: Int): ApiResult<String> {
        return try {
            val response = apiService.getBookingQRCode(id)
            if (response.isSuccessful && response.body() != null) {
                val qr = response.body()!!.qrCode
                if (!qr.isNullOrEmpty()) {
                    ApiResult.Success(qr)
                } else {
                    ApiResult.Error(message = "QR code not available")
                }
            } else {
                ApiResult.ServerError(response.code(), "Failed to fetch QR code")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error fetching QR code")
        }
    }

    override suspend fun cancelBookingInt(id: Int): ApiResult<Unit> {
        return try {
            val response = apiService.cancelBooking(id)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), "Failed to cancel booking")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error cancelling booking")
        }
    }

    override suspend fun cancelSeriesInt(id: Int): ApiResult<Unit> {
        return try {
            val response = apiService.cancelSeries(id)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), "Failed to cancel booking series")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error cancelling booking series")
        }
    }

    override suspend fun assignTeamInt(id: Int, teamId: Int): ApiResult<Unit> {
        return try {
            val response = apiService.assignTeam(id, mapOf("team_id" to teamId))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), "Failed to assign team")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error assigning team")
        }
    }

    override suspend fun removeTeamInt(id: Int): ApiResult<Unit> {
        return try {
            val response = apiService.removeTeam(id)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), "Failed to remove team")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error removing team")
        }
    }

    override suspend fun fetchSportsForVenue(venueId: Int): ApiResult<List<VenueSportDto>> {
        return try {
            val response = apiService.fetchSportsForVenue(venueId)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.ServerError(response.code(), "Failed to fetch sports for venue")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error fetching sports")
        }
    }

    override suspend fun fetchMyTeams(): ApiResult<List<BookingTeamData>> {
        return try {
            val response = apiService.getMyTeams()
            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                val result = mutableListOf<BookingTeamData>()
                val parseTeam = { obj: com.google.gson.JsonObject ->
                    val id = if (obj.has("id")) obj.get("id").asInt else null
                    val name = if (obj.has("name") && !obj.get("name").isJsonNull) obj.get("name").asString else null
                    val count = if (obj.has("members_count") && !obj.get("members_count").isJsonNull) obj.get("members_count").asInt else 0
                    if (id != null) BookingTeamData(id, name, count) else null
                }
                if (json.isJsonArray) {
                    json.asJsonArray.forEach { elem ->
                        if (elem.isJsonObject) parseTeam(elem.asJsonObject)?.let { result.add(it) }
                    }
                } else if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    if (obj.has("results") && obj.get("results").isJsonArray) {
                        obj.get("results").asJsonArray.forEach { elem ->
                            if (elem.isJsonObject) parseTeam(elem.asJsonObject)?.let { result.add(it) }
                        }
                    }
                }
                ApiResult.Success(result)
            } else {
                ApiResult.ServerError(response.code(), "Failed to fetch teams")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Error fetching teams")
        }
    }

    private fun parseAPIBookingsJson(jsonElement: JsonElement): List<APIBooking> {
        return try {
            val listType = object : TypeToken<List<APIBooking>>() {}.type
            when {
                jsonElement.isJsonArray -> gson.fromJson(jsonElement, listType)
                jsonElement.isJsonObject -> {
                    val obj = jsonElement.asJsonObject
                    when {
                        obj.has("results") && obj.get("results").isJsonArray -> gson.fromJson(obj.get("results"), listType)
                        obj.has("bookings") && obj.get("bookings").isJsonArray -> gson.fromJson(obj.get("bookings"), listType)
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing APIBooking list")
            emptyList()
        }
    }

    private fun parseConfirmedBookingsCustom(jsonElement: JsonElement): List<ConfirmedBookingData> {
        val result = mutableListOf<ConfirmedBookingData>()
        try {
            if (jsonElement.isJsonArray) {
                jsonElement.asJsonArray.forEach { elem ->
                    if (elem.isJsonObject) parseConfirmedItem(elem.asJsonObject)?.let { result.add(it) }
                }
            } else if (jsonElement.isJsonObject) {
                val obj = jsonElement.asJsonObject
                if (obj.has("bookings") && obj.get("bookings").isJsonArray) {
                    obj.get("bookings").asJsonArray.forEach { elem ->
                        if (elem.isJsonObject) parseConfirmedItem(elem.asJsonObject)?.let { result.add(it) }
                    }
                } else if (obj.has("id")) {
                    parseConfirmedItem(obj)?.let { result.add(it) }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing confirmed bookings")
        }
        return result
    }

    private fun parseConfirmedItem(obj: com.google.gson.JsonObject): ConfirmedBookingData? {
        val id = if (obj.has("id")) obj.get("id").asInt else return null
        val qr = if (obj.has("qr_code") && !obj.get("qr_code").isJsonNull) obj.get("qr_code").asString else null
        val startTime = if (obj.has("start_time") && !obj.get("start_time").isJsonNull) obj.get("start_time").asString else null
        val endTime = if (obj.has("end_time") && !obj.get("end_time").isJsonNull) obj.get("end_time").asString else null
        val price = if (obj.has("price") && !obj.get("price").isJsonNull) obj.get("price").asDouble else null
        val duration = if (obj.has("duration") && !obj.get("duration").isJsonNull) obj.get("duration").asInt else null
        val date = if (obj.has("booking_date") && !obj.get("booking_date").isJsonNull) obj.get("booking_date").asString else null
        val ref = if (obj.has("booking_reference") && !obj.get("booking_reference").isJsonNull) obj.get("booking_reference").asString else null

        var team: BookingTeamData? = null
        if (obj.has("team") && obj.get("team").isJsonObject) {
            val tObj = obj.getAsJsonObject("team")
            if (tObj.has("id")) {
                val tId = tObj.get("id").asInt
                val tName = if (tObj.has("name") && !tObj.get("name").isJsonNull) tObj.get("name").asString else null
                val mCount = if (tObj.has("members_count") && !tObj.get("members_count").isJsonNull) tObj.get("members_count").asInt else 0
                team = BookingTeamData(tId, tName, mCount)
            }
        }

        return ConfirmedBookingData(id, qr, startTime, endTime, price, duration, date, ref, false, team)
    }

    private fun parseConfirmedBookingsJson(jsonElement: JsonElement): List<ConfirmedBookingDto> {
        return try {
            val listType = object : TypeToken<List<ConfirmedBookingDto>>() {}.type
            when {
                jsonElement.isJsonArray -> gson.fromJson(jsonElement, listType)
                jsonElement.isJsonObject -> {
                    val obj = jsonElement.asJsonObject
                    when {
                        obj.has("bookings") && obj.get("bookings").isJsonArray -> gson.fromJson(obj.get("bookings"), listType)
                        obj.has("results") && obj.get("results").isJsonArray -> gson.fromJson(obj.get("results"), listType)
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
