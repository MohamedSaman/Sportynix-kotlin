package com.sportynix.app.domain.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.User
import com.sportynix.app.domain.model.ForgotPasswordResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun checkUsernameAvailability(username: String): ApiResult<Boolean>
    suspend fun login(usernameOrEmail: String, pass: String): ApiResult<User>
    suspend fun signUp(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        dob: String,
        pass: String,
        referralCode: String?
    ): ApiResult<String> // Returns sessionId
    suspend fun verifySignUpOtp(sessionId: String, otpCode: String): ApiResult<User>
    suspend fun resendOtp(sessionId: String): ApiResult<String>
    suspend fun forgotPassword(email: String): ApiResult<ForgotPasswordResult>
    suspend fun verifyPasswordResetOtp(email: String, otpCode: String): ApiResult<Unit>
    suspend fun resetPassword(email: String, otpCode: String, newPass: String, confirmPass: String): ApiResult<Unit>
    suspend fun verifyEmailOtp(email: String, code: String): ApiResult<Unit>
    suspend fun resendEmailOtp(email: String): ApiResult<Unit>
    suspend fun googleAuth(idToken: String, dateOfBirth: String? = null, termsAccepted: Boolean? = null): ApiResult<User>
    suspend fun logout(): ApiResult<Unit>
    fun isLoggedIn(): Flow<Boolean>
    suspend fun getCurrentUser(): ApiResult<User>
}
