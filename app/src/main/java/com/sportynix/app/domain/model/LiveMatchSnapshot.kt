package com.sportynix.app.domain.model

data class LiveMatchTeam(
    val id: String = "",
    val name: String = "Team A",
    val shortName: String = "TMA",
    val logo: String? = null,
    val score: String = "",
    val wickets: Int? = null,
    val overs: String? = null
)

data class LiveMatchSnapshot(
    val matchId: String,
    val leagueId: String = "",
    val leagueName: String = "",
    val competitionType: String? = null,
    val sourceLabel: String? = null,
    val sourceReference: String? = null,
    val cricketVariant: String? = null,
    val sportType: String = "cricket",
    val team1: LiveMatchTeam = LiveMatchTeam(),
    val team2: LiveMatchTeam = LiveMatchTeam(),
    val battingTeamId: String? = null,
    val currentInnings: Int? = null,
    val displayMessage: String? = null,
    val chaseStatus: String? = null,
    val tossText: String? = null,
    val target: Int? = null,
    val runsRequired: Int? = null,
    val ballsRemaining: Int? = null,
    val isBreak: Boolean = false,
    val breakReason: String? = null,
    val venue: String? = null,
    val matchType: String? = null,
    val matchNumber: Int? = null,
    val status: String = "scheduled",
    val result: String? = null,
    val winnerName: String? = null,
    val margin: String? = null,
    val scheduledDate: String? = null,
    val scheduledTime: String? = null
)
