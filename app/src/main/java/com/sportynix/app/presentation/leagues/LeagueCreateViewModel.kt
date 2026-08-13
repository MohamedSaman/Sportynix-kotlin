package com.sportynix.app.presentation.leagues

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.FullLeagueDto
import com.sportynix.app.data.repository.LeagueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class TeamInputState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val shortName: String = "",
    val jerseyColor: String = "#00D982"
)

data class LeagueCreateUiState(
    val currentStep: Int = 1,
    val isEditMode: Boolean = false,
    val leagueId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,

    // Step 1: Basic Info
    val name: String = "",
    val description: String = "",
    val sportType: String = "cricket", // cricket, football, etc.
    val format: String = "round_robin", // round_robin, knockout, group_knockout, league_playoff
    val logoUri: Uri? = null,
    val bannerUri: Uri? = null,

    // Step 2: Venue Selection
    val isVenueHosted: Boolean = false,
    val primaryVenueId: String = "",
    val customVenueText: String = "",

    // Step 3: Cricket & Sport Settings
    val cricketVariant: String = "softball", // softball, hardball
    val overs: Int = 20,
    val ballsPerOver: Int = 6, // Clamped between 4 and 6
    val powerplayOvers: Int = 6,
    val deathOvers: Int = 5,

    // Step 4: Team & Squad Configuration
    val numTeams: Int = 8,
    val squadSize: Int = 15,
    val minPlayers: Int = 11,
    val playingPlayersCount: Int = 11,
    val teamsList: List<TeamInputState> = List(8) { idx -> TeamInputState(name = "Team ${idx + 1}") },

    // Step 5: Schedule & Dates
    val registrationStart: String = "",
    val registrationEnd: String = "",
    val startDate: String = "",
    val endDate: String = "",

    // Step 6: Advanced Settings & Rules
    val prizePool: String = "",
    val rulesText: String = "",
    val isPublic: Boolean = true,
    val isFeatured: Boolean = false,
    val contactEmail: String = "",
    val contactPhone: String = ""
)

