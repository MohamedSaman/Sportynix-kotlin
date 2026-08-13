package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BattingStatsDto(
    @SerializedName("runs_scored") val runsScored: Int = 0,
    @SerializedName("balls_faced") val ballsFaced: Int = 0,
    @SerializedName("fours") val fours: Int = 0,
    @SerializedName("sixes") val sixes: Int = 0,
    @SerializedName("strike_rate") val strikeRate: Double = 0.0,
    @SerializedName("not_out") val notOut: Boolean = false,
    @SerializedName("dismissal_type") val dismissalType: String? = null
)

data class BowlingStatsDto(
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("runs_conceded") val runsConceded: Int = 0,
    @SerializedName("overs") val overs: Double = 0.0,
    @SerializedName("economy") val economy: Double = 0.0,
    @SerializedName("maidens") val maidens: Int = 0,
    @SerializedName("dots") val dots: Int = 0,
    @SerializedName("wides") val wides: Int = 0,
    @SerializedName("no_balls") val noBalls: Int = 0
)

data class FieldingStatsDto(
    @SerializedName("catches") val catches: Int = 0,
    @SerializedName("run_outs") val runOuts: Int = 0,
    @SerializedName("stumpings") val stumpings: Int = 0
)

data class PlayerMatchStatTeamDto(
    @SerializedName("id") val id: String,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("team_name") val teamName: String? = null,
    @SerializedName("logo") val logo: String? = null
)

data class PlayerMatchStatLeagueDto(
    @SerializedName("name") val name: String?,
    @SerializedName("sport_type") val sportType: String?
)

data class PlayerMatchStatMatchDto(
    @SerializedName("id") val id: String,
    @SerializedName("match_name") val matchName: String? = null,
    @SerializedName("scheduled_date") val scheduledDate: String? = null,
    @SerializedName("scheduled_time") val scheduledTime: String? = null,
    @SerializedName("result") val result: String? = null,
    @SerializedName("team1") val team1: PlayerMatchStatTeamDto? = null,
    @SerializedName("team2") val team2: PlayerMatchStatTeamDto? = null,
    @SerializedName("league") val league: PlayerMatchStatLeagueDto? = null
)

data class PlayerMatchStatDto(
    @SerializedName("id") val id: String,
    @SerializedName("match") val match: PlayerMatchStatMatchDto?,
    @SerializedName("team") val team: PlayerMatchStatTeamDto?,
    @SerializedName("batting_stats") val battingStats: BattingStatsDto? = null,
    @SerializedName("bowling_stats") val bowlingStats: BowlingStatsDto? = null,
    @SerializedName("fielding_stats") val fieldingStats: FieldingStatsDto? = null,
    @SerializedName("is_man_of_match") val isManOfMatch: Boolean = false
)

data class PlayerMatchStatsPageDto(
    @SerializedName("results") val results: List<PlayerMatchStatDto> = emptyList(),
    @SerializedName("next") val next: String? = null,
    @SerializedName("previous") val previous: String? = null,
    @SerializedName("count") val count: Int = 0
)
