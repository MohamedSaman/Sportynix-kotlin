package com.sportynix.app.domain.usecase.auth

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.repository.AuthRepository
import javax.inject.Inject

class CheckUsernameUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(username: String): ApiResult<Boolean> {
        if (username.length < 4) {
            return ApiResult.Success(false)
        }
        return repository.checkUsernameAvailability(username.trim().lowercase())
    }
}
