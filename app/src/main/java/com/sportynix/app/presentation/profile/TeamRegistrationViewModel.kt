package com.sportynix.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sportynix.app.data.remote.api.LeagueApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamRegistrationState(
    val teamName: String = "",
    val shortName: String = "",
    val captainName: String = "",
    val captainEmail: String = "",
    val captainPhone: String = "",
    val homeGround: String = "",
    val notes: String = "",
    val submitting: Boolean = false,
    val success: String? = null,
    val error: String? = null
)

@HiltViewModel
class TeamRegistrationViewModel @Inject constructor(private val api: LeagueApiService) : ViewModel() {
    private val _state = MutableStateFlow(TeamRegistrationState())
    val state = _state.asStateFlow()
    fun update(transform: (TeamRegistrationState) -> TeamRegistrationState) { _state.value = transform(_state.value).copy(error = null) }
    fun clearMessage() { _state.value = _state.value.copy(error = null, success = null) }
    fun submit(leagueId: String) {
        val s = _state.value
        val error = when {
            s.teamName.trim().length < 3 -> "Team name must be at least 3 characters"
            s.shortName.trim().isEmpty() -> "Short name is required"
            s.shortName.trim().length > 5 -> "Short name must be 5 characters or less"
            s.captainName.trim().isEmpty() -> "Captain name is required"
            s.captainEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(s.captainEmail.trim()).matches() -> "Invalid email format"
            s.captainPhone.isNotBlank() && !s.captainPhone.matches(Regex("^\\+?[0-9\\s-]{10,}$")) -> "Invalid phone number"
            else -> null
        }
        if (error != null) { _state.value = s.copy(error = error); return }
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
                if (!response.isSuccessful) throw IllegalStateException(response.errorBody()?.string()?.take(240) ?: "Registration failed (${response.code()})")
                _state.value = _state.value.copy(submitting = false, success = "Your team registration has been submitted for approval.")
            } catch (e: Exception) { _state.value = _state.value.copy(submitting = false, error = e.message ?: "Registration failed") }
        }
    }
}
