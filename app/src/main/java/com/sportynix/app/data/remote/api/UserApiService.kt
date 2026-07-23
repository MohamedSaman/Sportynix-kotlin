package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.sportynix.app.data.remote.dto.FavoriteVenueDto
import com.sportynix.app.data.remote.dto.PaginatedFavoritesDto
import com.sportynix.app.data.remote.dto.PhoneOtpVerifyRequestDto
import com.sportynix.app.data.remote.dto.PhoneVerifyRequestDto
import com.sportynix.app.data.remote.dto.PhoneVerifyResponseDto
import com.sportynix.app.data.remote.dto.PointsDto
import com.sportynix.app.data.remote.dto.ReferralDto
import com.sportynix.app.data.remote.dto.UpdateProfileRequestDto
import com.sportynix.app.data.remote.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApiService {

    @GET("api/users/me/")
    suspend fun getCurrentUser(): Response<UserDto>

    @PATCH("api/users/me/")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): Response<UserDto>

    @Multipart
    @POST("api/users/me/avatar/")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): Response<UserDto>

    @GET("api/users/me/favorites/")
    suspend fun getFavorites(@Query("page") page: Int = 1): Response<PaginatedFavoritesDto>

    @POST("api/venues/{id}/favorite/")
    suspend fun addFavorite(@Path("id") venueId: String): Response<JsonElement>

    @DELETE("api/venues/{id}/unfavorite/")
    suspend fun removeFavorite(@Path("id") venueId: String): Response<Unit>

    @POST("api/auth/verify-phone/")
    suspend fun requestPhoneVerification(@Body request: PhoneVerifyRequestDto): Response<PhoneVerifyResponseDto>

    @POST("api/auth/verify-phone-otp/")
    suspend fun verifyPhoneOtp(@Body request: PhoneOtpVerifyRequestDto): Response<JsonElement>

    @GET("api/users/me/points/")
    suspend fun getUserPoints(): Response<PointsDto>

    @GET("api/referrals/")
    suspend fun getReferrals(): Response<ReferralDto>
}
