package com.sportynix.app.domain.usecase.auth

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.User
import com.sportynix.app.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend fun verifySignUpOtp(sessionId: String, otpCode: String): ApiResult<User> {
        if (otpCode.length != 6) {
            return ApiResult.Error(message = "Please enter a valid 6-digit OTP code")
        }
        return repository.verifySignUpOtp(sessionId, otpCode)
    }

    suspend fun resendOtp(sessionId: String): ApiResult<String> {
        return repository.resendOtp(sessionId)
    }
}
