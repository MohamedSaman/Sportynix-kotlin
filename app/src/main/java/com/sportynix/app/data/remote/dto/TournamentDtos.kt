package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FullTournamentDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("banner") val banner: String? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("sport_type") val sportType: String = "cricket",
    @SerializedName("sport") val sport: String? = null,
    @SerializedName("cricket_variant") val cricketVariant: String? = "softball",
    @SerializedName("cricket_config") val cricketConfig: CricketConfigDto? = null,
    @SerializedName("format") val format: String = "knockout",
    @SerializedName("team_capacity") val teamCapacity: Int = 8,
    @SerializedName("max_teams") val maxTeams: Int? = null,
    @SerializedName("roster_size_limit") val rosterSizeLimit: Int = 15,
    @SerializedName("min_roster_size") val minRosterSize: Int = 7,
    @SerializedName("playing_players_count") val playingPlayersCount: Int? = 11,
    @SerializedName("entry_fee") val entryFee: Double? = 0.0,
    @SerializedName("prize_pool") val prizePool: Any? = null,
    @SerializedName("applications_open") val applicationsOpen: Boolean = true,
    @SerializedName("application_open_date") val applicationOpenDate: String? = null,
    @SerializedName("application_deadline") val applicationDeadline: String? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("status") val status: String = "draft",
    @SerializedName("is_public") val isPublic: Boolean = true,
    @SerializedName("created_by") val createdBy: Any? = null,
    @SerializedName("created_by_name") val createdByName: String? = null,
    @SerializedName("applications_count") val applicationsCount: Int = 0,
    @SerializedName("approved_teams_count") val approvedTeamsCount: Int = 0,
    @SerializedName("registered_teams_count") val registeredTeamsCount: Int? = null,
    @SerializedName("approval_status") val approvalStatus: String? = "pending",
    @SerializedName("approved_by") val approvedBy: String? = null,
    @SerializedName("approved_by_name") val approvedByName: String? = null,
    @SerializedName("approval_date") val approvalDate: String? = null,
    @SerializedName("approval_note") val approvalNote: String? = null,
    @SerializedName("is_creator") val isCreator: Boolean? = null,
    @SerializedName("is_staff_user") val isStaffUser: Boolean? = null,
    @SerializedName("display_status") val displayStatus: String? = null,
    @SerializedName("applications_effectively_open") val applicationsEffectivelyOpen: Boolean? = null,
    @SerializedName("effective_status") val effectiveStatus: String? = null,
    @SerializedName("venues") val venues: List<VenueDto>? = null,
    @SerializedName("custom_venue_text") val customVenueText: String? = null,
    @SerializedName("is_venue_hosted") val isVenueHosted: Boolean? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    val displayTitle: String
        get() = name.ifEmpty { title ?: "Tournament" }

    val actualSportType: String
        get() = sportType.ifEmpty { sport ?: "cricket" }

    val actualTeamCapacity: Int
        get() = if (teamCapacity > 0) teamCapacity else (maxTeams ?: 8)

    val actualApprovedCount: Int
        get() = if (approvedTeamsCount > 0) approvedTeamsCount else (registeredTeamsCount ?: 0)
}

data class TournamentApplicationUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

data class TournamentApplicationRosterMemberDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("user") val user: TournamentApplicationUserDto? = null,
    @SerializedName("role") val role: String = "player",
    @SerializedName("playing_position") val playingPosition: String? = null,
    @SerializedName("jersey_number") val jerseyNumber: Int? = null,
    @SerializedName("is_captain") val isCaptain: Boolean = false,
    @SerializedName("is_vice_captain") val isViceCaptain: Boolean = false
)

data class TeamDetailsSummaryDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("banner") val banner: String? = null,
    @SerializedName("team_type") val teamType: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("contact_email") val contactEmail: String? = null,
    @SerializedName("contact_number") val contactNumber: String? = null
)

data class TournamentApplicationDto(
    @SerializedName("id") val id: String,
    @SerializedName("tournament") val tournament: String? = null,
    @SerializedName("tournament_name") val tournamentName: String? = null,
    @SerializedName("team") val team: String? = null,
    @SerializedName("team_name") val teamName: String? = null,
    @SerializedName("team_details") val teamDetails: TeamDetailsSummaryDto? = null,
    @SerializedName("applied_by") val appliedBy: String? = null,
    @SerializedName("applied_by_name") val appliedByName: String? = null,
    @SerializedName("status") val status: String = "pending", // pending, approved, rejected, withdrawn, waitlisted, finalized
    @SerializedName("team_note") val teamNote: String? = null,
    @SerializedName("submitted_roster_count") val submittedRosterCount: Int = 0,
    @SerializedName("review_note") val reviewNote: String? = null,
    @SerializedName("reviewed_at") val reviewedAt: String? = null,
    @SerializedName("app_edit_request_status") val appEditRequestStatus: String? = null,
    @SerializedName("app_edit_requested_at") val appEditRequestedAt: String? = null,
    @SerializedName("app_edit_request_note") val appEditRequestNote: String? = null,
    @SerializedName("app_edit_review_note") val appEditReviewNote: String? = null,
    @SerializedName("roster_members") val rosterMembers: List<TournamentApplicationRosterMemberDto> = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null
)

