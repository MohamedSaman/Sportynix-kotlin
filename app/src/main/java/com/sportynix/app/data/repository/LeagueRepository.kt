package com.sportynix.app.data.repository

import com.google.gson.JsonObject
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.LeagueApiService
import com.sportynix.app.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeagueRepository @Inject constructor(
    private val apiService: LeagueApiService
) {
    suspend fun getLeagues(
        search: String? = null,
        sportType: String? = null,
        status: String? = null,
        format: String? = null,
        seasonYear: Int? = null,
        isPublic: Boolean? = null,
        isFeatured: Boolean? = null,
        venueId: String? = null
    ): ApiResult<List<FullLeagueDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeagues(
                    search = search,
                    sportType = sportType,
                    status = status,
                    format = format,
                    seasonYear = seasonYear,
                    isPublic = isPublic,
                    isFeatured = isFeatured,
                    venueId = venueId
                )
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load leagues")
            }
        }
    }

    suspend fun getLeagueDetail(leagueId: String): ApiResult<FullLeagueDto> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeagueDetail(leagueId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load league details")
            }
        }
    }

    suspend fun createLeague(
        data: RequestBody,
        logo: MultipartBody.Part? = null,
        banner: MultipartBody.Part? = null
    ): ApiResult<FullLeagueDto> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.createLeague(data, logo, banner)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to create league")
            }
        }
    }

    suspend fun updateLeague(
        leagueId: String,
        data: RequestBody,
        logo: MultipartBody.Part? = null,
        banner: MultipartBody.Part? = null
    ): ApiResult<FullLeagueDto> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.updateLeague(leagueId, data, logo, banner)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to update league")
            }
        }
    }

    suspend fun publishLeague(leagueId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.publishLeague(leagueId)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to publish league")
            }
        }
    }

    suspend fun startLeague(leagueId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.startLeague(leagueId)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to start league")
            }
        }
    }

    suspend fun suspendLeague(leagueId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.suspendLeague(leagueId)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to suspend league")
            }
        }
    }

    suspend fun completeLeague(leagueId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.completeLeague(leagueId)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to complete league")
            }
        }
    }

    suspend fun cancelLeague(leagueId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.cancelLeague(leagueId)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to cancel league")
            }
        }
    }

    suspend fun getLeagueTeams(leagueId: String): ApiResult<List<FullLeagueTeamDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeagueTeams(leagueId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load league teams")
            }
        }
    }

    suspend fun getLeagueTeamDetail(teamId: String): ApiResult<FullLeagueTeamDto> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeagueTeamDetail(teamId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load team details")
            }
        }
    }

    suspend fun registerTeam(leagueId: String, body: JsonObject): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.registerTeam(leagueId, body)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to register team")
            }
        }
    }

    suspend fun addSquadMember(teamId: String, body: JsonObject): ApiResult<SquadMemberDto> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.addSquadMember(teamId, body)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to add squad member")
            }
        }
    }

    suspend fun bulkAddSquadMembers(teamId: String, body: JsonObject): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.bulkAddSquadMembers(teamId, body)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to bulk add squad members")
            }
        }
    }

    suspend fun removeSquadMember(teamId: String, memberId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.removeSquadMember(teamId, memberId)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to remove squad member")
            }
        }
    }

    suspend fun updateSquadMember(teamId: String, memberId: String, body: JsonObject): ApiResult<SquadMemberDto> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.updateSquadMember(teamId, memberId, body)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to update squad member")
            }
        }
    }

    suspend fun addCoAdmin(teamId: String, userId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply { addProperty("user_id", userId) }
                apiService.addCoAdmin(teamId, body)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to add co-admin")
            }
        }
    }

    suspend fun removeCoAdmin(teamId: String, userId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.removeCoAdmin(teamId, userId)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to remove co-admin")
            }
        }
    }

    suspend fun getLeaguePlayerApplications(leagueId: String): ApiResult<List<LeaguePlayerApplicationDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeaguePlayerApplications(leagueId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load player applications")
            }
        }
    }

    suspend fun applyAsPlayer(leagueId: String, body: JsonObject): ApiResult<LeaguePlayerApplicationDto> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.applyAsPlayer(leagueId, body)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to submit player application")
            }
        }
    }

    suspend fun reviewApplication(appId: String, status: String, reviewNote: String?): ApiResult<LeaguePlayerApplicationDto> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply {
                    addProperty("status", status)
                    if (reviewNote != null) addProperty("review_note", reviewNote)
                }
                val res = apiService.reviewApplication(appId, body)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to review application")
            }
        }
    }

    suspend fun bulkReviewApplications(appIds: List<String>, status: String, reviewNote: String?): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val dto = BulkReviewRequestDto(applicationIds = appIds, status = status, reviewNote = reviewNote)
                apiService.bulkReviewApplications(dto)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to bulk review applications")
            }
        }
    }

    suspend fun withdrawApplication(appId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.withdrawApplication(appId)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to withdraw application")
            }
        }
    }

    suspend fun getLeagueFixtures(leagueId: String): ApiResult<List<FixtureDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeagueFixtures(leagueId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load fixtures")
            }
        }
    }

    suspend fun generateSchedule(leagueId: String, body: JsonObject): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.generateSchedule(leagueId, body)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to generate schedule")
            }
        }
    }

    suspend fun getLeagueStandings(leagueId: String): ApiResult<List<FullStandingDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeagueStandings(leagueId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load standings")
            }
        }
    }

    suspend fun getLeagueStats(leagueId: String): ApiResult<List<PlayerStatDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeagueStats(leagueId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load stats")
            }
        }
    }
}
