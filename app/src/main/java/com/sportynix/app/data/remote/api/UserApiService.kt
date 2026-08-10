package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.sportynix.app.data.remote.dto.APIFavoriteDto
import com.sportynix.app.data.remote.dto.BlockedUserDto
import com.sportynix.app.data.remote.dto.EmailChangeRequestDto
import com.sportynix.app.data.remote.dto.EmailVerifyNewRequestDto
import com.sportynix.app.data.remote.dto.LocationCityDto
import com.sportynix.app.data.remote.dto.LocationDistrictDto
import com.sportynix.app.data.remote.dto.LocationProvinceDto
import com.sportynix.app.data.remote.dto.PasswordChangeRequestDto
import com.sportynix.app.data.remote.dto.PhoneOtpSendRequestDto
import com.sportynix.app.data.remote.dto.PhoneOtpSendResponseDto
import com.sportynix.app.data.remote.dto.PhoneOtpVerifyDto
import com.sportynix.app.data.remote.dto.PhoneOtpVerifyResultDto
import com.sportynix.app.data.remote.dto.PointsHistoryResponseDto
import com.sportynix.app.data.remote.dto.ReferralResponseDto
import com.sportynix.app.data.remote.dto.ReportItemDto
import com.sportynix.app.data.remote.dto.UpdateAllowDirectTeamAddRequestDto
import com.sportynix.app.data.remote.dto.UserDataDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApiService {

    @GET("api/auth/profile/")
    suspend fun getProfile(): Response<UserDataDto>

    @PATCH("api/auth/profile/")
    suspend fun updateProfileJson(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<UserDataDto>

    @Multipart
    @PATCH("api/auth/profile/")
    suspend fun updateProfileMultipart(
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part profilePicture: MultipartBody.Part? = null
    ): Response<UserDataDto>

    @PATCH("api/auth/profile/")
    suspend fun updateAllowDirectTeamAdd(@Body request: UpdateAllowDirectTeamAddRequestDto): Response<UserDataDto>

    @POST("api/auth/phone/send-otp/")
    suspend fun sendPhoneOtp(@Body request: PhoneOtpSendRequestDto): Response<PhoneOtpSendResponseDto>

    @POST("api/auth/phone/verify-otp/")
    suspend fun verifyPhoneOtp(@Body request: PhoneOtpVerifyDto): Response<PhoneOtpVerifyResultDto>

    @POST("api/auth/email/resend-verification/")
    suspend fun resendEmailVerificationLink(): Response<JsonElement>

    @POST("api/auth/email/change-request/")
    suspend fun requestEmailChange(@Body request: EmailChangeRequestDto): Response<JsonElement>

    @POST("api/auth/email/send-change-otp/")
    suspend fun sendEmailChangeOtp(): Response<JsonElement>

    @POST("api/auth/email/verify-change/")
    suspend fun verifyCurrentEmailForChange(@Body body: Map<String, String>): Response<JsonElement>

    @POST("api/auth/email/verify-new/")
    suspend fun verifyNewEmail(@Body request: EmailVerifyNewRequestDto): Response<JsonElement>

    @POST("api/auth/password/change/")
    suspend fun changePassword(@Body request: PasswordChangeRequestDto): Response<JsonElement>

    @POST("api/auth/password/change/")
    suspend fun changePasswordRaw(@Body body: Map<String, String>): Response<JsonElement>

    @POST("api/auth/password/set/")
    suspend fun setPassword(@Body body: Map<String, String>): Response<JsonElement>

    @POST("api/auth/delete-account/")
    suspend fun deleteAccount(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<JsonElement>

    @POST("api/support/bug-report/")
    suspend fun submitBugReport(@Body body: Map<String, String>): Response<JsonElement>

    @GET("api/favorites/")
    suspend fun getFavorites(): Response<JsonElement>

    @POST("api/favorites/")
    suspend fun addFavorite(@Body body: Map<String, String>): Response<APIFavoriteDto>

    @DELETE("api/favorites/{id}/")
    suspend fun removeFavorite(@Path("id") favoriteId: Int): Response<Unit>

    @GET("api/auth/points/history/")
    suspend fun getPointsHistory(@Query("limit") limit: Int = 100): Response<PointsHistoryResponseDto>

    @GET("api/referrals/my_referrals/")
    suspend fun getReferrals(): Response<ReferralResponseDto>

    @GET("api/locations/provinces/")
    suspend fun getLocationProvinces(): Response<List<LocationProvinceDto>>

    @GET("api/locations/districts/")
    suspend fun getLocationDistricts(@Query("province_id") provinceId: Int): Response<List<LocationDistrictDto>>

    @GET("api/locations/cities/")
    suspend fun getLocationCities(
        @Query("district_id") districtId: Int? = null,
        @Query("search") search: String? = null,
        @Query("page_size") pageSize: Int = 50
    ): Response<List<LocationCityDto>>

    @GET("api/users/blocked/")
    suspend fun getBlockedUsers(): Response<List<BlockedUserDto>>

    @POST("api/users/unblock/")
    suspend fun unblockUser(@Body body: Map<String, Int>): Response<JsonElement>

    @GET("api/reports/my_reports/")
    suspend fun getReports(): Response<List<ReportItemDto>>

    @DELETE("api/reports/{id}/cancel/")
    suspend fun cancelReport(@Path("id") reportId: Int): Response<Unit>
}
