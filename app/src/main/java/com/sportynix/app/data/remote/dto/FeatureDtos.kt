package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// --- League DTOs ---
data class LeagueDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("sport") val sport: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("banner_url") val bannerUrl: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null
)

data class FixtureDto(
    @SerializedName("id") val id: String,
    @SerializedName("league_id") val leagueId: String,
    @SerializedName("team1_name") val team1Name: String,
    @SerializedName("team2_name") val team2Name: String,
    @SerializedName("team1_logo") val team1Logo: String? = null,
    @SerializedName("team2_logo") val team2Logo: String? = null,
    @SerializedName("match_date") val matchDate: String,
    @SerializedName("venue") val venue: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("score_summary") val scoreSummary: String? = null
)

data class StandingDto(
    @SerializedName("id") val id: String,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("team_logo") val teamLogo: String? = null,
    @SerializedName("played") val played: Int,
    @SerializedName("won") val won: Int,
    @SerializedName("lost") val lost: Int,
    @SerializedName("drawn") val drawn: Int,
    @SerializedName("points") val points: Int,
    @SerializedName("net_run_rate") val netRunRate: Double? = null
)

data class PlayerStatDto(
    @SerializedName("player_id") val playerId: String,
    @SerializedName("player_name") val playerName: String,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("highest_score") val highestScore: Int = 0,
    @SerializedName("best_bowling") val bestBowling: String? = null,
    @SerializedName("matches") val matches: Int = 0
)

data class LeagueTeamDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("captain") val captain: String? = null,
    @SerializedName("players_count") val playersCount: Int = 0
)

// --- Tournament DTOs ---
data class TournamentDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("sport") val sport: String,
    @SerializedName("format") val format: String,
    @SerializedName("entry_fee") val entryFee: Double = 0.0,
    @SerializedName("prize_pool") val prizePool: Double = 0.0,
    @SerializedName("max_teams") val maxTeams: Int,
    @SerializedName("registered_teams_count") val registeredTeamsCount: Int,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("location") val location: String? = null
)

data class TournamentMatchDto(
    @SerializedName("id") val id: String,
    @SerializedName("round") val round: String,
    @SerializedName("team1") val team1: String,
    @SerializedName("team2") val team2: String,
    @SerializedName("winner") val winner: String? = null,
    @SerializedName("status") val status: String
)

data class BracketDto(
    @SerializedName("round_number") val roundNumber: Int,
    @SerializedName("round_name") val roundName: String,
    @SerializedName("matches") val matches: List<TournamentMatchDto>
)

data class TournamentRegistrationRequestDto(
    @SerializedName("team_name") val teamName: String,
    @SerializedName("captain_phone") val captainPhone: String
)

data class TournamentRegistrationResponseDto(
    @SerializedName("registration_id") val registrationId: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

// --- Cricket Scoring DTOs ---
data class CricketMatchDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("team_a") val teamA: String,
    @SerializedName("team_b") val teamB: String,
    @SerializedName("status") val status: String,
    @SerializedName("total_overs") val totalOvers: Int,
    @SerializedName("current_score") val currentScore: String? = null
)

data class LiveScorecardDto(
    @SerializedName("match_id") val matchId: String,
    @SerializedName("batting_team") val battingTeam: String,
    @SerializedName("bowling_team") val bowlingTeam: String,
    @SerializedName("runs") val runs: Int,
    @SerializedName("wickets") val wickets: Int,
    @SerializedName("overs") val overs: Double,
    @SerializedName("striker_name") val strikerName: String? = null,
    @SerializedName("striker_runs") val strikerRuns: Int = 0,
    @SerializedName("non_striker_name") val nonStrikerName: String? = null,
    @SerializedName("bowler_name") val bowlerName: String? = null,
    @SerializedName("recent_balls") val recentBalls: List<String> = emptyList()
)

data class BallRecordRequestDto(
    @SerializedName("runs") val runs: Int,
    @SerializedName("is_wicket") val isWicket: Boolean = false,
    @SerializedName("is_wide") val isWide: Boolean = false,
    @SerializedName("is_no_ball") val isNoBall: Boolean = false,
    @SerializedName("is_boundary") val isBoundary: Boolean = false,
    @SerializedName("is_six") val isSix: Boolean = false
)

data class BallRecordResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("updated_score") val updatedScore: LiveScorecardDto
)

// --- Auction DTOs ---
data class AuctionDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("league_name") val leagueName: String,
    @SerializedName("current_player_name") val currentPlayerName: String? = null,
    @SerializedName("current_bid_amount") val currentBidAmount: Double = 0.0,
    @SerializedName("current_highest_bidder") val currentHighestBidder: String? = null,
    @SerializedName("status") val status: String
)

data class AuctionTeamDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("purse_remaining") val purseRemaining: Double,
    @SerializedName("players_bought") val playersBought: Int
)

data class AuctionBidRequestDto(
    @SerializedName("bid_amount") val bidAmount: Double,
    @SerializedName("team_id") val teamId: String
)

data class AuctionBidResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("new_highest_bid") val newHighestBid: Double,
    @SerializedName("highest_bidder_name") val highestBidderName: String
)
