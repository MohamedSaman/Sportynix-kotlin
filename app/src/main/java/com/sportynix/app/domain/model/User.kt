package com.sportynix.app.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val avatarUrl: String? = null,
    val role: String = "USER"
)
