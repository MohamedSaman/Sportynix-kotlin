package com.sportynix.app.data.remote.api

import com.sportynix.app.data.remote.dto.BookingDto
import com.sportynix.app.data.remote.dto.CreateBookingRequestDto
import com.sportynix.app.data.remote.dto.QrCodeResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BookingApiService {
    @GET("api/my-bookings/")
    suspend fun getUserBookings(): Response<List<BookingDto>>

    @POST("api/bookings/")
    suspend fun createBooking(@Body request: CreateBookingRequestDto): Response<BookingDto>

    @POST("api/bookings/{id}/cancel/")
    suspend fun cancelBooking(@Path("id") id: String): Response<Unit>

    @GET("api/my-bookings/{id}/qr_code/")
    suspend fun getBookingQrCode(@Path("id") id: String): Response<QrCodeResponseDto>
}
