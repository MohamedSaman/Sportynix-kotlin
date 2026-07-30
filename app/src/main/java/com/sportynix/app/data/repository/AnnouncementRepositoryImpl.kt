package com.sportynix.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.AnnouncementApiService
import com.sportynix.app.data.remote.dto.AnnouncementDto
import com.sportynix.app.domain.model.Announcement
import com.sportynix.app.domain.repository.AnnouncementRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementRepositoryImpl @Inject constructor(
    private val apiService: AnnouncementApiService,
    private val gson: Gson
) : AnnouncementRepository {

    override suspend fun getAnnouncements(): ApiResult<List<Announcement>> {
        return try {
            val response = apiService.getAnnouncements()
            if (response.isSuccessful && response.body() != null) {
                val jsonElement: JsonElement = response.body()!!
                val dtos = parseAnnouncements(jsonElement)
                val domainList = dtos.map { dto ->
                    Announcement(
                        id = dto.id,
                        title = dto.title ?: "Announcement",
                        subtitle = dto.subtitle ?: dto.shortDescription ?: "",
                        badge = dto.label ?: if (dto.isPinned == true) "Featured" else "Update",
                        isPinned = dto.isPinned ?: false,
                        bgColor = dto.fallbackBgColor ?: "#1a8553",
                        imageUrl = dto.imageUrl,
                        actionUrl = dto.actionUrl
                    )
                }
                ApiResult.Success(domainList)
            } else {
                ApiResult.Success(emptyList())
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch announcements")
            ApiResult.Success(emptyList())
        }
    }

    private fun parseAnnouncements(element: JsonElement): List<AnnouncementDto> {
        return try {
            val listType = object : TypeToken<List<AnnouncementDto>>() {}.type
            when {
                element.isJsonArray -> gson.fromJson(element, listType)
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    when {
                        obj.has("results") && obj.get("results").isJsonArray -> gson.fromJson(obj.get("results"), listType)
                        obj.has("data") && obj.get("data").isJsonArray -> gson.fromJson(obj.get("data"), listType)
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing announcements JSON")
            emptyList()
        }
    }
}
