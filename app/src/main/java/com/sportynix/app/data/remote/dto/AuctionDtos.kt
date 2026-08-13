package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuctionWalletDto(
    @SerializedName("id") val id: String,
    @SerializedName("team_id") val teamId: String,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("team_logo") val teamLogo: String? = null,
    @SerializedName("starting_points") val startingPoints: Double = 0.0,
    @SerializedName("remaining_points") val remainingPoints: Double = 0.0,
    @SerializedName("spent_points") val spentPoints: Double = 0.0,
    @SerializedName("players_won_count") val playersWonCount: Int = 0,
    @SerializedName("slot_limit") val slotLimit: Int = 0
)

data class AuctionBattingStatsDto(
    @SerializedName("matches") val matches: Int = 0,
    @SerializedName("innings") val innings: Int = 0,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("average") val average: Double = 0.0,
    @SerializedName("strike_rate") val strikeRate: Double = 0.0,
    @SerializedName("highest_score") val highestScore: Int = 0,
    @SerializedName("balls_faced") val ballsFaced: Int = 0,
    @SerializedName("fours") val fours: Int = 0,
    @SerializedName("sixes") val sixes: Int = 0,
    @SerializedName("fifties") val fifties: Int = 0,
    @SerializedName("hundreds") val hundreds: Int = 0,
    @SerializedName("not_outs") val notOuts: Int = 0
)

data class AuctionBowlingStatsDto(
    @SerializedName("matches") val matches: Int = 0,
    @SerializedName("innings") val innings: Int = 0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("economy") val economy: Double = 0.0,
    @SerializedName("average") val average: Double = 0.0,
    @SerializedName("strike_rate") val strikeRate: Double = 0.0,
    @SerializedName("overs") val overs: Double = 0.0,
    @SerializedName("runs_conceded") val runsConceded: Int = 0,
    @SerializedName("maidens") val maidens: Int = 0,
    @SerializedName("dots") val dots: Int = 0,
    @SerializedName("best_bowling") val bestBowling: String? = null,
    @SerializedName("four_wickets") val fourWickets: Int = 0,
    @SerializedName("five_wickets") val fiveWickets: Int = 0
)

data class AuctionFieldingStatsDto(
    @SerializedName("catches") val catches: Int = 0,
    @SerializedName("stumpings") val stumpings: Int = 0,
    @SerializedName("run_outs") val runOuts: Int = 0
)

data class AuctionPlayerStatsDto(
    @SerializedName("source") val source: String = "profile",
    @SerializedName("variant") val variant: String? = null,
    @SerializedName("matches_played") val matchesPlayed: Int = 0,
    @SerializedName("innings_batted") val inningsBatted: Int = 0,
    @SerializedName("innings_bowled") val inningsBowled: Int = 0,
    @SerializedName("batting_runs") val battingRuns: Int = 0,
    @SerializedName("bowling_wickets") val bowlingWickets: Int = 0,
    @SerializedName("man_of_match_awards") val manOfMatchAwards: Int = 0,
    @SerializedName("titles_won") val titlesWon: Int = 0,
    @SerializedName("batting") val batting: AuctionBattingStatsDto? = null,
    @SerializedName("bowling") val bowling: AuctionBowlingStatsDto? = null,
    @SerializedName("fielding") val fielding: AuctionFieldingStatsDto? = null
)

data class AuctionPlayerDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String? = null,
    @SerializedName("application") val application: String? = null,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("role_label") val roleLabel: String? = null,
    @SerializedName("playing_position") val playingPosition: String? = null,
    @SerializedName("batting_style") val battingStyle: String? = null,
    @SerializedName("bowling_style") val bowlingStyle: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerializedName("status") val status: String = "available", // available, nominated, sold, unsold, withdrawn
    @SerializedName("sold_to_wallet_id") val soldToWalletId: String? = null,
    @SerializedName("sold_to_team_name") val soldToTeamName: String? = null,
    @SerializedName("sold_to_team_logo") val soldToTeamLogo: String? = null,
    @SerializedName("sold_for") val soldFor: Double = 0.0,
    @SerializedName("sold_at") val soldAt: String? = null,
    @SerializedName("player_statistics") val playerStatistics: AuctionPlayerStatsDto? = null
)

