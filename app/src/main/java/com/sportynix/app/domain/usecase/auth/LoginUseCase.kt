package com.sportynix.app.domain.usecase.auth

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.User
import com.sportynix.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, pass: String): ApiResult<User> {
        if (email.isBlank()) return ApiResult.Error(message = "Please enter your email, phone, or username")
        if (pass.isBlank()) return ApiResult.Error(message = "Please enter your password")
        return repository.login(email.trim().lowercase(), pass.trim())
    }
}
