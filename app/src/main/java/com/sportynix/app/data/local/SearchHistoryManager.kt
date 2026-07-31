package com.sportynix.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("nearby_venues_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val PREF_KEY = "nearby_venues_search_history_v1"

    fun getSearchHistory(): List<String> {
        val json = prefs.getString(PREF_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun pushSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val current = getSearchHistory().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val trimmedList = if (current.size > 8) current.take(8) else current
        prefs.edit().putString(PREF_KEY, gson.toJson(trimmedList)).apply()
    }

    fun removeSearchHistoryItem(item: String) {
        val current = getSearchHistory().toMutableList()
        current.removeAll { it == item }
        prefs.edit().putString(PREF_KEY, gson.toJson(current)).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove(PREF_KEY).apply()
    }
}
