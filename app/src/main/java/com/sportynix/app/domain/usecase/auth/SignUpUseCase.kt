package com.sportynix.app.domain.usecase.auth

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.repository.AuthRepository
import javax.inject.Inject
import java.time.LocalDate
import java.time.Period

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
        if (cleanFirstName.isEmpty()) return ApiResult.Error(message = "First name is required")
        if (cleanFirstName.length < 2) return ApiResult.Error(message = "First name must be at least 2 characters")
        if (!cleanFirstName.matches(Regex("^[a-zA-Z\\s]+$"))) return ApiResult.Error(message = "First name can only contain letters and spaces")
        if (cleanLastName.isEmpty()) return ApiResult.Error(message = "Last name is required")
        if (cleanLastName.length < 2) return ApiResult.Error(message = "Last name must be at least 2 characters")
        if (!cleanLastName.matches(Regex("^[a-zA-Z\\s]+$"))) return ApiResult.Error(message = "Last name can only contain letters and spaces")
        if (!cleanEmail.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) return ApiResult.Error(message = "Please enter a valid email address")
        if (!cleanPhone.matches(Regex("^\\+?[0-9]{8,20}$"))) return ApiResult.Error(message = "Please enter a valid phone number")
        val birthDate = runCatching { LocalDate.parse(dob.trim()) }.getOrNull()
            ?: return ApiResult.Error(message = "Please enter a valid date of birth")
        if (Period.between(birthDate, LocalDate.now()).years < 13) return ApiResult.Error(message = "You must be at least 13 years old to sign up")
        if (!cleanUsername.matches(Regex("^[a-z0-9_-]{4,30}$"))) return ApiResult.Error(message = "Username must be 4-30 chars (lowercase letters, numbers, - or _)")
        if (pass.length < 8) return ApiResult.Error(message = "Password must be at least 8 characters")
        if (!pass.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$"))) return ApiResult.Error(message = "Password must contain uppercase, lowercase, and a number")
        if (cleanReferral.isNotEmpty() && !cleanReferral.matches(Regex("^[A-Z0-9]{3,20}$"))) return ApiResult.Error(message = "Referral code must be 3-20 alphanumeric characters")

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
