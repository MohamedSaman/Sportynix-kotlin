package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LiveMatchTeamDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("short_name") val shortName: String?,
    @SerializedName("logo") val logo: String?,
    @SerializedName("score") val score: Any?, // Can be Int or String e.g. "150/4"
    @SerializedName("wickets") val wickets: Int?,
    @SerializedName("overs") val overs: String?
)

data class LiveMatchWinnerDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

data class LiveMatchDto(
    @SerializedName(value = "match_id", alternate = ["id"]) val matchId: Int,
    @SerializedName("league_id") val leagueId: Int?,
    @SerializedName("league_name") val leagueName: String?,
    @SerializedName("competition_type") val competitionType: String?,
    @SerializedName("source_label") val sourceLabel: String?,
    @SerializedName("source_reference") val sourceReference: String?,
    @SerializedName(value = "cricket_variant", alternate = ["ball_category", "legacy_cricket_variant", "league_cricket_variant", "variant"]) val cricketVariant: String?,
    @SerializedName("sport_type") val sportType: String?,
    @SerializedName("team1") val team1: LiveMatchTeamDto?,
    @SerializedName("team2") val team2: LiveMatchTeamDto?,
    @SerializedName("batting_team_id") val battingTeamId: Int?,
    @SerializedName("current_innings") val currentInnings: Int?,
    @SerializedName("display_message") val displayMessage: String?,
    @SerializedName("chase_status") val chaseStatus: String?,
    @SerializedName("toss_text") val tossText: String?,
    @SerializedName("target") val target: Int?,
    @SerializedName("runs_required") val runsRequired: Int?,
    @SerializedName("balls_remaining") val ballsRemaining: Int?,
    @SerializedName("is_break") val isBreak: Boolean?,
    @SerializedName("break_reason") val breakReason: String?,
    @SerializedName("venue") val venue: String?,
    @SerializedName("match_type") val matchType: String?,
    @SerializedName("match_number") val matchNumber: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("result") val result: String?,
    @SerializedName("winner") val winner: LiveMatchWinnerDto?,
    @SerializedName("margin") val margin: String?,
    @SerializedName("scheduled_date") val scheduledDate: String?,
    @SerializedName("scheduled_time") val scheduledTime: String?
)
