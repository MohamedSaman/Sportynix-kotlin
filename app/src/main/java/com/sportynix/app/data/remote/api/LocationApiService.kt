package com.sportynix.app.data.remote.api

import com.sportynix.app.domain.model.location.LocationCity
import com.sportynix.app.domain.model.location.LocationDistrict
import com.sportynix.app.domain.model.location.LocationProvince
import retrofit2.http.GET
import retrofit2.http.Query

interface LocationApiService {
    @GET("api/locations/provinces/")
    suspend fun getProvinces(): List<LocationProvince>

    @GET("api/locations/districts/")
    suspend fun getDistricts(@Query("province_id") provinceId: Int): List<LocationDistrict>

    @GET("api/locations/cities/")
    suspend fun getCities(
        @Query("district_id") districtId: Int? = null,
        @Query("search") search: String? = null,
        @Query("page_size") pageSize: Int = 25
    ): LocationCityResponse
}

data class LocationCityResponse(
    val results: List<LocationCity>
)
