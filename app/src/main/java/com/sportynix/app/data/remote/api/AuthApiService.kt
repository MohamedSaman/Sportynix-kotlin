package com.sportynix.app.data.remote.api

import com.sportynix.app.data.remote.dto.AuthResponseDto
import com.sportynix.app.data.remote.dto.ForgotPasswordRequestDto
import com.sportynix.app.data.remote.dto.ForgotPasswordResponseDto
import com.sportynix.app.data.remote.dto.LoginRequestDto
import com.sportynix.app.data.remote.dto.RefreshTokenRequestDto
import com.sportynix.app.data.remote.dto.RefreshTokenResponseDto
import com.sportynix.app.data.remote.dto.ResendOtpRequestDto
import com.sportynix.app.data.remote.dto.ResetPasswordRequestDto
import com.sportynix.app.data.remote.dto.SignUpRequestDto
import com.sportynix.app.data.remote.dto.SignUpResponseDto
import com.sportynix.app.data.remote.dto.TokenVerifyRequestDto
import com.sportynix.app.data.remote.dto.UnreadCountsDto
import com.sportynix.app.data.remote.dto.UserDto
import com.sportynix.app.data.remote.dto.UsernameCheckResponseDto
import com.sportynix.app.data.remote.dto.VerifyOtpRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {

    @Headers("No-Auth: true")
    @GET("api/auth/username/check/")
    suspend fun checkUsernameAvailability(@Query("username") username: String): Response<UsernameCheckResponseDto>

    @Headers("No-Auth: true")
    @POST("api/auth/login/")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @Headers("No-Auth: true")
    @POST("api/auth/signup/")
    suspend fun signUp(@Body request: SignUpRequestDto): Response<SignUpResponseDto>

    @Headers("No-Auth: true")
    @POST("api/auth/verify-signup/")
    suspend fun verifySignUpOtp(@Body request: VerifyOtpRequestDto): Response<AuthResponseDto>

    @Headers("No-Auth: true")
    @POST("api/auth/resend-otp/")
    suspend fun resendOtp(@Body request: ResendOtpRequestDto): Response<SignUpResponseDto>

    @Headers("No-Auth: true")
    @POST("api/auth/forgot-password/")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto): Response<ForgotPasswordResponseDto>

    @Headers("No-Auth: true")
    @POST("api/auth/reset-password/")
    suspend fun resetPassword(@Body request: ResetPasswordRequestDto): Response<Unit>

    @Headers("No-Auth: true")
    @POST("api/token/refresh/")
    suspend fun refreshToken(@Body request: RefreshTokenRequestDto): Response<RefreshTokenResponseDto>

    @Headers("No-Auth: true")
    @POST("api/token/verify/")
    suspend fun verifyToken(@Body request: TokenVerifyRequestDto): Response<Unit>

    @GET("api/users/me/")
    suspend fun getCurrentUser(): Response<UserDto>

    @GET("api/notifications/unread_counts/")
    suspend fun getUnreadCounts(): Response<UnreadCountsDto>

    @POST("api/auth/logout/")
    suspend fun logout(): Response<Unit>
}
