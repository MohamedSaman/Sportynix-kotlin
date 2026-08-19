package com.sportynix.app.domain.model

import com.google.gson.annotations.SerializedName

data class LastMessage(
    val message: String? = null,
    @SerializedName("sender_name") val senderName: String? = null,
    @SerializedName("sender_id") val senderId: Long? = null,
    @SerializedName("message_type") val messageType: String = "text",
    @SerializedName("created_at") val createdAt: String? = null,
    val duration: Int? = null,
    val metadata: Map<String, Any>? = null,
    @SerializedName("booking_id") val bookingId: Long? = null
)

data class TeamSimple(
    val id: Long,
    val name: String,
    val logo: String? = null,
    val admin: TeamAdminSimple? = null
)

data class TeamAdminSimple(
    val id: Long
)

data class Chat(
    val id: Long,
    @SerializedName("chat_type") val chatType: String = "direct", // team_group, team_channel, direct, rivalry, challenge
    val team: TeamSimple? = null,
    @SerializedName("team_name") val teamName: String? = null,
    @SerializedName(value = "team_logo", alternate = ["team_logo_secure", "logo_secure"]) val teamLogo: String? = null,
    val name: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    val description: String? = null,
    @SerializedName("last_message") val lastMessage: LastMessage? = null,
    @SerializedName("last_message_text") val lastMessageText: String? = null,
    @SerializedName("last_message_time") val lastMessageTime: String? = null,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val participants: List<Long>? = null,
    @SerializedName("follower_count") val followerCount: Int? = null,
    @SerializedName("is_following") val isFollowing: Boolean? = null,
    @SerializedName("is_public") val isPublic: Boolean? = null,
    @SerializedName("pinned_messages") val pinnedMessages: List<ChatMessage>? = null,
    @SerializedName("other_user_name") val otherUserName: String? = null,
    @SerializedName("other_user_id") val otherUserId: Long? = null,
    @SerializedName(value = "other_user_avatar", alternate = ["other_user_avatar_secure", "profile_picture", "profile_picture_secure", "avatar", "avatar_url"]) val otherUserAvatar: String? = null,
    @SerializedName("other_user_online") val otherUserOnline: Boolean? = null,
    @SerializedName("other_user_last_seen") val otherUserLastSeen: String? = null,
    @SerializedName("is_admin") val isAdmin: Boolean? = null,
    @SerializedName("admin_only") val adminOnly: Boolean? = null,
    @SerializedName("can_post") val canPost: Boolean? = null,
    @SerializedName("can_manage") val canManage: Boolean? = null,
    @SerializedName("created_by") val createdBy: Long? = null,
    @SerializedName("admin_list") val adminList: List<Long>? = null,
    @SerializedName("members_count") val membersCount: Int? = null,
    @SerializedName("is_blocked") val isBlocked: Boolean? = null,
    @SerializedName("blocked_by_me") val blockedByMe: Boolean? = null,
    @SerializedName("blocked_me") val blockedMe: Boolean? = null,
    @SerializedName("blocked_user_id") val blockedUserId: Long? = null,
    @SerializedName("blocked_user_name") val blockedUserName: String? = null,
    @SerializedName("block_status_message") val blockStatusMessage: String? = null,
    @SerializedName("challenge_info") val challengeInfo: ChallengeInfo? = null,
    @SerializedName("rivalry_info") val rivalryInfo: RivalryInfo? = null,
    @SerializedName("team_id") val teamId: Long? = null
)

data class ChatMember(
    val id: Long,
    @SerializedName("full_name") val fullName: String,
    val email: String? = null,
    val avatar: String? = null,
    val role: String = "Member", // Owner, Admin, Member, Follower
    @SerializedName("is_admin") val isAdmin: Boolean = false
)

