package com.sportynix.app.domain.usecase.auth

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.repository.AuthRepository
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): ApiResult<String> {
        if (email.isBlank()) {
            return ApiResult.Error(message = "Please enter your email address")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return ApiResult.Error(message = "Please enter a valid email address")
        }
        return repository.forgotPassword(email.trim())
    }
}
