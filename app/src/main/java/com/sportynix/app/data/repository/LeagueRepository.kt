package com.sportynix.app.data.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.LeagueApiService
import com.sportynix.app.data.remote.dto.FixtureDto
import com.sportynix.app.data.remote.dto.LeagueDto
import com.sportynix.app.data.remote.dto.StandingDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeagueRepository @Inject constructor(
    private val apiService: LeagueApiService
) {
    suspend fun getLeagues(search: String? = null, sport: String? = null): ApiResult<List<LeagueDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeagues(search, sport)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load leagues")
            }
        }
    }

    suspend fun getLeagueDetail(leagueId: String): ApiResult<LeagueDto> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeagueDetail(leagueId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load league details")
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

    suspend fun getLeagueStandings(leagueId: String): ApiResult<List<StandingDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getLeagueStandings(leagueId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load standings")
            }
        }
    }
}