data class ChatMessage(
    val id: Long = 0,
    val chat: Long = 0,
    val sender: Long = 0,
    @SerializedName("sender_name") val senderName: String = "Unknown",
    @SerializedName("sender_avatar") val senderAvatar: String? = null,
    val message: String = "",
    @SerializedName("message_type") val messageType: String = "text", // text, photo, video, voice, event, system
    val timestamp: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("is_deleted") val isDeleted: Boolean = false,
    val delivered: Boolean = false,
    @SerializedName("is_pinned") val isPinned: Boolean = false,
    @SerializedName("pinned_by") val pinnedBy: Long? = null,
    @SerializedName("pinned_at") val pinnedAt: String? = null,
    val duration: Int? = null,
    val metadata: Map<String, Any>? = null,
    @SerializedName("booking_id") val bookingId: Long? = null,
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("media_expires_at") val mediaExpiresAt: String? = null,
    @SerializedName("local_media_path") val localMediaPath: String? = null,
    @SerializedName("is_downloading") val isDownloading: Boolean = false,
    @SerializedName("download_progress") val downloadProgress: Float = 0f,
    val queued: Boolean = false,
    @SerializedName("client_msg_id") val clientMsgId: String? = null
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
    val duration: Int? = null,
    val metadata: Map<String, Any>? = null,
    @SerializedName("booking_id") val bookingId: Long? = null,
    val delivered: Boolean? = false,
    val queued: Boolean? = false,
    @SerializedName("client_msg_id") val clientMsgId: String? = null
)

data class SendMessageRequest(
    val message: String,
    @SerializedName("message_type") val messageType: String = "text",
    val booking: Long? = null,
    @SerializedName("booking_id") val bookingId: Long? = null,
    val metadata: Map<String, Any>? = null
)

data class Follower(
    val id: Long,
    val user: FollowerUser,
    @SerializedName("followed_at") val followedAt: String
)

