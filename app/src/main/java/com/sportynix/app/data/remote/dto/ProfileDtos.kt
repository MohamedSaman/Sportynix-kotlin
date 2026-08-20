package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserDataDto(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("email_verified_at") val emailVerifiedAt: String? = null,
    @SerializedName("phone_verified_at") val phoneVerifiedAt: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("date_of_birth") val dateOfBirth: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("must_verify_phone") val mustVerifyPhone: Boolean? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null,
    @SerializedName("points") val points: Int? = 0,
    @SerializedName("address") val address: String? = null,
    @SerializedName("home_district") val homeDistrict: String? = null,
    @SerializedName("home_city") val homeCity: String? = null,
    @SerializedName("sports_preferences") val sportsPreferences: List<String>? = emptyList(),
    @SerializedName("availability") val availability: String? = null,
    @SerializedName("is_public_profile") val isPublicProfile: Boolean? = true,
    @SerializedName("is_show_contact") val isShowContact: Boolean? = false,
    @SerializedName("referral_code") val referralCode: String? = null,
    @SerializedName("is_social") val isSocial: Boolean? = false,
    @SerializedName("has_password") val hasPassword: Boolean? = true,
    @SerializedName("cricket_profile") val cricketProfile: CricketProfileDataDto? = null,
    @SerializedName("username_changes_used") val usernameChangesUsed: Int? = 0,
    @SerializedName("username_changes_remaining") val usernameChangesRemaining: Int? = 3,
    @SerializedName("username_last_changed_at") val usernameLastChangedAt: String? = null,
    @SerializedName("username_next_change_at") val usernameNextChangeAt: String? = null,
    @SerializedName("username_change_cooldown_days_remaining") val usernameChangeCooldownDaysRemaining: Int? = 0,
    @SerializedName("can_change_username_now") val canChangeUsernameNow: Boolean? = true,
    @SerializedName("allow_direct_team_add") val allowDirectTeamAdd: Boolean? = false,
    @SerializedName("home_city_id") val homeCityId: Int? = null,
    @SerializedName("home_province_name") val homeProvinceName: String? = null
)

data class CricketProfileDataDto(
    @SerializedName("preferred_variant") val preferredVariant: String? = null,
    @SerializedName("primary_role") val primaryRole: String? = null,
    @SerializedName("playing_position") val playingPosition: String? = null,
    @SerializedName("batting_style") val battingStyle: String? = null,
    @SerializedName("bowling_style") val bowlingStyle: String? = null,
    @SerializedName("jersey_number") val jerseyNumber: Int? = null
)

data class PhoneOtpSendRequestDto(
    @SerializedName("phone_number") val phoneNumber: String
)

data class PhoneOtpSendResponseDto(
    @SerializedName("challenge_id") val challengeId: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
)

data class PhoneOtpVerifyDto(
    @SerializedName("challenge_id") val challengeId: Int,
    @SerializedName("otp_code") val otpCode: String
)

data class PhoneOtpVerifyResultDto(
    @SerializedName("user") val user: UserDataDto? = null
)

data class EmailChangeRequestDto(
    @SerializedName("new_email") val newEmail: String,
    @SerializedName("current_password") val currentPassword: String? = null
)

data class EmailVerifyNewRequestDto(
    @SerializedName("otp_code") val otpCode: String,
    @SerializedName("new_email") val newEmail: String
)

data class PasswordChangeRequestDto(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("confirm_password") val confirmPassword: String
)

data class APIFavoriteDto(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String? = "venue",
    @SerializedName("venue") val venue: FavoriteVenueDto? = null,
    @SerializedName("sport") val sport: FavoriteSportDto? = null
)

data class FavoriteVenueDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("rating") val rating: Double? = 0.0,
    @SerializedName("reviews") val reviews: Int? = 0,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_url_secure") val imageUrlSecure: String? = null
)

data class FavoriteSportDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("price") val price: String? = null,
    @SerializedName("average_rating") val averageRating: Double? = 0.0,
    @SerializedName("reviews_count") val reviewsCount: Int? = 0,
    @SerializedName("image") val image: String? = null,
    @SerializedName("image_secure") val imageSecure: String? = null,
    @SerializedName("venue") val venue: FavoriteVenueDto? = null
)

data class PointsHistoryResponseDto(
    @SerializedName("summary") val summary: PointsSummaryDto? = null,
    @SerializedName("results") val results: List<PointsHistoryItemDto> = emptyList()
)

data class PointsSummaryDto(
    @SerializedName("total_earned") val totalEarned: Int? = 0,
    @SerializedName("total_spent") val totalSpent: Int? = 0
)

data class PointsHistoryItemDto(
    @SerializedName("id") val id: Int,
    @SerializedName("amount") val amount: Int,
    @SerializedName("direction") val direction: String? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("reason_label") val reasonLabel: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ReferralResponseDto(
    @SerializedName("referral_code") val referralCode: String? = null,
    @SerializedName("referrals") val referrals: List<ReferralItemDto>? = emptyList(),
    @SerializedName("stats") val stats: ReferralStatsDto? = null
)

data class ReferralItemDto(
    @SerializedName("id") val id: Int,
    @SerializedName("referred_user_name") val referredUserName: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ReferralStatsDto(
    @SerializedName("total_points") val totalPoints: Int? = 0,
    @SerializedName("total_referrals") val totalReferrals: Int? = 0
)

data class LocationProvinceDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name_en") val nameEn: String
)

data class LocationDistrictDto(
    @SerializedName("id") val id: Int,
    @SerializedName("province_id") val provinceId: Int,
    @SerializedName("province_name") val provinceName: String,
    @SerializedName("name_en") val nameEn: String
)

data class LocationCityDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("district_id") val districtId: Int? = null,
    @SerializedName("district_name") val districtName: String? = null,
    @SerializedName("province_id") val provinceId: Int? = null,
    @SerializedName("province_name") val provinceName: String? = null,
    @SerializedName("name_en") val nameEn: String? = null
)

data class BlockedUserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

data class ReportItemDto(
    @SerializedName("id") val id: Int,
    @SerializedName("reported_user_name") val reportedUserName: String? = null,
    @SerializedName("reported_user_username") val reportedUserUsername: String? = null,
    @SerializedName("reported_user_profile_picture") val reportedUserProfilePicture: String? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("reviewed_note") val reviewedNote: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("can_cancel") val canCancel: Boolean? = false
)

data class UpdateAllowDirectTeamAddRequestDto(
    @SerializedName("allow_direct_team_add") val allowDirectTeamAdd: Boolean
)
