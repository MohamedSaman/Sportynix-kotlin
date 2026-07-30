package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.sportynix.app.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface BookingApiService {
    @GET("api/my-bookings/")
    suspend fun getUserBookings(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
        @Query("booking_type") bookingType: String? = null,
        @Query("status") status: String? = null
    ): Response<JsonElement>

    @GET("api/my-bookings/{id}/")
    suspend fun getBookingDetail(@Path("id") id: Int): Response<APIBooking>

    @GET("api/my-bookings/{id}/qr_code/")
    suspend fun getBookingQRCode(@Path("id") id: Int): Response<QrCodeResponseDto>

    @GET("api/available_slots/{sportId}/")
    suspend fun fetchAvailableSlots(
        @Path("sportId") sportId: Int,
        @Query("venue") venueId: Int,
        @Query("date") date: String,
        @Query("exclude_current_user_holds") excludeCurrentUserHolds: Boolean? = null
    ): Response<AvailableSlotsResponse>

    @POST("api/sports/{sportId}/permanent_availability/")
    suspend fun fetchPermanentAvailability(
        @Path("sportId") sportId: Int,
        @Body body: Map<String, List<Int>>
    ): Response<JsonElement>

    @POST("api/hold_slot/")
    suspend fun holdSlot(@Body body: Map<String, Any>): Response<JsonElement>

    @POST("api/release_slot/")
    suspend fun releaseSlot(@Body body: Map<String, Any>): Response<JsonElement>

    @POST("api/convert_holds_to_bookings/")
    suspend fun convertHoldsToBookings(@Body body: Map<String, Any>): Response<JsonElement>

    @POST("api/bookings/")
    suspend fun createBookingRaw(@Body body: Map<String, Any>): Response<JsonElement>

    @POST("api/bookings/quote/")
    suspend fun getQuote(@Body request: QuoteRequestDto): Response<QuoteResponseDto>

    @POST("api/payments/checkout/")
    suspend fun createPaymentCheckout(@Body request: PaymentCheckoutRequestDto): Response<PaymentCheckoutResponseDto>

    @GET("api/payments/{orderId}/status/")
    suspend fun getPaymentStatus(@Path("orderId") orderId: String): Response<PaymentStatusResponseDto>

    @POST("api/bookings/{id}/assign_team/")
    suspend fun assignTeam(
        @Path("id") id: Int,
        @Body body: Map<String, Int>
    ): Response<JsonElement>

    @POST("api/bookings/{id}/remove_team/")
    suspend fun removeTeam(@Path("id") id: Int): Response<JsonElement>

    @POST("api/bookings/{id}/cancel/")
    suspend fun cancelBooking(
        @Path("id") id: Int,
        @Body request: CancelBookingRequestDto = CancelBookingRequestDto()
    ): Response<JsonElement>

    @POST("api/bookings/{id}/cancel_series/")
    suspend fun cancelSeries(
        @Path("id") id: Int,
        @Body request: CancelBookingRequestDto = CancelBookingRequestDto()
    ): Response<JsonElement>

    @GET("api/venues/{venueId}/sports/{sportId}/reviews/")
    suspend fun getSportReviews(
        @Path("venueId") venueId: Int,
        @Path("sportId") sportId: Int
    ): Response<JsonElement>

    @Multipart
    @POST("api/venues/{venueId}/sports/{sportId}/reviews/")
    suspend fun submitSportReview(
        @Path("venueId") venueId: Int,
        @Path("sportId") sportId: Int,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part images: List<MultipartBody.Part>
    ): Response<JsonElement>

    @Multipart
    @PUT("api/venues/{venueId}/sports/{sportId}/reviews/{reviewId}/")
    suspend fun updateSportReview(
        @Path("venueId") venueId: Int,
        @Path("sportId") sportId: Int,
        @Path("reviewId") reviewId: Int,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part images: List<MultipartBody.Part>
    ): Response<JsonElement>
}
