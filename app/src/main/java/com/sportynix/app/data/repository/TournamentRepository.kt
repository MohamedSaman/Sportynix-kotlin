package com.sportynix.app.data.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.TournamentApiService
import com.sportynix.app.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TournamentRepository @Inject constructor(
    private val apiService: TournamentApiService
) {
    suspend fun getTournaments(search: String? = null, sport: String? = null): ApiResult<List<TournamentDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getTournaments(search, sport)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load tournaments")
            }
        }
    }

    suspend fun getTournamentDetail(id: String): ApiResult<TournamentDto> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getTournamentDetail(id)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load tournament details")
            }
        }
    }

    suspend fun getTournamentMatches(id: String): ApiResult<List<TournamentMatchDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getTournamentMatches(id)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load matches")
            }
        }
    }

    suspend fun registerForTournament(id: String, teamName: String, captainPhone: String): ApiResult<TournamentRegistrationResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.registerForTournament(id, TournamentRegistrationRequestDto(teamName, captainPhone))
                ApiResult.Success(response)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Tournament registration failed")
            }
        }
    }
}