data class AuctionBidDto(
    @SerializedName("id") val id: String,
    @SerializedName("team_wallet_id") val teamWalletId: String,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("team_logo") val teamLogo: String? = null,
    @SerializedName("amount") val amount: Double,
    @SerializedName("sequence") val sequence: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AuctionNominationDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String = "active", // active, sold, unsold, reverted
    @SerializedName("nomination_order") val nominationOrder: Int = 0,
    @SerializedName("opening_bid") val openingBid: Double = 0.0,
    @SerializedName("current_bid") val currentBid: Double = 0.0,
    @SerializedName("current_team_wallet_id") val currentTeamWalletId: String? = null,
    @SerializedName("current_team_name") val currentTeamName: String? = null,
    @SerializedName("current_team_logo") val currentTeamLogo: String? = null,
    @SerializedName("sold_team_wallet_id") val soldTeamWalletId: String? = null,
    @SerializedName("sold_team_name") val soldTeamName: String? = null,
    @SerializedName("sold_team_logo") val soldTeamLogo: String? = null,
    @SerializedName("sold_amount") val soldAmount: Double = 0.0,
    @SerializedName("opened_at") val openedAt: String? = null,
    @SerializedName("closed_at") val closedAt: String? = null,
    @SerializedName("player") val player: AuctionPlayerDto,
    @SerializedName("bids") val bids: List<AuctionBidDto> = emptyList()
)

data class AuctionCommentaryEntryDto(
    @SerializedName("id") val id: String,
    @SerializedName("kind") val kind: String = "system",
    @SerializedName("message") val message: String,
    @SerializedName("sequence") val sequence: Int = 0,
    @SerializedName("created_by_name") val createdByName: String? = null,
    @SerializedName("is_retracted") val isRetracted: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AuctionActionLogDto(
    @SerializedName("id") val id: String,
    @SerializedName("sequence") val sequence: Int = 0,
    @SerializedName("action_type") val actionType: String,
    @SerializedName("actor_name") val actorName: String? = null,
    @SerializedName("is_reversible") val isReversible: Boolean = false,
    @SerializedName("is_reversed") val isReversed: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AuctionSessionSnapshotDto(
    @SerializedName("id") val id: String,
    @SerializedName("league") val leagueId: String,
    @SerializedName("league_name") val leagueName: String,
    @SerializedName("status") val status: String = "draft", // draft, live, paused, completed
    @SerializedName("starting_points") val startingPoints: Double = 10000.0,
    @SerializedName("minimum_opening_bid") val minimumOpeningBid: Double = 100.0,
    @SerializedName("bid_increment") val bidIncrement: Double = 50.0,
    @SerializedName("squad_size_limit") val squadSizeLimit: Int = 15,
    @SerializedName("current_nomination") val currentNomination: AuctionNominationDto? = null,
    @SerializedName("team_wallets") val teamWallets: List<AuctionWalletDto> = emptyList(),
    @SerializedName("players") val players: List<AuctionPlayerDto> = emptyList(),
    @SerializedName("commentary") val commentary: List<AuctionCommentaryEntryDto> = emptyList(),
    @SerializedName("action_logs") val actionLogs: List<AuctionActionLogDto> = emptyList(),
    @SerializedName("is_host_admin") val isHostAdmin: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class AuctionControlAccessDto(
    @SerializedName("league_id") val leagueId: String,
    @SerializedName("can_control") val canControl: Boolean = false
)

data class AuctionUpsertPayloadDto(
    @SerializedName("league_id") val leagueId: String,
    @SerializedName("starting_points") val startingPoints: Double,
    @SerializedName("minimum_opening_bid") val minimumOpeningBid: Double,
    @SerializedName("bid_increment") val bidIncrement: Double
)
