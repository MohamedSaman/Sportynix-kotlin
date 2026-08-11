package com.sportynix.app.domain.auth

import java.time.LocalDate
import java.time.Period

object AuthValidators {
    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val usernameRegex = Regex("^[a-z0-9_-]{4,30}$")
    private val phoneRegex = Regex("^[0-9]{10}$")
    private val nameRegex = Regex("^[a-zA-Z\\s]+$")
    private val referralRegex = Regex("^[A-Z0-9]{3,20}$")

    fun email(value: String): String? = when {
        value.isBlank() -> "Email is required"
        !emailRegex.matches(value.trim()) -> "Please enter a valid email address"
        else -> null
    }

    fun name(label: String, value: String): String? = when {
        value.isBlank() -> "$label is required"
        value.trim().length < 2 -> "$label must be at least 2 characters"
        !nameRegex.matches(value.trim()) -> "$label can only contain letters and spaces"
        else -> null
    }

    fun phone(value: String): String? = when {
        value.isBlank() -> "Phone number is required"
        !phoneRegex.matches(value.trim()) -> "Phone number must contain exactly 10 digits"
        else -> null
    }

    fun username(value: String): String? = if (usernameRegex.matches(value.trim().lowercase())) null
        else "Username must be 4-30 chars (lowercase letters, numbers, - or _)"

    fun dateOfBirth(value: String, today: LocalDate = LocalDate.now()): String? {
        val date = runCatching { LocalDate.parse(value.trim()) }.getOrNull()
            ?: return "Please enter a valid date of birth"
        if (date.isAfter(today)) return "Date of birth cannot be in the future"
        if (Period.between(date, today).years < 13) return "You must be at least 13 years old to sign up"
        return null
    }

    fun password(value: String): String? = when {
        value.isEmpty() -> "Password is required"
        value.length < 8 -> "Password must be at least 8 characters"
        !Regex("(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)").containsMatchIn(value) ->
            "Password must contain at least one uppercase letter, one lowercase letter, and one number"
        else -> null
    }

    fun referral(value: String): String? = if (value.isBlank() || referralRegex.matches(value.trim().uppercase())) null
        else "Referral code must be 3-20 alphanumeric characters"

    fun otp(value: String): String = value.filter(Char::isDigit).take(6)
}
