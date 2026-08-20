package com.sportynix.app.presentation.profile

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sportynix.app.data.remote.api.LeagueApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class TeamRegistrationState(
    val teamName: String = "",
    val shortName: String = "",
    val captainName: String = "",
    val captainEmail: String = "",
    val captainPhone: String = "",
    val homeGround: String = "",
    val notes: String = "",
    val logoUri: Uri? = null,
    val submitting: Boolean = false,
    val success: String? = null,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class TeamRegistrationViewModel @Inject constructor(
    private val api: LeagueApiService
) : ViewModel() {
    private val _state = MutableStateFlow(TeamRegistrationState())
    val state = _state.asStateFlow()

    fun update(transform: (TeamRegistrationState) -> TeamRegistrationState) {
        _state.value = transform(_state.value).copy(error = null)
    }

    fun setLogoUri(uri: Uri?) {
        _state.value = _state.value.copy(logoUri = uri)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(error = null, success = null)
    }

    fun validate(): Boolean {
        val s = _state.value
        val errors = mutableMapOf<String, String>()

        if (s.teamName.trim().isEmpty()) {
            errors["teamName"] = "Team name is required"
        } else if (s.teamName.trim().length < 3) {
            errors["teamName"] = "Team name must be at least 3 characters"
        }

        if (s.shortName.trim().isEmpty()) {
            errors["shortName"] = "Short name is required"
        } else if (s.shortName.trim().length > 5) {
            errors["shortName"] = "Short name must be 5 characters or less"
        }

        if (s.captainName.trim().isEmpty()) {
            errors["captainName"] = "Captain name is required"
        }

        if (s.captainEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(s.captainEmail.trim()).matches()) {
            errors["captainEmail"] = "Invalid email format"
        }

        if (s.captainPhone.isNotBlank() && !s.captainPhone.matches(Regex("^\\+?[0-9\\s-]{10,}$"))) {
            errors["captainPhone"] = "Invalid phone number"
        }

        _state.value = s.copy(fieldErrors = errors, error = if (errors.isNotEmpty()) "Please fix validation errors below." else null)
        return errors.isEmpty()
    }

    fun submit(leagueId: String) {
        if (!validate()) return
        val s = _state.value
        if (s.submitting) return

        _state.value = s.copy(submitting = true, error = null)
        viewModelScope.launch {
            try {
                val body = JsonObject().apply {
                    addProperty("team_id", s.teamName.trim().lowercase().replace(Regex("\\s+"), "-"))
                    addProperty("team_name_override", s.teamName.trim())
                    addProperty("team_short_name", s.shortName.trim().uppercase())
                    if (s.captainName.isNotBlank()) addProperty("captain_name", s.captainName.trim())
                    if (s.captainEmail.isNotBlank()) addProperty("captain_email", s.captainEmail.trim())
                    if (s.captainPhone.isNotBlank()) addProperty("captain_phone", s.captainPhone.trim())
                    if (s.homeGround.isNotBlank()) addProperty("home_ground", s.homeGround.trim())
                    if (s.notes.isNotBlank()) addProperty("notes", s.notes.trim())
                }
                val response = api.registerTeam(leagueId, body)
                if (!response.isSuccessful) {
                    throw IllegalStateException(response.errorBody()?.string()?.take(240) ?: "Registration failed (${response.code()})")
                }
                _state.value = _state.value.copy(
                    submitting = false,
                    success = "Your team registration has been submitted for approval. You will be notified once reviewed."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(submitting = false, error = e.message ?: "Registration failed")
            }
        }
    }
}