data class FollowerUser(
    val id: Long,
    val username: String,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

data class MutualUser(
    val id: Long,
    val username: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

data class UserSearchResult(
    val id: String, // String or Long in json
    val username: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val email: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

data class ChatRequestUser(
    val id: Long,
    val username: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName(value = "profile_picture", alternate = ["profile_picture_secure", "avatar", "avatar_url"]) val profilePicture: String? = null
)

data class ChatRequestItem(
    val id: Long,
    val status: String, // pending, accepted, declined, cancelled
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("responded_at") val respondedAt: String? = null,
    @SerializedName("from_user") val fromUser: ChatRequestUser,
    @SerializedName("to_user") val toUser: ChatRequestUser,
    val counterpart: ChatRequestUser
)

data class ChatRequestListResponse(
    val received: List<ChatRequestItem> = emptyList(),
    val sent: List<ChatRequestItem> = emptyList()
)

data class BlockedUser(
    val id: Long,
    val username: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null,
    @SerializedName("blocked_at") val blockedAt: String? = null
)

data class UserReportItem(
    val id: Long,
    @SerializedName("reported_user_id") val reportedUserId: Long,
    @SerializedName("reported_user_name") val reportedUserName: String,
    @SerializedName("reported_user_username") val reportedUserUsername: String? = null,
    @SerializedName("reported_user_profile_picture") val reportedUserProfilePicture: String? = null,
    @SerializedName("chat_id") val chatId: Long? = null,
    val reason: String,
    val notes: String? = null,
    val status: String = "pending",
    @SerializedName("reviewed_note") val reviewedNote: String? = null,
    @SerializedName("can_cancel") val canCancel: Boolean? = true,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class RegisterDeviceRequest(
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("onesignal_id") val onesignalId: String,
    val platform: String = "android"
)

data class DiscoverTeam(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerializedName("members_count") val membersCount: Int = 0,
    @SerializedName("max_members") val maxMembers: Int = 20,
    val logo: String? = null,
    @SerializedName("cover_image") val coverImage: String? = null,
    val location: String? = null,
    @SerializedName("team_type") val teamType: String? = null,
    @SerializedName("skill_level") val skillLevel: String? = null,
    @SerializedName("is_public") val isPublic: Boolean = true,
    @SerializedName("join_status") val joinStatus: String? = "none", // none, requested, approved, rejected, member
    val admin: Long? = null
)

data class WebSocketMessage(
    val type: String = "message",
    val message: String? = null,
    @SerializedName("message_type") val messageType: String? = "text",
    @SerializedName("media_data") val mediaData: String? = null,
    val duration: Int? = null,
    val metadata: Map<String, Any>? = null,
    @SerializedName("booking_id") val bookingId: Long? = null,
    val booking: Any? = null,
    val sender: String? = null,
    @SerializedName("sender_id") val senderId: Long? = null,
    @SerializedName("sender_name") val senderName: String? = null,
    @SerializedName("sender_avatar") val senderAvatar: String? = null,
    @SerializedName("user_id") val userId: Long? = null,
    val online: Boolean? = null,
    @SerializedName("last_seen") val lastSeen: String? = null,
    val timestamp: String? = null,
    @SerializedName("message_id") val messageId: Long? = null,
    @SerializedName("local_id") val localId: Long? = null,
    @SerializedName("is_typing") val isTyping: Boolean? = null,
    @SerializedName("is_pinned") val isPinned: Boolean? = null,
    @SerializedName("pinned_by") val pinnedBy: Long? = null,
    @SerializedName("pinned_at") val pinnedAt: String? = null,
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("media_expires_at") val mediaExpiresAt: String? = null,
    @SerializedName("conversation_id") val conversationId: Long? = null,
    @SerializedName("chat_id") val chatId: Long? = null,
    val delivered: Boolean? = null,
    val error: String? = null,
    val counts: Map<String, Int>? = null
)

data class NewMessageNotification(
    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("message_id") val messageId: Long? = null,
    val message: String = "",
    @SerializedName("message_type") val messageType: String = "text",
    @SerializedName("sender_id") val senderId: Long = 0,
    @SerializedName("sender_name") val senderName: String = "",
    @SerializedName("sender_avatar") val senderAvatar: String? = null,
    val timestamp: String = "",
    @SerializedName("unread_count") val unreadCount: Int? = null
)

data class ChallengeTeam(
    val id: Long,
    val name: String,
    val logo: String? = null
)

data class ChallengeMember(
    val id: Long,
    val name: String,
    val avatar: String? = null,
    val role: String? = null
)

data class ChallengeInfo(
    val id: Long,
    val challenger: ChallengeTeam? = null,
    val challenged: ChallengeTeam? = null,
    val sport: SportSimple? = null,
    val venue: VenueSimple? = null,
    @SerializedName("match_date") val matchDate: String? = null,
    @SerializedName("match_time") val matchTime: String? = null,
    val status: String? = null,
    @SerializedName("challenger_members") val challengerMembers: List<ChallengeMember> = emptyList(),
    @SerializedName("challenged_members") val challengedMembers: List<ChallengeMember> = emptyList(),
    @SerializedName("past_games") val pastGames: PastGamesResponse? = null
)

data class RivalryInfo(
    val id: Long,
    @SerializedName("team_a") val teamA: ChallengeTeam,
    @SerializedName("team_b") val teamB: ChallengeTeam,
    @SerializedName("is_active") val isActive: Boolean? = null,
    @SerializedName("total_challenges") val totalChallenges: Int? = null
)

data class SportSimple(
    val id: Long,
    val name: String
)

data class VenueSimple(
    val id: Long,
    val name: String
)

data class PastGame(
    val id: Long,
    val status: String,
    @SerializedName("match_date") val matchDate: String? = null,
    @SerializedName("match_time") val matchTime: String? = null,
    @SerializedName("is_permanent") val isPermanent: Boolean = false,
    val stake: String? = null,
    @SerializedName("court_number") val courtNumber: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("updated_at") val updatedAt: String = "",
    val booking: GameBooking? = null,
    val venue: VenueSimple? = null,
    val game: SportSimple? = null
)

data class GameBooking(
    val id: Long,
    @SerializedName("booking_date") val bookingDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String? = null,
    val status: String,
    val team: ChallengeTeam? = null,
    @SerializedName("made_by") val madeBy: ChallengeMember? = null
)

data class PastGamesResponse(
    val page: Int = 1,
    @SerializedName(value = "page_size", alternate = ["pageSize"]) val pageSize: Int = 10,
    @SerializedName(value = "total", alternate = ["resultsCount"]) val total: Int = 0,
    val results: List<PastGame> = emptyList()
)
