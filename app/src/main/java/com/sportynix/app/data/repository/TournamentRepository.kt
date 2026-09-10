package com.sportynix.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.TournamentApiService
import com.sportynix.app.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TournamentRepository @Inject constructor(
    private val apiService: TournamentApiService,
    private val gson: Gson
) {
    suspend fun getTournaments(
        search: String? = null,
        status: String? = null,
        sportType: String? = null,
        venueId: String? = null
    ): ApiResult<List<FullTournamentDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getTournaments(
                    search = search?.ifBlank { null },
                    status = status?.ifBlank { null },
                    sportType = sportType?.ifBlank { null },
                    venueId = venueId?.ifBlank { null }
                )
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load tournaments")
            }
        }
    }

    suspend fun getTournamentDetail(id: String): ApiResult<FullTournamentDto> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getTournamentDetail(id)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load tournament details")
            }
        }
    }

    suspend fun createTournamentMultipart(
        data: Map<String, RequestBody>,
        banner: MultipartBody.Part? = null
    ): ApiResult<FullTournamentDto> {
        return withContext(Dispatchers.IO) {
            try {
                val created = apiService.createTournamentMultipart(data, banner)
                ApiResult.Success(created)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to create tournament")
            }
        }
    }

    suspend fun createTournament(body: JsonObject): ApiResult<FullTournamentDto> {
        return withContext(Dispatchers.IO) {
            try {
                val created = apiService.createTournament(body)
                ApiResult.Success(created)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to create tournament")
            }
        }
    }

    suspend fun deleteTournament(id: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteTournament(id)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to delete tournament")
            }
        }
    }

    suspend fun publishTournament(id: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.publishTournament(id)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to publish tournament")
            }
        }
    }

    suspend fun openApplications(id: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.openApplications(id)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to open applications")
            }
        }
    }

    suspend fun closeApplications(id: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.closeApplications(id)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to close applications")
            }
        }
    }

    suspend fun finalizeTeams(id: String, applicationIds: List<String>? = null): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply {
                    if (!applicationIds.isNullOrEmpty()) {
                        add("application_ids", gson.toJsonTree(applicationIds))
                    }
                }
                apiService.finalizeTeams(id, body)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to finalize teams")
            }
        }
    }

    suspend fun generateMatches(id: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.generateMatches(id)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to generate matches")
            }
        }
    }

    suspend fun getEligibleTeams(id: String): ApiResult<List<EligibleTeamDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.getEligibleTeams(id)
                ApiResult.Success(res.results)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load eligible teams")
            }
        }
    }

    suspend fun applyTeam(id: String, payload: TournamentApplyPayloadDto): ApiResult<TournamentApplicationDto> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.applyTeam(id, payload)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to apply team")
            }
        }
    }

    suspend fun getApplications(id: String, status: String? = null): ApiResult<List<TournamentApplicationDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.getApplications(id, status?.ifBlank { null })
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load applications")
            }
        }
    }

    suspend fun reviewApplication(
        applicationId: String,
        status: String,
        reviewNote: String? = null
    ): ApiResult<TournamentApplicationDto> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.reviewApplication(
                    applicationId,
                    TournamentReviewPayloadDto(status = status, reviewNote = reviewNote)
                )
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to review application")
            }
        }
    }

    suspend fun getMatches(tournamentId: String): ApiResult<List<FullTournamentMatchDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.getMatches(tournamentId)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load matches")
            }
        }
    }

    suspend fun attachScoringLink(matchId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.attachScoringLink(matchId)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to attach scoring link")
            }
        }
    }

    suspend fun getParticipations(tournamentId: String): ApiResult<List<TournamentParticipationDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.getParticipations(tournamentId)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load participations")
            }
        }
    }

    suspend fun approveTournament(id: String, approvalNote: String = ""): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply { addProperty("approval_note", approvalNote) }
                apiService.approveTournament(id, body)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to approve tournament")
            }
        }
    }

    suspend fun rejectTournament(id: String, rejectionNote: String = ""): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply { addProperty("approval_note", rejectionNote) }
                apiService.rejectTournament(id, body)
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to reject tournament")
            }
        }
    }

    suspend fun requestTournamentEdit(id: String, changesRequested: String): ApiResult<TournamentEditRequestDto> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply { addProperty("changes_requested", changesRequested) }
                val res = apiService.requestTournamentEdit(id, body)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to submit edit request")
            }
        }
    }

    suspend fun getTournamentEditRequests(id: String): ApiResult<List<TournamentEditRequestDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.getTournamentEditRequests(id)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load edit requests")
            }
        }
    }

    suspend fun reviewTournamentEditRequest(
        requestId: String,
        status: String,
        reviewNote: String? = null
    ): ApiResult<TournamentEditRequestDto> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply {
                    addProperty("status", status)
                    if (!reviewNote.isNullOrBlank()) addProperty("review_note", reviewNote)
                }
                val res = apiService.reviewTournamentEditRequest(requestId, body)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to review edit request")
            }
        }
    }

    suspend fun requestApplicationEdit(applicationId: String, requestNote: String): ApiResult<TournamentApplicationDto> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply { addProperty("request_note", requestNote) }
                val res = apiService.requestApplicationEdit(applicationId, body)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to request application edit")
            }
        }
    }

    suspend fun reviewApplicationEditRequest(
        applicationId: String,
        status: String,
        reviewNote: String? = null
    ): ApiResult<TournamentApplicationDto> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply {
                    addProperty("status", status)
                    if (!reviewNote.isNullOrBlank()) addProperty("review_note", reviewNote)
                }
                val res = apiService.reviewApplicationEditRequest(applicationId, body)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to review application edit request")
            }
        }
    }

    suspend fun limitedUpdateTournament(id: String, body: JsonObject): ApiResult<FullTournamentDto> {
        return withContext(Dispatchers.IO) {
            try {
                val res = apiService.limitedUpdateTournament(id, body)
                ApiResult.Success(res)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to update tournament")
            }
        }
    }

    suspend fun getTeamMembersForRoster(teamId: String): ApiResult<List<TournamentTeamMemberDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val json = try {
                    apiService.getTeamDetails(teamId)
                } catch (e: Exception) {
                    apiService.getBookingTeamDetails(teamId)
                }
                val membersElement = json.get("members")
                val membersList = if (membersElement != null && membersElement.isJsonArray) {
                    val type = object : com.google.gson.reflect.TypeToken<List<TournamentTeamMemberDto>>() {}.type
                    gson.fromJson<List<TournamentTeamMemberDto>>(membersElement, type) ?: emptyList()
                } else {
                    emptyList()
                }
                ApiResult.Success(membersList)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load team roster members")
            }
        }
    }
}
