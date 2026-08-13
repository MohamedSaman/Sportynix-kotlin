package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserSummaryDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

data class CricketConfigDto(
    @SerializedName("overs") val overs: Int = 20,
    @SerializedName("powerplay_overs") val powerplayOvers: Int? = null,
    @SerializedName("death_overs") val deathOvers: Int? = null,
    @SerializedName("balls_per_over") val ballsPerOver: Int? = 6
)

data class SportConfigDto(
    @SerializedName("halves") val halves: Int? = null,
    @SerializedName("half_duration") val halfDuration: Int? = null,
    @SerializedName("extra_time") val extraTime: Boolean? = null,
    @SerializedName("quarters") val quarters: Int? = null,
    @SerializedName("quarter_duration") val quarterDuration: Int? = null,
    @SerializedName("sets_to_win") val setsToWin: Int? = null
)

data class SponsorDto(
    @SerializedName("name") val name: String,
    @SerializedName("logo") val logo: String? = null
)

data class SocialLinksDto(
    @SerializedName("twitter") val twitter: String? = null,
    @SerializedName("instagram") val instagram: String? = null,
    @SerializedName("facebook") val facebook: String? = null,
    @SerializedName("youtube") val youtube: String? = null
)

data class LeagueTeamSummaryDto(
    @SerializedName("id") val id: String,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("team_short_name") val teamShortName: String? = null,
    @SerializedName("team_logo") val teamLogo: String? = null,
    @SerializedName("status") val status: String? = null
)

data class FullLeagueDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("sport_type") val sportType: String = "cricket",
    @SerializedName("cricket_variant") val cricketVariant: String? = null,
    @SerializedName("format") val format: String = "round_robin",
    @SerializedName("status") val status: String = "draft",
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("banner") val banner: String? = null,
    @SerializedName("theme_color") val themeColor: String? = null,
    @SerializedName("num_teams") val numTeams: Int = 8,
    @SerializedName("squad_size") val squadSize: Int? = null,
    @SerializedName("min_players") val minPlayers: Int? = null,
    @SerializedName("playing_players_count") val playingPlayersCount: Int? = null,
    @SerializedName("game_duration") val gameDuration: Int? = null,
    @SerializedName("cricket_config") val cricketConfig: CricketConfigDto? = null,
    @SerializedName("sport_config") val sportConfig: SportConfigDto? = null,
    @SerializedName("season_year") val seasonYear: Int? = null,
    @SerializedName("season_name") val seasonName: String? = null,
    @SerializedName("registration_start") val registrationStart: String? = null,
    @SerializedName("registration_end") val registrationEnd: String? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("allow_draws") val allowDraws: Boolean? = null,
    @SerializedName("enable_tiebreaker") val enableTiebreaker: Boolean? = null,
    @SerializedName("prize_pool") val prizePool: String? = null,
    @SerializedName("sponsors") val sponsors: List<SponsorDto>? = null,
    @SerializedName("rules_text") val rulesText: String? = null,
    @SerializedName("is_public") val isPublic: Boolean? = true,
    @SerializedName("is_featured") val isFeatured: Boolean? = false,
    @SerializedName("is_active") val isActive: Boolean? = true,
    @SerializedName("website") val website: String? = null,
    @SerializedName("social_links") val socialLinks: SocialLinksDto? = null,
    @SerializedName("contact_email") val contactEmail: String? = null,
    @SerializedName("contact_phone") val contactPhone: String? = null,
    @SerializedName("matches_completed") val matchesCompleted: Int? = 0,
    @SerializedName("total_matches_planned") val totalMatchesPlanned: Int? = 0,
    @SerializedName("teams_count") val teamsCount: Int? = 0,
    @SerializedName("created_by") val createdBy: UserSummaryDto? = null,
    @SerializedName("is_venue_hosted") val isVenueHosted: Boolean? = false,
    @SerializedName("primary_venue") val primaryVenue: VenueDto? = null,
    @SerializedName("venues") val venues: List<VenueDto>? = null,
    @SerializedName("teams") val teams: List<LeagueTeamSummaryDto>? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("is_creator") val isCreator: Boolean? = null,
    @SerializedName("is_admin") val isAdmin: Boolean? = null,
    @SerializedName("is_moderator") val isModerator: Boolean? = null,
    @SerializedName("user_application_status") val userApplicationStatus: String? = null,
    @SerializedName("user_application_id") val userApplicationId: String? = null
)

