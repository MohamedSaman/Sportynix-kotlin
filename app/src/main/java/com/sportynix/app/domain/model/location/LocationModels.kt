package com.sportynix.app.domain.model.location

import com.google.gson.annotations.SerializedName

data class LocationProvince(
    val id: Int,
    @SerializedName("source_id") val sourceId: Int,
    @SerializedName("name_en") val nameEn: String,
    @SerializedName("name_si") val nameSi: String?,
    @SerializedName("name_ta") val nameTa: String?
)

data class LocationDistrict(
    val id: Int,
    @SerializedName("source_id") val sourceId: Int,
    @SerializedName("province_id") val provinceId: Int,
    @SerializedName("province_name") val provinceName: String,
    @SerializedName("name_en") val nameEn: String,
    @SerializedName("name_si") val nameSi: String?,
    @SerializedName("name_ta") val nameTa: String?
)

data class LocationCity(
    val id: Int,
    @SerializedName("source_id") val sourceId: Int,
    @SerializedName("district_id") val districtId: Int,
    @SerializedName("district_name") val districtName: String,
    @SerializedName("province_id") val provinceId: Int,
    @SerializedName("province_name") val provinceName: String,
    @SerializedName("name_en") val nameEn: String,
    @SerializedName("name_si") val nameSi: String?,
    @SerializedName("name_ta") val nameTa: String?,
    @SerializedName("sub_name_en") val subNameEn: String?,
    @SerializedName("postcode") val postcode: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?
)
