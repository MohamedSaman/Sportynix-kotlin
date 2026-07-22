package com.sportynix.app.domain.model

data class User(
    val id: String,
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val name: String,
    val email: String,
    val phone: String = "",
    val avatarUrl: String? = null,
    val bio: String? = null,
    val points: Int = 0,
    val role: String = "USER",
    val isEmailVerified: Boolean = false,
    val isPhoneVerified: Boolean = false,
    val mustVerifyPhone: Boolean = false
)
