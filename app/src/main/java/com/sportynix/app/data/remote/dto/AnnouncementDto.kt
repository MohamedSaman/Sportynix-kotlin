package com.sportynix.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnnouncementDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("short_description") val shortDescription: String?,
    @SerializedName("label") val label: String?,
    @SerializedName("is_pinned") val isPinned: Boolean?,
    @SerializedName("fallback_bg_color") val fallbackBgColor: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("action_url") val actionUrl: String?
)