data class SquadMemberDto(
    @SerializedName("id") val id: String,
    @SerializedName("user") val user: UserSummaryDto,
    @SerializedName("jersey_number") val jerseyNumber: Int? = null,
    @SerializedName("role") val role: String = "player",
    @SerializedName("playing_position") val playingPosition: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("is_available") val isAvailable: Boolean = true
)

data class FullLeagueTeamDto(
    @SerializedName("id") val id: String,
    @SerializedName("team") val team: UserSummaryDto? = null,
    @SerializedName("team_name_override") val teamNameOverride: String? = null,
    @SerializedName("team_short_name") val teamShortName: String? = null,
    @SerializedName("captain") val captain: UserSummaryDto? = null,
    @SerializedName("vice_captain") val viceCaptain: UserSummaryDto? = null,
    @SerializedName("co_admins") val coAdmins: List<UserSummaryDto>? = null,
    @SerializedName("status") val status: String = "approved",
    @SerializedName("jersey_color") val jerseyColor: String? = null,
    @SerializedName("team_logo_override") val teamLogoOverride: String? = null,
    @SerializedName("squad_size") val squadSize: Int? = 0,
    @SerializedName("squad") val squad: List<SquadMemberDto>? = null,
    @SerializedName("is_captain") val isCaptain: Boolean? = false,
    @SerializedName("is_co_admin") val isCoAdmin: Boolean? = false,
    @SerializedName("is_vice_captain") val isViceCaptain: Boolean? = false,
    @SerializedName("is_league_moderator") val isLeagueModerator: Boolean? = false,
    @SerializedName("is_league_creator") val isLeagueCreator: Boolean? = false,
    @SerializedName("can_manage_team") val canManageTeam: Boolean? = false
)

data class LeaguePlayerApplicationDto(
    @SerializedName("id") val id: String,
    @SerializedName("league") val leagueId: String,
    @SerializedName("league_name") val leagueName: String? = null,
    @SerializedName("user") val user: UserSummaryDto,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("application_note") val applicationNote: String? = null,
    @SerializedName("cricket_preferred_variant") val cricketPreferredVariant: String? = null,
    @SerializedName("cricket_primary_role") val cricketPrimaryRole: String? = null,
    @SerializedName("cricket_playing_position") val cricketPlayingPosition: String? = null,
    @SerializedName("cricket_batting_style") val cricketBattingStyle: String? = null,
    @SerializedName("cricket_bowling_style") val cricketBowlingStyle: String? = null,
    @SerializedName("reviewed_by") val reviewedBy: UserSummaryDto? = null,
    @SerializedName("review_note") val reviewNote: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class BulkReviewRequestDto(
    @SerializedName("application_ids") val applicationIds: List<String>,
    @SerializedName("status") val status: String,
    @SerializedName("review_note") val reviewNote: String? = null
)

data class FullStandingDto(
    @SerializedName("id") val id: String,
    @SerializedName("team_name") val teamName: String? = null,
    @SerializedName("team_short_name") val teamShortName: String? = null,
    @SerializedName("team_logo") val teamLogo: String? = null,
    @SerializedName("rank") val rank: Int = 0,
    @SerializedName("matches_played") val matchesPlayed: Int = 0,
    @SerializedName("wins") val wins: Int = 0,
    @SerializedName("losses") val losses: Int = 0,
    @SerializedName("draws") val draws: Int = 0,
    @SerializedName("ties") val ties: Int = 0,
    @SerializedName("points") val points: Int = 0,
    @SerializedName("net_run_rate") val netRunRate: Double? = 0.0,
    @SerializedName("recent_form") val recentForm: String? = null,
    @SerializedName("is_qualified") val isQualified: Boolean? = false,
    @SerializedName("is_eliminated") val isEliminated: Boolean? = false
)
