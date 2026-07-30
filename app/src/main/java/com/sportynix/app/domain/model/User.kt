package com.sportynix.app.domain.model

data class User(
    val id: String,
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val name: String = "",
    val email: String = "",
    val emailVerifiedAt: String? = null,
    val phoneVerifiedAt: String? = null,
    val phone: String = "",
    val mustVerifyPhone: Boolean = false,
    val gender: String = "prefer_not_to_say",
    val dateOfBirth: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val points: Int = 0,
    val role: String = "USER",
    val address: String = "",
    val homeDistrict: String = "",
    val homeCity: String = "",
    val homeCityId: Int? = null,
    val homeProvinceName: String = "",
    val sportsPreferences: List<String> = emptyList(),
    val availability: String = "both",
    val isPublicProfile: Boolean = true,
    val isShowContact: Boolean = false,
    val referralCode: String = "",
    val isSocial: Boolean = false,
    val hasPassword: Boolean = true,
    val cricketPreferredVariant: String = "all",
    val cricketPrimaryRole: String = "",
    val cricketPlayingPosition: String = "",
    val cricketBattingStyle: String = "",
    val cricketBowlingStyle: String = "",
    val cricketJerseyNumber: String = "",
    val usernameChangesUsed: Int = 0,
    val usernameChangesRemaining: Int = 3,
    val usernameLastChangedAt: String? = null,
    val usernameNextChangeAt: String? = null,
    val usernameChangeCooldownDaysRemaining: Int = 0,
    val canChangeUsernameNow: Boolean = true,
    val allowDirectTeamAdd: Boolean = false
) {
    val displayName: String
        get() {
            if (name.isNotBlank()) return name
            val full = "$firstName $lastName".trim()
            return if (full.isBlank()) "User" else full
        }

    val isEmailUnverified: Boolean
        get() = email.isNotBlank() && emailVerifiedAt == null

    val isPhoneVerified: Boolean
        get() = phoneVerifiedAt != null || !mustVerifyPhone

    val isFullyVerified: Boolean
        get() = emailVerifiedAt != null && isPhoneVerified
}
