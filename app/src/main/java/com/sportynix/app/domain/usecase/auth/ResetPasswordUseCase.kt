package com.sportynix.app.domain.usecase.auth

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.repository.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        otpCode: String,
        newPass: String,
        confirmPass: String
    ): ApiResult<Unit> {
        if (newPass.length < 8) {
            return ApiResult.Error(message = "Password must be at least 8 characters long")
        }
        if (!Regex("(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)").containsMatchIn(newPass)) {
            return ApiResult.Error(message = "Password must contain at least one uppercase letter, one lowercase letter, and one number")
        }
        if (newPass != confirmPass) {
            return ApiResult.Error(message = "Passwords do not match")
        }
        return repository.resetPassword(email.trim(), otpCode.trim(), newPass, confirmPass)
    }
}
