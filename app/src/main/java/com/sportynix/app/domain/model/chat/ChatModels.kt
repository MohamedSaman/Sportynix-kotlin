package com.sportynix.app.domain.model.chat

import com.google.gson.annotations.SerializedName

data class Chat(
    val id: Long,
    @SerializedName("chat_type") val chatType: String,
    val name: String?,
    @SerializedName("display_name") val displayName: String?,
    val description: String?,
    @SerializedName("team_name") val teamName: String?,
    @SerializedName("team_logo") val teamLogo: String?,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("last_message_time") val lastMessageTime: String?,
    @SerializedName("follower_count") val followerCount: Int? = 0,
    @SerializedName("is_following") val isFollowing: Boolean? = false,
    @SerializedName("is_public") val isPublic: Boolean? = false,
    @SerializedName("other_user_name") val otherUserName: String?,
    @SerializedName("other_user_id") val otherUserId: Long?,
    @SerializedName("other_user_avatar") val otherUserAvatar: String?,
    @SerializedName("is_admin") val isAdmin: Boolean? = false,
    @SerializedName("admin_only") val adminOnly: Boolean? = false,
    @SerializedName("can_post") val canPost: Boolean? = true,
    @SerializedName("can_manage") val canManage: Boolean? = false,
    @SerializedName("members_count") val membersCount: Int? = 0,
    @SerializedName("is_blocked") val isBlocked: Boolean? = false,
    @SerializedName("blocked_by_me") val blockedByMe: Boolean? = false,
    @SerializedName("blocked_me") val blockedMe: Boolean? = false,
    @SerializedName("blocked_user_id") val blockedUserId: Long?,
    @SerializedName("blocked_user_name") val blockedUserName: String?,
    @SerializedName("block_status_message") val blockStatusMessage: String?
)

data class SendMessageResponse(
    val id: Long,
    val chat: Long,
    val message: String,
    @SerializedName("message_type") val messageType: String,
    val sender: Long,
    @SerializedName("sender_name") val senderName: String,
    val timestamp: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_pinned") val isPinned: Boolean? = false,
    val duration: Int?,
    val metadata: Map<String, Any>?,
    @SerializedName("booking_id") val bookingId: Long?,
    val delivered: Boolean? = false,
    val queued: Boolean? = false,
    @SerializedName("client_msg_id") val clientMsgId: String?
)
