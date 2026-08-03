package com.sportynix.app.data.repository

import com.sportynix.app.core.datastore.SessionManager
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.remote.api.AuthApiService
import com.sportynix.app.data.remote.dto.ForgotPasswordRequestDto
import com.sportynix.app.data.remote.dto.LoginRequestDto
import com.sportynix.app.data.remote.dto.ResendOtpRequestDto
import com.sportynix.app.data.remote.dto.ResetPasswordRequestDto
import com.sportynix.app.data.remote.dto.SignUpRequestDto
import com.sportynix.app.data.remote.dto.VerifyOtpRequestDto
import com.sportynix.app.data.remote.dto.VerifyPasswordResetOtpRequestDto
import com.sportynix.app.domain.model.User
import com.sportynix.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun checkUsernameAvailability(username: String): ApiResult<Boolean> {
        return try {
            val response = apiService.checkUsernameAvailability(username)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.available)
            } else {
                ApiResult.Success(false)
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Could not verify username availability")
        }
    }

    override suspend fun login(usernameOrEmail: String, pass: String): ApiResult<User> {
        return try {
            val response = apiService.login(LoginRequestDto(usernameOrEmail, pass))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val token = body.accessToken ?: body.tokens?.access ?: ""
                val refreshToken = body.refreshToken ?: body.tokens?.refresh ?: ""
                val userDomain = body.user?.toDomain() ?: User(id = "1", name = usernameOrEmail, email = usernameOrEmail)

                sessionManager.saveSession(
                    accessToken = token,
                    refreshToken = refreshToken,
                    userId = userDomain.id,
                    email = userDomain.email,
                    name = userDomain.name
                )
                ApiResult.Success(userDomain)
            } else if (response.code() == 401) {
                ApiResult.Unauthorized
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Login failed. Please check credentials.")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network connection error")
        }
    }

    override suspend fun signUp(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        dob: String,
        pass: String,
        referralCode: String?
    ): ApiResult<String> {
        return try {
            val request = SignUpRequestDto(
                username = username,
                firstName = firstName,
                lastName = lastName,
                email = email,
                phoneNumber = phone,
                dateOfBirth = dob,
                pass = pass,
                termsAccepted = true,
                referralCode = referralCode
            )
            val response = apiService.signUp(request)
            if (response.isSuccessful && response.body() != null) {
                val sessionId = response.body()!!.sessionId ?: ""
                ApiResult.Success(sessionId)
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Registration failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error during sign up")
        }
    }

    override suspend fun verifySignUpOtp(sessionId: String, otpCode: String): ApiResult<User> {
        return try {
            val response = apiService.verifySignUpOtp(VerifyOtpRequestDto(sessionId, otpCode))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val token = body.accessToken ?: body.tokens?.access ?: ""
                val refreshToken = body.refreshToken ?: body.tokens?.refresh ?: ""
                val userDomain = body.user?.toDomain() ?: User(id = "1", name = "User", email = "")

                sessionManager.saveSession(
                    accessToken = token,
                    refreshToken = refreshToken,
                    userId = userDomain.id,
                    email = userDomain.email,
                    name = userDomain.name
                )
                ApiResult.Success(userDomain)
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "OTP Verification failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error during OTP verification")
        }
    }

    override suspend fun resendOtp(sessionId: String): ApiResult<String> {
        return try {
            val response = apiService.resendOtp(ResendOtpRequestDto(sessionId))
            if (response.isSuccessful && response.body() != null) {
                val newSessionId = response.body()!!.sessionId ?: sessionId
                ApiResult.Success(newSessionId)
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Resend OTP failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Failed to resend OTP")
        }
    }

    override suspend fun forgotPassword(email: String): ApiResult<String> {
        return try {
            val response = apiService.forgotPassword(ForgotPasswordRequestDto(email))
            if (response.isSuccessful && response.body() != null) {
                val sessionId = response.body()!!.sessionId ?: ""
                ApiResult.Success(sessionId)
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Forgot password request failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error")
        }
    }

    override suspend fun verifyPasswordResetOtp(email: String, otpCode: String): ApiResult<Unit> {
        return try {
            val response = apiService.verifyPasswordResetOtp(VerifyPasswordResetOtpRequestDto(email, otpCode))
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.ServerError(response.code(), response.message().ifBlank { "Invalid or expired verification code" })
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error verifying code")
        }
    }

    override suspend fun resetPassword(
        email: String,
        otpCode: String,
        newPass: String,
        confirmPass: String
    ): ApiResult<Unit> {
        return try {
            val request = ResetPasswordRequestDto(email, otpCode, newPass, confirmPass)
            val response = apiService.resetPassword(request)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Reset password failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error during password reset")
        }
    }

    override suspend fun logout(): ApiResult<Unit> {
        try {
            apiService.logout()
        } catch (_: Exception) {}
        sessionManager.clearSession()
        return ApiResult.Success(Unit)
    }

    override fun isLoggedIn(): Flow<Boolean> = sessionManager.isLoggedIn

    override suspend fun getCurrentUser(): ApiResult<User> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                val userDomain = response.body()!!.toDomain()
                sessionManager.saveSession(
                    accessToken = sessionManager.accessToken.firstOrNull() ?: "",
                    refreshToken = sessionManager.refreshToken.firstOrNull() ?: "",
                    userId = userDomain.id,
                    email = userDomain.email,
                    name = userDomain.name
                )
                ApiResult.Success(userDomain)
            } else if (response.code() == 401) {
                ApiResult.Unauthorized
            } else {
                val cachedEmail = sessionManager.userEmail.firstOrNull()
                val cachedName = sessionManager.userName.firstOrNull()
                val cachedId = sessionManager.userId.firstOrNull()
                if (!cachedEmail.isNullOrEmpty()) {
                    ApiResult.Success(User(id = cachedId ?: "", name = cachedName ?: "", email = cachedEmail))
                } else {
                    ApiResult.ServerError(response.code(), response.message() ?: "Failed to load user profile")
                }
            }
        } catch (e: Exception) {
            val cachedEmail = sessionManager.userEmail.firstOrNull()
            val cachedName = sessionManager.userName.firstOrNull()
            val cachedId = sessionManager.userId.firstOrNull()
            if (!cachedEmail.isNullOrEmpty()) {
                ApiResult.Success(User(id = cachedId ?: "", name = cachedName ?: "", email = cachedEmail))
            } else {
                ApiResult.Error(message = e.localizedMessage ?: "Network error loading profile")
            }
        }
    }
}
