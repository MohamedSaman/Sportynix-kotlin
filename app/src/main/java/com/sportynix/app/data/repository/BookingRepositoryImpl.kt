package com.sportynix.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.local.dao.BookingDao
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.mapper.toEntity
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.repository.BookingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
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
        return try {
            val response = apiService.getUserBookings(page = 1, pageSize = 50, bookingType = bookingType, status = status)
            if (response.isSuccessful && response.body() != null) {
                val jsonElement = response.body()!!
                val dtos = parseBookingsJson(jsonElement)
                bookingDao.insertBookings(dtos.map { it.toEntity() })
                ApiResult.Success(dtos.map { it.toDomain() })
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Failed to fetch bookings")
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception fetching user bookings")
            ApiResult.Error(message = e.localizedMessage ?: "Network error fetching bookings")
        }
    }

    override suspend fun getBookingDetail(bookingId: String): ApiResult<Booking> {
        return try {
            val response = apiService.getBookingDetail(bookingId)
            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                val dtoList = parseBookingsJson(json)
                val dto = dtoList.firstOrNull() ?: gson.fromJson(json, BookingDto::class.java)
                bookingDao.insertBooking(dto.toEntity())
                ApiResult.Success(dto.toDomain())
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Booking not found")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Failed to load booking details")
        }
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
            val response = apiService.createBooking(request)
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
                            id = first.id,
                            venueId = venueId,
                            venueName = "Venue",
                            venueImageUrl = null,
                            slotTime = first.startTime ?: "07:00 AM",
                            bookingDate = date,
                            totalPrice = first.price ?: 400.0,
                            status = com.sportynix.app.domain.model.BookingStatus.CONFIRMED
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
        return try {
            val req = CancelBookingRequestDto(reason = reason)
            val response = if (isSeries) apiService.cancelSeries(bookingId, req) else apiService.cancelBooking(bookingId, req)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), "Cancellation failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Cancellation exception")
        }
    }

    override suspend fun assignTeam(bookingId: String, teamId: Int): ApiResult<Unit> {
        return try {
            val req = AssignTeamRequestDto(teamId = teamId)
            val response = apiService.assignTeam(bookingId, req)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), "Team assignment failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Team assignment exception")
        }
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

    private fun parseBookingsJson(jsonElement: JsonElement): List<BookingDto> {
        return try {
            val listType = object : TypeToken<List<BookingDto>>() {}.type
            when {
                jsonElement.isJsonArray -> gson.fromJson(jsonElement, listType)
                jsonElement.isJsonObject -> {
                    val obj = jsonElement.asJsonObject
                    when {
                        obj.has("results") && obj.get("results").isJsonArray -> gson.fromJson(obj.get("results"), listType)
                        obj.has("bookings") && obj.get("bookings").isJsonArray -> gson.fromJson(obj.get("bookings"), listType)
                        obj.has("data") && obj.get("data").isJsonArray -> gson.fromJson(obj.get("data"), listType)
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing bookings JSON")
            emptyList()
        }
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
