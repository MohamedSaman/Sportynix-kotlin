package com.sportynix.app.domain.model

data class ForgotPasswordResult(
    val sessionId: String,
    val message: String? = null,
    val isSocialUser: Boolean = false,
    val canSetPassword: Boolean = false
)
