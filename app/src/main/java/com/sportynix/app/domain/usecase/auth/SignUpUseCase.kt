package com.sportynix.app.domain.usecase.auth

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        dob: String,
        pass: String,
        referralCode: String?
    ): ApiResult<String> {
        if (firstName.isBlank()) return ApiResult.Error(message = "First name is required")
        if (lastName.isBlank()) return ApiResult.Error(message = "Last name is required")
        if (email.isBlank()) return ApiResult.Error(message = "Email is required")
        if (phone.isBlank()) return ApiResult.Error(message = "Phone number is required")
        if (dob.isBlank()) return ApiResult.Error(message = "Date of birth is required")
        if (username.isBlank()) return ApiResult.Error(message = "Username is required")
        if (pass.length < 8) return ApiResult.Error(message = "Password must be at least 8 characters long")

        return repository.signUp(
            username.trim().lowercase(),
            firstName.trim(),
            lastName.trim(),
            email.trim(),
            phone.trim(),
            dob.trim(),
            pass,
            referralCode?.trim()?.takeIf { it.isNotEmpty() }
        )
    }
}
