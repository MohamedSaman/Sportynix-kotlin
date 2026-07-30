package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.sportynix.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface BookingApiService {
    @GET("api/my-bookings/")
    suspend fun getUserBookings(
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
        @Query("booking_type") bookingType: String? = null,
        @Query("status") status: String? = null
    ): Response<JsonElement>

    @GET("api/bookings/{id}/")
    suspend fun getBookingDetail(@Path("id") id: String): Response<JsonElement>

    @GET("api/my-bookings/{id}/")
    suspend fun getMyBookingDetail(@Path("id") id: String): Response<JsonElement>

    @POST("api/bookings/quote/")
    suspend fun getQuote(@Body request: QuoteRequestDto): Response<QuoteResponseDto>

    @POST("api/my-bookings/")
    suspend fun createBooking(@Body request: CreateBookingRequestDto): Response<JsonElement>

    @POST("api/payments/checkout/")
    suspend fun createPaymentCheckout(@Body request: PaymentCheckoutRequestDto): Response<PaymentCheckoutResponseDto>

    @GET("api/payments/{orderId}/status/")
    suspend fun getPaymentStatus(@Path("orderId") orderId: String): Response<PaymentStatusResponseDto>

    @POST("api/bookings/{id}/assign-team/")
    suspend fun assignTeam(
        @Path("id") id: String,
        @Body body: AssignTeamRequestDto
    ): Response<JsonElement>

    @POST("api/bookings/{id}/cancel/")
    suspend fun cancelBooking(
        @Path("id") id: String,
        @Body request: CancelBookingRequestDto = CancelBookingRequestDto()
    ): Response<JsonElement>

    @POST("api/bookings/{id}/cancel_series/")
    suspend fun cancelSeries(
        @Path("id") id: String,
        @Body request: CancelBookingRequestDto = CancelBookingRequestDto()
    ): Response<JsonElement>

    @POST("api/users/request-otp/")
    suspend fun requestPhoneOtp(@Body body: Map<String, String>): Response<JsonElement>

    @POST("api/users/verify-otp/")
    suspend fun verifyPhoneOtp(@Body body: Map<String, String>): Response<JsonElement>
}
