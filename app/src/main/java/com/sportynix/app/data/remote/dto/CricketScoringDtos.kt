package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StartMatchRequestDto(
    @SerializedName("toss_winner_id") val tossWinnerId: String,
    @SerializedName("toss_decision") val tossDecision: String // "bat" or "bowl"
)

data class StartInningsRequestDto(
    @SerializedName("batting_team_id") val battingTeamId: String,
    @SerializedName("bowling_team_id") val bowlingTeamId: String,
    @SerializedName("innings_number") val inningsNumber: Int
)

data class SetBatsmenRequestDto(
    @SerializedName("striker_id") val strikerId: String,
    @SerializedName("non_striker_id") val nonStrikerId: String
)

data class SetBowlerRequestDto(
    @SerializedName("bowler_id") val bowlerId: String
)

data class PlayingXIPlayerDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("role") val role: String? = null,
    @SerializedName("is_captain") val isCaptain: Boolean = false,
    @SerializedName("is_vice_captain") val isViceCaptain: Boolean = false,
    @SerializedName("is_wicketkeeper") val isWicketKeeper: Boolean = false
)

data class PlayingXITeamDto(
    @SerializedName("team_id") val teamId: String,
    @SerializedName("players") val players: List<PlayingXIPlayerDto>
)

data class PlayingXIRequestDto(
    @SerializedName("team1") val team1: PlayingXITeamDto,
    @SerializedName("team2") val team2: PlayingXITeamDto
)

data class PlayingXIResponseDto(
    @SerializedName("team1") val team1: PlayingXITeamDto?,
    @SerializedName("team2") val team2: PlayingXITeamDto?
)

data class RecordBallRequestDto(
    @SerializedName("ball_type") val ballType: String, // "legal", "wide", "no_ball", "bye", "leg_bye"
    @SerializedName("runs") val runs: Int,
    @SerializedName("extra_runs") val extraRuns: Int = 0,
    @SerializedName("is_wicket") val isWicket: Boolean = false,
    @SerializedName("wicket_type") val wicketType: String? = null,
    @SerializedName("dismissed_batsman_id") val dismissedBatsmanId: String? = null,
    @SerializedName("fielder_id") val fielderId: String? = null,
    @SerializedName("bowler_id") val bowlerId: String? = null,
    @SerializedName("batsman_id") val batsmanId: String? = null,
    @SerializedName("non_striker_id") val nonStrikerId: String? = null
)

data class RecordPenaltyRequestDto(
    @SerializedName("runs") val runs: Int,
    @SerializedName("team_id") val teamId: String,
    @SerializedName("reason") val reason: String? = null
)

data class EndMatchRequestDto(
    @SerializedName("result") val result: String? = null,
    @SerializedName("winner_team_id") val winnerTeamId: String? = null
)

data class LivePlayerDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("balls") val balls: Int = 0,
    @SerializedName("fours") val fours: Int = 0,
    @SerializedName("sixes") val sixes: Int = 0,
    @SerializedName("strike_rate") val strikeRate: Double = 0.0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("runs_conceded") val runsConceded: Int = 0,
    @SerializedName("overs") val overs: Double = 0.0,
    @SerializedName("economy") val economy: Double = 0.0,
    @SerializedName("maidens") val maidens: Int = 0
)

data class LiveInningDataDto(
    @SerializedName("innings_number") val inningsNumber: Int,
    @SerializedName("batting_team_id") val battingTeamId: String?,
    @SerializedName("batting_team_name") val battingTeamName: String?,
    @SerializedName("bowling_team_id") val bowlingTeamId: String?,
    @SerializedName("bowling_team_name") val bowlingTeamName: String?,
    @SerializedName("score") val score: Int = 0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("overs") val overs: Double = 0.0,
    @SerializedName("current_run_rate") val currentRunRate: Double = 0.0,
    @SerializedName("required_run_rate") val requiredRunRate: Double? = null,
    @SerializedName("target") val target: Int? = null,
    @SerializedName("striker") val striker: LivePlayerDto? = null,
    @SerializedName("non_striker") val nonStriker: LivePlayerDto? = null,
    @SerializedName("bowler") val bowler: LivePlayerDto? = null,
    @SerializedName("recent_balls") val recentBalls: List<String> = emptyList(),
    @SerializedName("is_completed") val isCompleted: Boolean = false
)

data class LiveStateDto(
    @SerializedName("match_id") val matchId: String,
    @SerializedName("status") val status: String,
    @SerializedName("current_innings") val currentInnings: Int = 1,
    @SerializedName("toss_winner_id") val tossWinnerId: String? = null,
    @SerializedName("toss_decision") val tossDecision: String? = null,
    @SerializedName("is_free_hit") val isFreeHit: Boolean = false,
    @SerializedName("inning1") val inning1: LiveInningDataDto? = null,
    @SerializedName("inning2") val inning2: LiveInningDataDto? = null,
    @SerializedName("display_message") val displayMessage: String? = null,
    @SerializedName("result_text") val resultText: String? = null,
    @SerializedName("man_of_match") val manOfMatch: LivePlayerDto? = null
)

