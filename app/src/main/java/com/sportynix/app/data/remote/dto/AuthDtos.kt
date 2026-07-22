package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName(value = "username_or_email", alternate = ["email", "username"]) val usernameOrEmail: String,
    @SerializedName("password") val pass: String
)

data class UsernameCheckResponseDto(
    @SerializedName("available") val available: Boolean = false,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("message") val message: String? = null
)

data class SignUpRequestDto(
    @SerializedName("username") val username: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("date_of_birth") val dateOfBirth: String,
    @SerializedName("password") val pass: String,
    @SerializedName("terms_accepted") val termsAccepted: Boolean = true,
    @SerializedName("referral_code") val referralCode: String? = null
)

data class SignUpResponseDto(
    @SerializedName("session_id") val sessionId: String?,
    @SerializedName("message") val message: String?
)

data class VerifyOtpRequestDto(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("otp_code") val otpCode: String
)

data class ResendOtpRequestDto(
    @SerializedName("session_id") val sessionId: String
)

data class ForgotPasswordRequestDto(
    @SerializedName("email") val email: String
)

data class ForgotPasswordResponseDto(
    @SerializedName("session_id") val sessionId: String?,
    @SerializedName("message") val message: String?
)

data class ResetPasswordRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("otp_code") val otpCode: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("confirm_password") val confirmPassword: String
)

data class RefreshTokenRequestDto(
    @SerializedName("refresh") val refresh: String
)

data class TokenVerifyRequestDto(
    @SerializedName("token") val token: String
)

data class AuthResponseDto(
    @SerializedName(value = "access", alternate = ["accessToken", "access_token"]) val accessToken: String?,
    @SerializedName(value = "refresh", alternate = ["refreshToken", "refresh_token"]) val refreshToken: String?,
    @SerializedName("user") val user: UserDto?,
    @SerializedName("tokens") val tokens: AuthTokensDto? = null
)

data class AuthTokensDto(
    @SerializedName("access") val access: String?,
    @SerializedName("refresh") val refresh: String?
)

data class RefreshTokenResponseDto(
    @SerializedName(value = "access", alternate = ["accessToken", "access_token"]) val access: String
)

data class UserDto(
    @SerializedName(value = "id") val id: String,
    @SerializedName(value = "username", alternate = ["name", "full_name"]) val username: String?,
    @SerializedName(value = "first_name") val firstName: String?,
    @SerializedName(value = "last_name") val lastName: String?,
    @SerializedName("email") val email: String,
    @SerializedName(value = "phone_number", alternate = ["phone"]) val phone: String?,
    @SerializedName(value = "profile_picture", alternate = ["avatarUrl", "avatar"]) val avatarUrl: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("points") val points: Int? = 0,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("email_verified_at") val emailVerifiedAt: String? = null,
    @SerializedName("phone_verified_at") val phoneVerifiedAt: String? = null,
    @SerializedName("is_phone_verified") val isPhoneVerified: Boolean? = null,
    @SerializedName("must_verify_phone") val mustVerifyPhone: Boolean? = null
)

data class UnreadCountsDto(
    @SerializedName("notifications") val notifications: Int = 0,
    @SerializedName("messages") val messages: Int = 0
)