data class EligibleTeamDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("member_count") val memberCount: Int? = 0,
    @SerializedName("already_applied") val alreadyApplied: Boolean = false
)

data class EligibleTeamsResponseDto(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("results") val results: List<EligibleTeamDto> = emptyList()
)

data class TournamentTeamMemberDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null,
    @SerializedName("playing_position") val playingPosition: String? = null,
    @SerializedName("batting_style") val battingStyle: String? = null,
    @SerializedName("bowling_style") val bowlingStyle: String? = null,
    @SerializedName("primary_role") val primaryRole: String? = null,
    @SerializedName("secondary_role") val secondaryRole: String? = null,
    @SerializedName("user") val user: UserSummaryDto? = null
) {
    val memberId: String
        get() = id.ifEmpty { userId ?: user?.id ?: "" }

    val displayName: String
        get() = fullName ?: name ?: user?.fullName ?: user?.name ?: "Player"

    val displayPicture: String?
        get() = profilePicture ?: user?.profilePicture
}

data class FullTournamentMatchDto(
    @SerializedName("id") val id: String,
    @SerializedName("tournament") val tournament: String? = null,
    @SerializedName("team1") val team1: String? = null,
    @SerializedName("team1_name") val team1Name: String? = null,
    @SerializedName("team2") val team2: String? = null,
    @SerializedName("team2_name") val team2Name: String? = null,
    @SerializedName("team1_team_id") val team1TeamId: String? = null,
    @SerializedName("team2_team_id") val team2TeamId: String? = null,
    @SerializedName("team1_logo") val team1Logo: String? = null,
    @SerializedName("team2_logo") val team2Logo: String? = null,
    @SerializedName("stage") val stage: String? = null,
    @SerializedName("round") val round: String? = null,
    @SerializedName("round_number") val roundNumber: Int? = null,
    @SerializedName("match_number") val matchNumber: Int? = null,
    @SerializedName("scheduled_at") val scheduledAt: String? = null,
    @SerializedName("status") val status: String = "scheduled",
    @SerializedName("linked_league_match") val linkedLeagueMatch: String? = null,
    @SerializedName("winner") val winner: String? = null,
    @SerializedName("winner_name") val winnerName: String? = null,
    @SerializedName("result_note") val resultNote: String? = null
)

data class TournamentParticipationDto(
    @SerializedName("id") val id: String,
    @SerializedName("tournament") val tournament: String? = null,
    @SerializedName("team") val team: String? = null,
    @SerializedName("team_name") val teamName: String? = null,
    @SerializedName("seed") val seed: Int? = null,
    @SerializedName("group_name") val groupName: String? = null,
    @SerializedName("status") val status: String = "confirmed",
    @SerializedName("roster_members") val rosterMembers: List<TournamentApplicationRosterMemberDto> = emptyList()
)

data class TournamentEditRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("tournament") val tournament: String? = null,
    @SerializedName("requested_by") val requestedBy: String? = null,
    @SerializedName("requested_by_name") val requestedByName: String? = null,
    @SerializedName("changes_requested") val changesRequested: String,
    @SerializedName("status") val status: String = "pending", // pending, approved, rejected
    @SerializedName("reviewed_by") val reviewedBy: String? = null,
    @SerializedName("reviewed_by_name") val reviewedByName: String? = null,
    @SerializedName("reviewed_at") val reviewedAt: String? = null,
    @SerializedName("review_note") val reviewNote: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class TournamentRosterMemberPayloadDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("role") val role: String = "player",
    @SerializedName("playing_position") val playingPosition: String? = null,
    @SerializedName("jersey_number") val jerseyNumber: Int? = null,
    @SerializedName("is_captain") val isCaptain: Boolean = false,
    @SerializedName("is_vice_captain") val isViceCaptain: Boolean = false
)

data class TournamentApplyPayloadDto(
    @SerializedName("team_id") val teamId: String,
    @SerializedName("team_note") val teamNote: String? = null,
    @SerializedName("roster") val roster: List<TournamentRosterMemberPayloadDto>
)

data class TournamentReviewPayloadDto(
    @SerializedName("status") val status: String,
    @SerializedName("review_note") val reviewNote: String? = null
)

data class GenericNotePayloadDto(
    @SerializedName("request_note") val requestNote: String? = null,
    @SerializedName("changes_requested") val changesRequested: String? = null,
    @SerializedName("approval_note") val approvalNote: String? = null
)
