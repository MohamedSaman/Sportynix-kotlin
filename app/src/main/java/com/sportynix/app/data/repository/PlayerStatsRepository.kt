package com.sportynix.app.data.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.PlayerStatsApiService
import com.sportynix.app.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class BattingCareerStats(
    val matches: Int = 0,
    val innings: Int = 0,
    val runs: Int = 0,
    val balls: Int = 0,
    val average: Double = 0.0,
    val strikeRate: Double = 0.0,
    val highest: Int = 0,
    val fifties: Int = 0,
    val hundreds: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val notOuts: Int = 0
)

data class BowlingCareerStats(
    val matches: Int = 0,
    val innings: Int = 0,
    val wickets: Int = 0,
    val runs: Int = 0,
    val overs: Double = 0.0,
    val economy: Double = 0.0,
    val average: Double = 0.0,
    val strikeRate: Double = 0.0,
    val bestFigures: String = "0/0",
    val maidens: Int = 0,
    val dots: Int = 0,
    val fourWickets: Int = 0,
    val fiveWickets: Int = 0
)

data class FieldingCareerStats(
    val catches: Int = 0,
    val runOuts: Int = 0,
    val stumpings: Int = 0
)

data class GeneralCareerStats(
    val totalMatches: Int = 0,
    val momAwards: Int = 0
)

data class CareerStats(
    val batting: BattingCareerStats = BattingCareerStats(),
    val bowling: BowlingCareerStats = BowlingCareerStats(),
    val fielding: FieldingCareerStats = FieldingCareerStats(),
    val general: GeneralCareerStats = GeneralCareerStats()
)

@Singleton
class PlayerStatsRepository @Inject constructor(
    private val apiService: PlayerStatsApiService
) {
    suspend fun getPlayerMatchStatsPage(
        playerId: String,
        cricketVariant: String? = null,
        context: String? = null,
        venueCategory: String? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): ApiResult<PlayerMatchStatsPageDto> {
        return withContext(Dispatchers.IO) {
            try {
                val variantParam = if (cricketVariant != "all") cricketVariant else null
                val contextParam = if (context != "all") context else null
                val venueParam = if (venueCategory != "all") venueCategory else null

                val response = apiService.getPlayerMatchStats(
                    playerId = playerId,
                    cricketVariant = variantParam,
                    context = contextParam,
                    venueCategory = venueParam,
                    page = page,
                    pageSize = pageSize
                )
                ApiResult.Success(response)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to fetch player stats")
            }
        }
    }

    fun calculateCareerStats(matchStats: List<PlayerMatchStatDto>): CareerStats {
        var battingInnings = 0
        var battingRuns = 0
        var battingBalls = 0
        var battingFours = 0
        var battingSixes = 0
        var battingHighest = 0
        var battingFifties = 0
        var battingHundreds = 0
        var battingNotOuts = 0

        var bowlingInnings = 0
        var bowlingWickets = 0
        var bowlingRuns = 0
        var bowlingOvers = 0.0
        var bowlingMaidens = 0
        var bowlingDots = 0
        var bowlingFourWickets = 0
        var bowlingFiveWickets = 0
        var bestWickets = 0
        var bestRuns = 999

        var catches = 0
        var runOuts = 0
        var stumpings = 0
        var momAwards = 0

        matchStats.forEach { stat ->
            // Batting
            stat.battingStats?.let { bs ->
                if (bs.runsScored > 0 || bs.ballsFaced > 0) {
                    battingInnings++
                    battingRuns += bs.runsScored
                    battingBalls += bs.ballsFaced
                    battingFours += bs.fours
                    battingSixes += bs.sixes

                    if (bs.runsScored > battingHighest) {
                        battingHighest = bs.runsScored
                    }
                    if (bs.runsScored in 50..99) {
                        battingFifties++
                    } else if (bs.runsScored >= 100) {
                        battingHundreds++
                    }
                    if (bs.notOut) {
                        battingNotOuts++
                    }
                }
            }

            // Bowling
            stat.bowlingStats?.let { bw ->
                if (bw.wickets > 0 || bw.overs > 0.0) {
                    bowlingInnings++
                    bowlingWickets += bw.wickets
                    bowlingRuns += bw.runsConceded
                    bowlingOvers += bw.overs
                    bowlingMaidens += bw.maidens
                    bowlingDots += bw.dots

                    if (bw.wickets == 4) {
                        bowlingFourWickets++
                    } else if (bw.wickets >= 5) {
                        bowlingFiveWickets++
                    }

                    if (bw.wickets > bestWickets || (bw.wickets == bestWickets && bw.runsConceded < bestRuns)) {
                        bestWickets = bw.wickets
                        bestRuns = bw.runsConceded
                    }
                }
            }

            // Fielding
            stat.fieldingStats?.let { fs ->
                catches += fs.catches
                runOuts += fs.runOuts
                stumpings += fs.stumpings
            }

            // MOM
            if (stat.isManOfMatch) {
                momAwards++
            }
        }

        val dismissals = battingInnings - battingNotOuts
        val battingAvg = if (dismissals > 0) roundToTwoDecimals(battingRuns.toDouble() / dismissals) else battingRuns.toDouble()
        val battingSr = if (battingBalls > 0) roundToTwoDecimals((battingRuns.toDouble() / battingBalls) * 100) else 0.0

        val bowlingEcon = if (bowlingOvers > 0) roundToTwoDecimals(bowlingRuns.toDouble() / bowlingOvers) else 0.0
        val bowlingAvg = if (bowlingWickets > 0) roundToTwoDecimals(bowlingRuns.toDouble() / bowlingWickets) else 0.0
        val bowlingSr = if (bowlingWickets > 0) roundToOneDecimal((bowlingOvers * 6) / bowlingWickets) else 0.0
        val bestFiguresStr = if (bestWickets > 0 || bestRuns < 999) "$bestWickets/${if (bestRuns == 999) 0 else bestRuns}" else "0/0"

        return CareerStats(
            batting = BattingCareerStats(
                matches = battingInnings,
                innings = battingInnings,
                runs = battingRuns,
                balls = battingBalls,
                average = battingAvg,
                strikeRate = battingSr,
                highest = battingHighest,
                fifties = battingFifties,
                hundreds = battingHundreds,
                fours = battingFours,
                sixes = battingSixes,
                notOuts = battingNotOuts
            ),
            bowling = BowlingCareerStats(
                matches = bowlingInnings,
                innings = bowlingInnings,
                wickets = bowlingWickets,
                runs = bowlingRuns,
                overs = roundToOneDecimal(bowlingOvers),
                economy = bowlingEcon,
                average = bowlingAvg,
                strikeRate = bowlingSr,
                bestFigures = bestFiguresStr,
                maidens = bowlingMaidens,
                dots = bowlingDots,
                fourWickets = bowlingFourWickets,
                fiveWickets = bowlingFiveWickets
            ),
            fielding = FieldingCareerStats(
                catches = catches,
                runOuts = runOuts,
                stumpings = stumpings
            ),
            general = GeneralCareerStats(
                totalMatches = matchStats.size,
                momAwards = momAwards
            )
        )
    }

    private fun roundToTwoDecimals(value: Double): Double = (value * 100.0).roundToInt() / 100.0
    private fun roundToOneDecimal(value: Double): Double = (value * 10.0).roundToInt() / 10.0
}
