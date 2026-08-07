package com.sportynix.app.domain.usecase.auth

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.repository.AuthRepository
import javax.inject.Inject
import java.time.LocalDate
import java.time.Period
import com.sportynix.app.domain.auth.AuthValidators

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
        val cleanFirstName = firstName.trim()
        val cleanLastName = lastName.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val cleanUsername = username.trim().lowercase()
        val cleanReferral = referralCode?.trim()?.uppercase().orEmpty()
        AuthValidators.name("First name", cleanFirstName)?.let { return ApiResult.Error(message = it) }
        AuthValidators.name("Last name", cleanLastName)?.let { return ApiResult.Error(message = it) }
        AuthValidators.email(cleanEmail)?.let { return ApiResult.Error(message = it) }
        AuthValidators.phone(cleanPhone)?.let { return ApiResult.Error(message = it) }
        AuthValidators.dateOfBirth(dob)?.let { return ApiResult.Error(message = it) }
        AuthValidators.username(cleanUsername)?.let { return ApiResult.Error(message = it) }
        AuthValidators.password(pass)?.let { return ApiResult.Error(message = it) }
        AuthValidators.referral(cleanReferral)?.let { return ApiResult.Error(message = it) }

        return repository.signUp(
            cleanUsername,
            cleanFirstName,
            cleanLastName,
            cleanEmail,
            cleanPhone,
            dob.trim(),
            pass,
            cleanReferral.takeIf { it.isNotEmpty() }
        )
    }
}
