package com.sportynix.app.domain.usecase.auth

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.User
import com.sportynix.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, pass: String): ApiResult<User> {
        if (email.isBlank()) return ApiResult.Error(message = "Email cannot be empty")
        if (pass.isBlank()) return ApiResult.Error(message = "Password cannot be empty")
        return repository.login(email.trim(), pass)
    }
}
