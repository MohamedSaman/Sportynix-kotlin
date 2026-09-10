package com.sportynix.app.presentation.tournaments

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.CricketConfigDto
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.data.repository.TournamentRepository
import com.sportynix.app.data.repository.VenueRepository
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TournamentCreateUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 4,
    val name: String = "",
    val description: String = "",
    val bannerUri: Uri? = null,
    val sportType: String = "cricket",
    val cricketVariant: String = "softball", // softball, hardball
    val overs: String = "20",
    val powerplayOvers: String = "6",
    val ballsPerOver: String = "6",
    val format: String = "knockout", // knockout, group_knockout, round_robin, swiss, custom
    val teamCapacity: String = "8",
    val rosterLimit: String = "15",
    val minRoster: String = "7",
    val playingPlayersCount: String = "11",
    val venueCategory: String = "indoor",
    val venueSearch: String = "",
    val venues: List<VenueDto> = emptyList(),
    val selectedVenue: VenueDto? = null,
    val customVenueText: String = "",
    val isVenueHosted: Boolean = false,
    val loadingVenues: Boolean = false,
    val applicationOpenDate: String? = null, // YYYY-MM-DD
    val applicationCloseDate: String? = null, // YYYY-MM-DD
    val tournamentStartDate: String? = null, // YYYY-MM-DD
    val tournamentEndDate: String? = null, // YYYY-MM-DD
    val isSubmitting: Boolean = false,
    val createdTournamentId: String? = null,
    val error: String? = null
)