data class ScorecardBatsmanDto(
    @SerializedName("player_id") val playerId: String,
    @SerializedName("name") val name: String,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("balls") val balls: Int = 0,
    @SerializedName("fours") val fours: Int = 0,
    @SerializedName("sixes") val sixes: Int = 0,
    @SerializedName("strike_rate") val strikeRate: Double = 0.0,
    @SerializedName("dismissal_text") val dismissalText: String = "not out",
    @SerializedName("is_out") val isOut: Boolean = false
)

data class ScorecardBowlerDto(
    @SerializedName("player_id") val playerId: String,
    @SerializedName("name") val name: String,
    @SerializedName("overs") val overs: Double = 0.0,
    @SerializedName("maidens") val maidens: Int = 0,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("economy") val economy: Double = 0.0,
    @SerializedName("wides") val wides: Int = 0,
    @SerializedName("no_balls") val noBalls: Int = 0
)

data class FallOfWicketDto(
    @SerializedName("wicket_number") val wicketNumber: Int,
    @SerializedName("runs") val runs: Int,
    @SerializedName("overs") val overs: Double,
    @SerializedName("player_name") val playerName: String
)

data class PartnershipDto(
    @SerializedName("runs") val runs: Int,
    @SerializedName("balls") val balls: Int,
    @SerializedName("player1_name") val player1Name: String,
    @SerializedName("player2_name") val player2Name: String
)

data class ExtrasBreakdownDto(
    @SerializedName("wides") val wides: Int = 0,
    @SerializedName("no_balls") val noBalls: Int = 0,
    @SerializedName("byes") val byes: Int = 0,
    @SerializedName("leg_byes") val legByes: Int = 0,
    @SerializedName("penalty") val penalty: Int = 0,
    @SerializedName("total") val total: Int = 0
)

data class ScorecardInningsDto(
    @SerializedName("innings_number") val inningsNumber: Int,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("total_runs") val totalRuns: Int = 0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("overs") val overs: Double = 0.0,
    @SerializedName("extras") val extras: ExtrasBreakdownDto? = null,
    @SerializedName("batsmen") val batsmen: List<ScorecardBatsmanDto> = emptyList(),
    @SerializedName("bowlers") val bowlers: List<ScorecardBowlerDto> = emptyList(),
    @SerializedName("fall_of_wickets") val fallOfWickets: List<FallOfWicketDto> = emptyList(),
    @SerializedName("partnerships") val partnerships: List<PartnershipDto> = emptyList(),
    @SerializedName("did_not_bat") val didNotBat: List<String> = emptyList()
)

data class ScorecardDto(
    @SerializedName("match_id") val matchId: String,
    @SerializedName("innings") val innings: List<ScorecardInningsDto> = emptyList(),
    @SerializedName("result") val result: String? = null
)

data class EligibleBatsmanDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("is_batting") val isBatting: Boolean = false,
    @SerializedName("is_out") val isOut: Boolean = false
)

data class EligibleBatsmenResponseDto(
    @SerializedName("eligible_batsmen") val eligibleBatsmen: List<EligibleBatsmanDto> = emptyList()
)

data class BallByBallBallDto(
    @SerializedName("id") val id: String,
    @SerializedName("ball_number") val ballNumber: String,
    @SerializedName("over_number") val overNumber: Int,
    @SerializedName("runs") val runs: Int,
    @SerializedName("ball_type") val ballType: String,
    @SerializedName("is_wicket") val isWicket: Boolean,
    @SerializedName("wicket_type") val wicketType: String? = null,
    @SerializedName("batsman_name") val batsmanName: String,
    @SerializedName("bowler_name") val bowlerName: String,
    @SerializedName("commentary") val commentary: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class BallByBallResponseDto(
    @SerializedName("balls") val balls: List<BallByBallBallDto> = emptyList()
)

data class MatchSummaryDto(
    @SerializedName("match_id") val matchId: String,
    @SerializedName("title") val title: String?,
    @SerializedName("team1_name") val team1Name: String?,
    @SerializedName("team2_name") val team2Name: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("result") val result: String?
)

data class MOMCandidateDto(
    @SerializedName("player_id") val playerId: String,
    @SerializedName("name") val name: String,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("catches") val catches: Int = 0,
    @SerializedName("impact_score") val impactScore: Double = 0.0,
    @SerializedName("reason") val reason: String? = null
)

data class MOMSuggestionResponseDto(
    @SerializedName("candidates") val candidates: List<MOMCandidateDto> = emptyList(),
    @SerializedName("top_candidate") val topCandidate: MOMCandidateDto? = null
)

data class FinalizeMOMRequestDto(
    @SerializedName("player_id") val playerId: String,
    @SerializedName("reason") val reason: String? = null
)
