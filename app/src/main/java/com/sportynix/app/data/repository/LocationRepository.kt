package com.sportynix.app.data.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.LocationApiService
import com.sportynix.app.domain.model.location.LocationCity
import com.sportynix.app.domain.model.location.LocationDistrict
import com.sportynix.app.domain.model.location.LocationProvince
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val apiService: LocationApiService
) {
    suspend fun getProvinces(): ApiResult<List<LocationProvince>> = safeApiCall {
        apiService.getProvinces()
    }

    suspend fun getDistricts(provinceId: Int): ApiResult<List<LocationDistrict>> = safeApiCall {
        apiService.getDistricts(provinceId)
    }

    suspend fun getCities(districtId: Int? = null, search: String? = null, pageSize: Int = 25): ApiResult<List<LocationCity>> = safeApiCall {
        apiService.getCities(districtId, search, pageSize).results
    }

    private suspend inline fun <T> safeApiCall(crossinline call: suspend () -> T): ApiResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                ApiResult.Success(call())
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Network error occurred")
            }
        }
    }
}
