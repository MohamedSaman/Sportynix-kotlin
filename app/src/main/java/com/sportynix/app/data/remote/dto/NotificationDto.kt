package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String?,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("time_ago") val timeAgo: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("data") val data: Map<String, Any>? = null
)

data class PaginatedNotificationsDto(
    @SerializedName("count") val count: Int?,
    @SerializedName("next") val next: String?,
    @SerializedName("previous") val previous: String?,
    @SerializedName("results") val results: List<NotificationDto>?,
    @SerializedName("unread_count") val unreadCount: Int?
)

data class MarkReadResponseDto(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?
)

data class UpdateProfileRequestDto(
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("location") val location: String? = null
)

data class PhoneVerifyRequestDto(
    @SerializedName("phone_number") val phoneNumber: String
)

data class PhoneVerifyResponseDto(
    @SerializedName("challenge_id") val challengeId: Int?,
    @SerializedName("session_id") val sessionId: String?,
    @SerializedName("message") val message: String?
)

data class PhoneOtpVerifyRequestDto(
    @SerializedName("challenge_id") val challengeId: Int,
    @SerializedName("otp_code") val otpCode: String
)

data class FavoriteVenueDto(
    @SerializedName("id") val id: String,
    @SerializedName("venue") val venue: VenueDto
)

data class PaginatedFavoritesDto(
    @SerializedName("count") val count: Int?,
    @SerializedName("next") val next: String?,
    @SerializedName("results") val results: List<FavoriteVenueDto>?
)

data class PointsDto(
    @SerializedName("total_points") val totalPoints: Int = 0,
    @SerializedName("points_history") val pointsHistory: List<PointHistoryDto>? = null
)

data class PointHistoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("points") val points: Int,
    @SerializedName("reason") val reason: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class ReferralDto(
    @SerializedName("referral_code") val referralCode: String?,
    @SerializedName("total_referrals") val totalReferrals: Int = 0,
    @SerializedName("referral_bonus") val referralBonus: Int = 0,
    @SerializedName("referrals") val referrals: List<ReferralItemDto>? = null
)

data class ReferralItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("referee_username") val refereeUsername: String?,
    @SerializedName("bonus_points") val bonusPoints: Int = 0,
    @SerializedName("created_at") val createdAt: String?
)

data class ChatConversationDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("chat_type") val chatType: String?,
    @SerializedName("last_message") val lastMessage: ChatMessageDto?,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("participants") val participants: List<UserDto>? = null,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class ChatMessageDto(
    @SerializedName("id") val id: String,
    @SerializedName("content") val content: String?,
    @SerializedName("sender") val sender: UserDto?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("message_type") val messageType: String? = "text",
    @SerializedName("is_read") val isRead: Boolean = false
)

data class PaginatedMessagesDto(
    @SerializedName("count") val count: Int?,
    @SerializedName("next") val next: String?,
    @SerializedName("results") val results: List<ChatMessageDto>?
)

data class SendMessageRequestDto(
    @SerializedName("content") val content: String,
    @SerializedName("message_type") val messageType: String = "text"
)
