package com.sportynix.app.domain.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun search(
        query: String,
        activeFilter: String = "all",
        sportFilter: String = "all",
        sortBy: String = "relevance",
        latitude: Double? = null,
        longitude: Double? = null
    ): ApiResult<List<SearchResult>>

    suspend fun fetchPopularVenues(
        latitude: Double? = null,
        longitude: Double? = null
    ): ApiResult<List<SearchResult>>

    fun getRecentSearches(): Flow<List<String>>
    suspend fun addRecentSearch(query: String)
    suspend fun removeRecentSearch(query: String)
    suspend fun clearRecentSearches()
}