@HiltViewModel
class TournamentCreateViewModel @Inject constructor(
    private val tournamentRepository: TournamentRepository,
    private val venueRepository: VenueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentCreateUiState())
    val uiState: StateFlow<TournamentCreateUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        val todayStr = dateFormat.format(Date())
        _uiState.value = _uiState.value.copy(
            applicationOpenDate = todayStr
        )
        loadVenues()
    }

    fun setStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step.coerceIn(1, _uiState.value.totalSteps), error = null)
    }

    fun nextStep(): Boolean {
        if (!validateCurrentStep()) return false
        _uiState.value = _uiState.value.copy(
            currentStep = (_uiState.value.currentStep + 1).coerceAtMost(_uiState.value.totalSteps),
            error = null
        )
        return true
    }

    fun prevStep() {
        _uiState.value = _uiState.value.copy(
            currentStep = (_uiState.value.currentStep - 1).coerceAtLeast(1),
            error = null
        )
    }

    fun updateBasics(
        name: String,
        description: String,
        sportType: String,
        cricketVariant: String,
        bannerUri: Uri?
    ) {
        _uiState.value = _uiState.value.copy(
            name = name,
            description = description,
            sportType = sportType,
            cricketVariant = cricketVariant,
            bannerUri = bannerUri
        )
    }

    fun updateSetup(
        format: String,
        teamCapacity: String,
        rosterLimit: String,
        minRoster: String,
        playingPlayersCount: String,
        overs: String = _uiState.value.overs,
        powerplayOvers: String = _uiState.value.powerplayOvers,
        ballsPerOver: String = _uiState.value.ballsPerOver
    ) {
        val oversVal = overs.toIntOrNull() ?: 20
        val ppVal = (powerplayOvers.toIntOrNull() ?: 6).coerceAtMost(oversVal)

        _uiState.value = _uiState.value.copy(
            format = format,
            teamCapacity = teamCapacity,
            rosterLimit = rosterLimit,
            minRoster = minRoster,
            playingPlayersCount = playingPlayersCount,
            overs = oversVal.toString(),
            powerplayOvers = ppVal.toString(),
            ballsPerOver = ballsPerOver
        )
    }

    fun updateVenueConfig(
        selectedVenue: VenueDto?,
        customVenueText: String,
        isVenueHosted: Boolean,
        venueCategory: String = _uiState.value.venueCategory
    ) {
        _uiState.value = _uiState.value.copy(
            selectedVenue = selectedVenue,
            customVenueText = customVenueText,
            isVenueHosted = isVenueHosted,
            venueCategory = venueCategory
        )
    }

    fun searchVenues(query: String) {
        _uiState.value = _uiState.value.copy(venueSearch = query)
        loadVenues(query)
    }

    fun setVenueCategory(category: String) {
        _uiState.value = _uiState.value.copy(venueCategory = category)
        loadVenues(_uiState.value.venueSearch, category)
    }

    fun updateDates(
        applicationOpen: String?,
        applicationClose: String?,
        tournamentStart: String?,
        tournamentEnd: String?
    ) {
        _uiState.value = _uiState.value.copy(
            applicationOpenDate = applicationOpen,
            applicationCloseDate = applicationClose,
            tournamentStartDate = tournamentStart,
            tournamentEndDate = tournamentEnd
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun loadVenues(search: String? = null, category: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingVenues = true)
            when (val res = venueRepository.getVenues(search = search, sportType = null)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        venues = res.data,
                        loadingVenues = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(loadingVenues = false)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(loadingVenues = false)
                }
            }
        }
    }

    private fun validateCurrentStep(): Boolean {
        val s = _uiState.value
        when (s.currentStep) {
            1 -> {
                if (s.name.trim().isEmpty()) {
                    _uiState.value = s.copy(error = "Please enter a tournament name.")
                    return false
                }
            }
            2 -> {
                val cap = s.teamCapacity.toIntOrNull()
                val roster = s.rosterLimit.toIntOrNull()
                val minR = s.minRoster.toIntOrNull()
                val playing = s.playingPlayersCount.toIntOrNull()

                if (cap == null || cap < 2) {
                    _uiState.value = s.copy(error = "Team capacity must be at least 2 teams.")
                    return false
                }
                if (roster == null || minR == null || playing == null) {
                    _uiState.value = s.copy(error = "Please enter valid numeric values for squad setup.")
                    return false
                }
                if (minR > roster) {
                    _uiState.value = s.copy(error = "Minimum players cannot exceed maximum squad size.")
                    return false
                }
                if (playing > roster) {
                    _uiState.value = s.copy(error = "Playing players count cannot exceed maximum squad size.")
                    return false
                }
                if (playing < minR) {
                    _uiState.value = s.copy(error = "Playing players count must be at least equal to minimum players.")
                    return false
                }
            }
            3 -> {
                if (s.selectedVenue == null && s.customVenueText.trim().isEmpty()) {
                    _uiState.value = s.copy(error = "Please select a venue or enter a custom venue name.")
                    return false
                }
                if (s.applicationOpenDate.isNullOrBlank() || s.applicationCloseDate.isNullOrBlank() ||
                    s.tournamentStartDate.isNullOrBlank() || s.tournamentEndDate.isNullOrBlank()
                ) {
                    _uiState.value = s.copy(error = "Please select all application and tournament dates.")
                    return false
                }

                try {
                    val appOpen = dateFormat.parse(s.applicationOpenDate)
                    val appClose = dateFormat.parse(s.applicationCloseDate)
                    val tourStart = dateFormat.parse(s.tournamentStartDate)
                    val tourEnd = dateFormat.parse(s.tournamentEndDate)

                    if (appOpen != null && appClose != null && appOpen.after(appClose)) {
                        _uiState.value = s.copy(error = "Application opening date must be before deadline.")
                        return false
                    }
                    if (appClose != null && tourStart != null && appClose.after(tourStart)) {
                        _uiState.value = s.copy(error = "Application deadline must be on or before tournament start date.")
                        return false
                    }
                    if (tourStart != null && tourEnd != null && tourStart.after(tourEnd)) {
                        _uiState.value = s.copy(error = "Tournament start date must be before end date.")
                        return false
                    }
                } catch (e: Exception) {
                    _uiState.value = s.copy(error = "Invalid date format.")
                    return false
                }
            }
        }
        _uiState.value = s.copy(error = null)
        return true
    }

    fun createTournament(context: Context) {
        if (!validateCurrentStep()) return

        val s = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

            val teamCap = s.teamCapacity.toIntOrNull() ?: 8
            val rosterLimit = s.rosterLimit.toIntOrNull() ?: 15
            val minRoster = s.minRoster.toIntOrNull() ?: 7
            val playingCount = s.playingPlayersCount.toIntOrNull() ?: 11

            val oversVal = s.overs.toIntOrNull() ?: 20
            val ppVal = s.powerplayOvers.toIntOrNull() ?: 6
            val bpoVal = s.ballsPerOver.toIntOrNull() ?: 6

            val cricketConfig = if (s.sportType == "cricket") {
                JsonObject().apply {
                    addProperty("overs", oversVal)
                    addProperty("powerplay_overs", ppVal)
                    addProperty("balls_per_over", bpoVal)
                }
            } else null

            val bannerFile = s.bannerUri?.let { uri -> uriToFile(context, uri) }
            val bannerPart = bannerFile?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("banner", file.name, requestFile)
            }

            if (bannerPart != null) {
                val partMap = mutableMapOf<String, okhttp3.RequestBody>()
                partMap["name"] = s.name.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                if (s.description.isNotBlank()) partMap["description"] = s.description.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                partMap["sport_type"] = s.sportType.toRequestBody("text/plain".toMediaTypeOrNull())
                if (s.sportType == "cricket") {
                    partMap["cricket_variant"] = s.cricketVariant.toRequestBody("text/plain".toMediaTypeOrNull())
                    cricketConfig?.let { partMap["cricket_config"] = it.toString().toRequestBody("text/plain".toMediaTypeOrNull()) }
                }
                partMap["format"] = s.format.toRequestBody("text/plain".toMediaTypeOrNull())
                partMap["team_capacity"] = teamCap.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                partMap["roster_size_limit"] = rosterLimit.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                partMap["min_roster_size"] = minRoster.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                partMap["playing_players_count"] = playingCount.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                partMap["is_venue_hosted"] = s.isVenueHosted.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                s.selectedVenue?.let { v -> partMap["venue_ids"] = "[\"${v.id}\"]".toRequestBody("text/plain".toMediaTypeOrNull()) }
                if (s.selectedVenue == null && s.customVenueText.isNotBlank()) {
                    partMap["custom_venue_text"] = s.customVenueText.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                }

                s.applicationOpenDate?.let { partMap["application_open_date"] = "${it}T00:00:00Z".toRequestBody("text/plain".toMediaTypeOrNull()) }
                s.applicationCloseDate?.let { partMap["application_deadline"] = "${it}T23:59:59Z".toRequestBody("text/plain".toMediaTypeOrNull()) }
                s.tournamentStartDate?.let { partMap["start_date"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
                s.tournamentEndDate?.let { partMap["end_date"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
                partMap["is_public"] = "true".toRequestBody("text/plain".toMediaTypeOrNull())

                when (val result = tournamentRepository.createTournamentMultipart(partMap, bannerPart)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            createdTournamentId = result.data.id
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isSubmitting = false, error = result.message)
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(isSubmitting = false)
                    }
                }
            } else {
                val json = JsonObject().apply {
                    addProperty("name", s.name.trim())
                    if (s.description.isNotBlank()) addProperty("description", s.description.trim())
                    addProperty("sport_type", s.sportType)
                    if (s.sportType == "cricket") {
                        addProperty("cricket_variant", s.cricketVariant)
                        cricketConfig?.let { add("cricket_config", it) }
                    }
                    addProperty("format", s.format)
                    addProperty("team_capacity", teamCap)
                    addProperty("roster_size_limit", rosterLimit)
                    addProperty("min_roster_size", minRoster)
                    addProperty("playing_players_count", playingCount)
                    addProperty("is_venue_hosted", s.isVenueHosted)

                    if (s.selectedVenue != null) {
                        val arr = com.google.gson.JsonArray().apply { add(s.selectedVenue.id) }
                        add("venue_ids", arr)
                    } else if (s.customVenueText.isNotBlank()) {
                        addProperty("custom_venue_text", s.customVenueText.trim())
                    }

                    s.applicationOpenDate?.let { addProperty("application_open_date", "${it}T00:00:00Z") }
                    s.applicationCloseDate?.let { addProperty("application_deadline", "${it}T23:59:59Z") }
                    s.tournamentStartDate?.let { addProperty("start_date", it) }
                    s.tournamentEndDate?.let { addProperty("end_date", it) }
                    addProperty("is_public", true)
                }

                when (val result = tournamentRepository.createTournament(json)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            createdTournamentId = result.data.id
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isSubmitting = false, error = result.message)
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(isSubmitting = false)
                    }
                }
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val tempFile = File.createTempFile("tournament_banner_", ".jpg", context.cacheDir)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
