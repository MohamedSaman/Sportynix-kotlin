package com.sportynix.app.domain.model

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sportynix.app.BuildConfig
import com.sportynix.app.data.remote.dto.NotificationDto
import java.util.UUID

enum class ContentSource { ANNOUNCEMENT, NOTIFICATION }

data class AnnouncementDetailPayload(
    val id: String,
    val source: ContentSource,
    val title: String,
    val subtitle: String? = null,
    val shortDescription: String? = null,
    val fullDescription: String? = null,
    val imageUrl: String? = null,
    val fallbackBgColor: String? = null,
    val label: String? = null,
    val publishedAt: String? = null,
    val readMoreUrl: String? = null,
    val readMoreLabel: String? = null
)

object ContentPayloadBuilder {
    private fun text(value: Any?): String? = when (value) {
        null -> null
        is JsonElement -> jsonText(value)
        else -> value.toString().trim().takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun jsonText(value: JsonElement?): String? {
        if (value == null || value.isJsonNull) return null
        if (value.isJsonPrimitive) return value.asJsonPrimitive.let {
            when { it.isString -> it.asString; it.isNumber -> it.asNumber.toString(); it.isBoolean -> it.asBoolean.toString(); else -> null }
        }?.trim()?.takeIf(String::isNotEmpty)
        if (value is JsonArray) return value.firstOrNull()?.let(::jsonText)
        if (value is JsonObject) return listOf("url", "image_url", "src").firstNotNullOfOrNull { jsonText(value.get(it)) }
        return null
    }

    private fun first(vararg values: Any?): String? = values.firstNotNullOfOrNull(::text)
    private fun JsonObject.first(vararg keys: String): String? = keys.firstNotNullOfOrNull { jsonText(get(it)) }

    fun normalizeMediaUrl(raw: String?): String? {
        val value = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (value.startsWith("http://api.sportynix.com")) return "https://" + value.removePrefix("http://")
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        return BuildConfig.BASE_URL.trimEnd('/') + "/" + value.trimStart('/')
    }

    fun fromAnnouncement(item: JsonObject): AnnouncementDetailPayload = AnnouncementDetailPayload(
        id = item.first("id") ?: "announcement_${UUID.randomUUID()}",
        source = ContentSource.ANNOUNCEMENT,
        title = item.first("title", "heading", "name") ?: "Announcement",
        subtitle = item.first("subtitle"),
        shortDescription = item.first("short_description", "shortDescription", "summary", "message"),
        fullDescription = item.first("full_description", "fullDescription", "description", "message"),
        imageUrl = normalizeMediaUrl(item.first("image_url", "image", "banner", "photo")),
        fallbackBgColor = item.first("fallback_bg_color", "background_color"),
        label = item.first("label", "badge"),
        publishedAt = item.first("created_at", "published_at"),
        readMoreUrl = item.first("read_more_url", "readMoreUrl", "link"),
        readMoreLabel = item.first("read_more_label", "readMoreLabel")
    )

    fun fromNotification(notification: NotificationDto): AnnouncementDetailPayload? {
        val d = notification.data.orEmpty()
        fun pick(vararg keys: String) = keys.firstNotNullOfOrNull { text(d[it]) }
        val type = notification.type.orEmpty().lowercase()
        val rich = first(pick("full_description", "description", "short_description", "image_url", "imageUrl", "banner", "announcement_id"), notification.message)
        if (rich == null && !type.contains("announcement") && !type.contains("feature")) return null
        return AnnouncementDetailPayload(
            id = first(pick("content_id", "announcement_id"), notification.id) ?: "notification_${UUID.randomUUID()}",
            source = ContentSource.NOTIFICATION,
            title = first(pick("title", "heading"), notification.title) ?: "Notification",
            subtitle = pick("subtitle"),
            shortDescription = first(pick("short_description", "shortDescription", "summary"), notification.message),
            fullDescription = first(pick("full_description", "fullDescription", "description", "message"), notification.message),
            imageUrl = normalizeMediaUrl(pick("image_url", "imageUrl", "image", "banner", "photo", "bigPicture", "attachments")),
            fallbackBgColor = pick("fallback_bg_color", "background_color"),
            label = pick("label", "badge", "category"),
            publishedAt = pick("published_at", "created_at"),
            readMoreUrl = pick("read_more_url", "readMoreUrl", "link", "url", "deep_link_url"),
            readMoreLabel = pick("read_more_label", "readMoreLabel")
        )
    }

    fun value(data: Map<String, Any>?, vararg keys: String): String? = keys.firstNotNullOfOrNull { text(data?.get(it)) }
    fun intValue(data: Map<String, Any>?, vararg keys: String): Int? = value(data, *keys)?.toDoubleOrNull()?.toInt()
}