@HiltViewModel
class LeagueCreateViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeagueCreateUiState())
    val uiState: StateFlow<LeagueCreateUiState> = _uiState.asStateFlow()

    fun initForEdit(leagueId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isEditMode = true, leagueId = leagueId)
            when (val res = leagueRepository.getLeagueDetail(leagueId)) {
                is ApiResult.Success -> {
                    val l = res.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        name = l.name,
                        description = l.description ?: "",
                        sportType = l.sportType,
                        format = l.format,
                        cricketVariant = l.cricketVariant ?: "softball",
                        numTeams = l.numTeams,
                        squadSize = l.squadSize ?: 15,
                        minPlayers = l.minPlayers ?: 11,
                        playingPlayersCount = l.playingPlayersCount ?: 11,
                        isVenueHosted = l.isVenueHosted ?: false,
                        primaryVenueId = l.primaryVenue?.id ?: "",
                        registrationStart = l.registrationStart ?: "",
                        registrationEnd = l.registrationEnd ?: "",
                        startDate = l.startDate ?: "",
                        endDate = l.endDate ?: "",
                        prizePool = l.prizePool ?: "",
                        rulesText = l.rulesText ?: "",
                        isPublic = l.isPublic ?: true,
                        isFeatured = l.isFeatured ?: false,
                        overs = l.cricketConfig?.overs ?: 20,
                        ballsPerOver = (l.cricketConfig?.ballsPerOver ?: 6).coerceIn(4, 6)
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun nextStep() {
        val next = (_uiState.value.currentStep + 1).coerceAtMost(6)
        _uiState.value = _uiState.value.copy(currentStep = next)
    }

    fun prevStep() {
        val prev = (_uiState.value.currentStep - 1).coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(currentStep = prev)
    }

    fun updateBasicInfo(name: String, description: String, sportType: String, format: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            description = description,
            sportType = sportType,
            format = format
        )
    }

    fun updateLogoUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(logoUri = uri)
    }

    fun updateBannerUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(bannerUri = uri)
    }

    fun updateVenueConfig(isHosted: Boolean, venueId: String, customText: String) {
        _uiState.value = _uiState.value.copy(
            isVenueHosted = isHosted,
            primaryVenueId = venueId,
            customVenueText = customText
        )
    }

    fun updateCricketSettings(variant: String, overs: Int, ballsPerOver: Int, powerplay: Int, death: Int) {
        // Enforce clamping balls per over between 4 and 6
        val clampedBalls = ballsPerOver.coerceIn(4, 6)
        _uiState.value = _uiState.value.copy(
            cricketVariant = variant,
            overs = overs,
            ballsPerOver = clampedBalls,
            powerplayOvers = powerplay,
            deathOvers = death
        )
    }

    fun updateNumTeams(count: Int) {
        val currentTeams = _uiState.value.teamsList.toMutableList()
        if (count > currentTeams.size) {
            for (i in currentTeams.size until count) {
                currentTeams.add(TeamInputState(name = "Team ${i + 1}"))
            }
        } else if (count < currentTeams.size) {
            while (currentTeams.size > count && currentTeams.size > 2) {
                currentTeams.removeAt(currentTeams.size - 1)
            }
        }
        _uiState.value = _uiState.value.copy(numTeams = currentTeams.size, teamsList = currentTeams)
    }

    fun updateTeamItem(index: Int, name: String, shortName: String, jerseyColor: String) {
        val list = _uiState.value.teamsList.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(name = name, shortName = shortName, jerseyColor = jerseyColor)
            _uiState.value = _uiState.value.copy(teamsList = list)
        }
    }

    fun updateDates(regStart: String, regEnd: String, start: String, end: String) {
        _uiState.value = _uiState.value.copy(
            registrationStart = regStart,
            registrationEnd = regEnd,
            startDate = start,
            endDate = end
        )
    }

    fun updateAdvanced(prizePool: String, rulesText: String, isPublic: Boolean, isFeatured: Boolean) {
        _uiState.value = _uiState.value.copy(
            prizePool = prizePool,
            rulesText = rulesText,
            isPublic = isPublic,
            isFeatured = isFeatured
        )
    }

    fun submitLeague(context: Context) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "League name is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val payloadJson = JsonObject().apply {
                addProperty("name", state.name)
                addProperty("description", state.description)
                addProperty("sport_type", state.sportType)
                addProperty("cricket_variant", state.cricketVariant)
                addProperty("format", state.format)
                addProperty("num_teams", state.numTeams)
                addProperty("squad_size", state.squadSize)
                addProperty("min_players", state.minPlayers)
                addProperty("playing_players_count", state.playingPlayersCount)
                addProperty("is_venue_hosted", state.isVenueHosted)
                if (state.isVenueHosted && state.primaryVenueId.isNotBlank()) {
                    addProperty("primary_venue_id", state.primaryVenueId)
                }
                if (state.customVenueText.isNotBlank()) {
                    addProperty("custom_venue_text", state.customVenueText)
                }
                if (state.sportType == "cricket") {
                    val cricketConfig = JsonObject().apply {
                        addProperty("overs", state.overs)
                        addProperty("powerplay_overs", state.powerplayOvers)
                        addProperty("death_overs", state.deathOvers)
                        addProperty("balls_per_over", state.ballsPerOver.coerceIn(4, 6))
                    }
                    add("cricket_config", cricketConfig)
                }
                if (state.registrationStart.isNotBlank()) addProperty("registration_start", state.registrationStart)
                if (state.registrationEnd.isNotBlank()) addProperty("registration_end", state.registrationEnd)
                if (state.startDate.isNotBlank()) addProperty("start_date", state.startDate)
                if (state.endDate.isNotBlank()) addProperty("end_date", state.endDate)
                if (state.prizePool.isNotBlank()) addProperty("prize_pool", state.prizePool)
                if (state.rulesText.isNotBlank()) addProperty("rules_text", state.rulesText)
                addProperty("is_public", state.isPublic)
                addProperty("is_featured", state.isFeatured)

                val teamsArray = JsonArray()
                state.teamsList.forEach { teamInput ->
                    if (teamInput.name.isNotBlank()) {
                        val teamObj = JsonObject().apply {
                            addProperty("name", teamInput.name)
                            if (teamInput.shortName.isNotBlank()) addProperty("short_name", teamInput.shortName)
                            if (teamInput.jerseyColor.isNotBlank()) addProperty("jersey_color", teamInput.jerseyColor)
                        }
                        teamsArray.add(teamObj)
                    }
                }
                add("teams", teamsArray)
            }

            val dataBody = gson.toJson(payloadJson).toRequestBody("application/json".toMediaTypeOrNull())
            val logoPart = state.logoUri?.let { uriToMultipart(context, it, "logo") }
            val bannerPart = state.bannerUri?.let { uriToMultipart(context, it, "banner") }

            val result = if (state.isEditMode && state.leagueId != null) {
                leagueRepository.updateLeague(state.leagueId, dataBody, logoPart, bannerPart)
            } else {
                leagueRepository.createLeague(dataBody, logoPart, bannerPart)
            }

            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message ?: "Submission failed")
                }
                else -> {}
            }
        }
    }

    private fun uriToMultipart(context: Context, uri: Uri, partName: String): MultipartBody.Part? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            val reqFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData(partName, tempFile.name, reqFile)
        } catch (e: Exception) {
            null
        }
    }
}
