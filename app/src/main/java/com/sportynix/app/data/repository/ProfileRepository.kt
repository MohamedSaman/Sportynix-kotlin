package com.sportynix.app.data.repository

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.sportynix.app.data.remote.api.UserApiService
import com.sportynix.app.data.remote.dto.APIFavoriteDto
import com.sportynix.app.data.remote.dto.BlockedUserDto
import com.sportynix.app.data.remote.dto.EmailChangeRequestDto
import com.sportynix.app.data.remote.dto.EmailVerifyNewRequestDto
import com.sportynix.app.data.remote.dto.LocationCityDto
import com.sportynix.app.data.remote.dto.LocationDistrictDto
import com.sportynix.app.data.remote.dto.LocationProvinceDto
import com.sportynix.app.data.remote.dto.PasswordChangeRequestDto
import com.sportynix.app.data.remote.dto.PhoneOtpSendRequestDto
import com.sportynix.app.data.remote.dto.PhoneOtpSendResponseDto
import com.sportynix.app.data.remote.dto.PhoneOtpVerifyDto
import com.sportynix.app.data.remote.dto.PointsHistoryResponseDto
import com.sportynix.app.data.remote.dto.ReferralResponseDto
import com.sportynix.app.data.remote.dto.ReportItemDto
import com.sportynix.app.data.remote.dto.UpdateAllowDirectTeamAddRequestDto
import com.sportynix.app.data.remote.dto.UserDataDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val apiService: UserApiService,
    private val gson: Gson
) {
    private val _currentUser = MutableStateFlow<UserDataDto?>(null)
    val currentUser: StateFlow<UserDataDto?> = _currentUser.asStateFlow()

    fun updateCachedUser(user: UserDataDto) {
        _currentUser.value = user
    }

    suspend fun fetchProfile(): Result<UserDataDto> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                _currentUser.value = user
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to fetch profile"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        context: Context,
        username: String,
        firstName: String,
        lastName: String,
        gender: String,
        dateOfBirth: String,
        phone: String,
        bio: String,
        address: String,
        homeDistrict: String? = null,
        homeCity: String? = null,
        homeCityId: Int? = null,
        sportsPreferences: List<String>,
        availability: String,
        isPublicProfile: Boolean,
        isShowContact: Boolean,
        cricketPreferredVariant: String,
        cricketPrimaryRole: String,
        cricketPlayingPosition: String,
        cricketBattingStyle: String,
        cricketBowlingStyle: String,
        cricketJerseyNumber: String,
        imageUri: Uri? = null,
        removeImage: Boolean = false
    ): Result<UserDataDto> {
        return try {
            val isImageChanged = imageUri != null || removeImage

            if (isImageChanged) {
                val parts = mutableMapOf<String, RequestBody>()
                fun addPart(key: String, value: String) {
                    parts[key] = value.toRequestBody("text/plain".toMediaTypeOrNull())
                }

                addPart("username", username)
                addPart("first_name", firstName)
                addPart("last_name", lastName)
                addPart("gender", gender)
                addPart("date_of_birth", dateOfBirth)
                addPart("phone_number", phone)
                addPart("bio", bio)
                addPart("address", address)
                if (!homeDistrict.isNullOrEmpty()) addPart("home_district", homeDistrict)
                if (!homeCity.isNullOrEmpty()) addPart("home_city", homeCity)
                if (homeCityId != null) addPart("home_city_id", homeCityId.toString())
                addPart("availability", availability)
                addPart("is_public_profile", isPublicProfile.toString())
                addPart("is_show_contact", isShowContact.toString())
                addPart("sports_preferences", gson.toJson(sportsPreferences))

                addPart("cricket_preferred_variant", cricketPreferredVariant)
                addPart("cricket_primary_role", cricketPrimaryRole)
                addPart("cricket_playing_position", cricketPlayingPosition)
                addPart("cricket_batting_style", cricketBattingStyle)
                addPart("cricket_bowling_style", cricketBowlingStyle)
                addPart("cricket_jersey_number", cricketJerseyNumber)

                var photoPart: MultipartBody.Part? = null
                if (imageUri != null) {
                    val file = uriToFile(context, imageUri)
                    if (file != null) {
                        val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        photoPart = MultipartBody.Part.createFormData("profile_picture", file.name, reqFile)
                    }
                } else if (removeImage) {
                    addPart("profile_picture", "")
                }

                val response = apiService.updateProfileMultipart(parts, photoPart)
                if (response.isSuccessful && response.body() != null) {
                    val updated = response.body()!!
                    _currentUser.value = updated
                    Result.success(updated)
                } else {
                    Result.failure(Exception(parseBackendError(response.errorBody()?.string(), "Failed to update profile")))
                }
            } else {
                val body = mutableMapOf<String, Any>(
                    "username" to username,
                    "first_name" to firstName,
                    "last_name" to lastName,
                    "gender" to gender,
                    "date_of_birth" to dateOfBirth,
                    "phone_number" to phone,
                    "bio" to bio,
                    "address" to address,
                    "sports_preferences" to sportsPreferences,
                    "availability" to availability,
                    "is_public_profile" to isPublicProfile,
                    "is_show_contact" to isShowContact,
                    "cricket_playing_position" to cricketPlayingPosition
                )

                if (!homeDistrict.isNullOrEmpty()) body["home_district"] = homeDistrict
                if (!homeCity.isNullOrEmpty()) body["home_city"] = homeCity
                if (homeCityId != null) body["home_city_id"] = homeCityId

                body["cricket_preferred_variant"] = cricketPreferredVariant
                body["cricket_primary_role"] = cricketPrimaryRole
                body["cricket_batting_style"] = cricketBattingStyle
                body["cricket_bowling_style"] = cricketBowlingStyle
                body["cricket_jersey_number"] = cricketJerseyNumber

                val response = apiService.updateProfileJson(body)
                if (response.isSuccessful && response.body() != null) {
                    val updated = response.body()!!
                    _currentUser.value = updated
                    Result.success(updated)
                } else {
                    Result.failure(Exception(parseBackendError(response.errorBody()?.string(), "Failed to update profile")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAllowDirectTeamAdd(enabled: Boolean): Result<UserDataDto> {
        return try {
            val response = apiService.updateAllowDirectTeamAdd(UpdateAllowDirectTeamAddRequestDto(enabled))
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                _currentUser.value = updated
                Result.success(updated)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update privacy setting"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPhoneOtp(phone: String): Result<PhoneOtpSendResponseDto> {
        return try {
            val response = apiService.sendPhoneOtp(PhoneOtpSendRequestDto(phone))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to send phone OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPhoneOtp(challengeId: Int, otp: String): Result<UserDataDto> {
        return try {
            val response = apiService.verifyPhoneOtp(PhoneOtpVerifyDto(challengeId, otp))
            if (response.isSuccessful) {
                val user = response.body()?.user
                if (user != null) {
                    _currentUser.value = user
                    Result.success(user)
                } else {
                    // Refetch profile on success if user DTO not directly in body
                    fetchProfile()
                }
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Invalid OTP code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendEmailVerificationLink(): Result<Unit> {
        return try {
            val response = apiService.resendEmailVerificationLink()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to resend verification email"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestEmailChange(newEmail: String, currentPassword: String?): Result<Unit> {
        return try {
            val response = apiService.requestEmailChange(EmailChangeRequestDto(newEmail, currentPassword))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to request email change"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyNewEmail(otp: String, newEmail: String): Result<Unit> {
        return try {
            val response = apiService.verifyNewEmail(EmailVerifyNewRequestDto(otp, newEmail))
            if (response.isSuccessful) {
                fetchProfile()
                Result.success(Unit)
            } else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to verify new email"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(current: String, new: String, confirm: String): Result<Unit> {
        return try {
            val response = apiService.changePassword(PasswordChangeRequestDto(current, new, confirm))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to change password"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val response = apiService.deleteAccount()
            if (response.isSuccessful) {
                _currentUser.value = null
                Result.success(Unit)
            } else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to delete account"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavorites(): Result<List<APIFavoriteDto>> {
        return try {
            val response = apiService.getFavorites()
            if (response.isSuccessful && response.body() != null) {
                val root = response.body()!!
                val elements = when {
                    root.isJsonArray -> root.asJsonArray.toList()
                    root.isJsonObject -> listOf("favorites", "results", "data", "items").firstNotNullOfOrNull { key ->
                        root.asJsonObject.get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.toList()
                    }.orEmpty()
                    else -> emptyList()
                }
                Result.success(elements.mapNotNull { element -> runCatching { gson.fromJson(element, APIFavoriteDto::class.java) }.getOrNull() })
            } else Result.failure(Exception(parseBackendError(response.errorBody()?.string(), "Failed to fetch favorites")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFavoriteVenue(venueId: String): Result<APIFavoriteDto> {
        return try {
            val body = mapOf("venue_id" to venueId)
            val response = apiService.addFavorite(body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to add favorite"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(id: Int): Result<Unit> {
        return try {
            val response = apiService.removeFavorite(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(parseBackendError(response.errorBody()?.string(), "Failed to remove favorite")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPointsHistory(limit: Int = 100): Result<PointsHistoryResponseDto> {
        return try {
            val response = apiService.getPointsHistory(limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch points history"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReferrals(): Result<ReferralResponseDto> {
        return try {
            val response = apiService.getReferrals()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch referrals"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocationProvinces(): Result<List<LocationProvinceDto>> {
        return try {
            val response = apiService.getLocationProvinces()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch provinces"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocationDistricts(provinceId: Int): Result<List<LocationDistrictDto>> {
        return try {
            val response = apiService.getLocationDistricts(provinceId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch districts"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocationCities(districtId: Int? = null, search: String? = null): Result<List<LocationCityDto>> {
        return try {
            val response = apiService.getLocationCities(districtId, search)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch cities"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBlockedUsers(): Result<List<BlockedUserDto>> {
        return try {
            val response = apiService.getBlockedUsers()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch blocked users"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unblockUser(id: Int): Result<Unit> {
        return try {
            val response = apiService.unblockUser(mapOf("user_id" to id))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to unblock user"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReports(): Result<List<ReportItemDto>> {
        return try {
            val response = apiService.getReports()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch reports"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelReport(id: Int): Result<Unit> {
        return try {
            val response = apiService.cancelReport(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to cancel report"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "profile_upload_${System.currentTimeMillis()}.jpg")
            val bitmap = inputStream.use(BitmapFactory::decodeStream) ?: return null
            val maxEdge = 1600
            val scale = minOf(1f, maxEdge.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat())
            val outputBitmap = if (scale < 1f) Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            ) else bitmap
            FileOutputStream(file).use { output ->
                if (!outputBitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) return null
            }
            if (outputBitmap !== bitmap) outputBitmap.recycle()
            bitmap.recycle()
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun parseBackendError(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        return runCatching {
            val root = gson.fromJson(raw, JsonElement::class.java)
            if (!root.isJsonObject) return@runCatching raw
            root.asJsonObject.entrySet().flatMap { (field, value) ->
                val label = field.split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                val messages = when {
                    value.isJsonArray -> value.asJsonArray.mapNotNull { it.takeUnless(JsonElement::isJsonNull)?.asString }
                    value.isJsonPrimitive -> listOf(value.asString)
                    else -> listOf(value.toString())
                }
                messages.map { message -> if (field in setOf("detail", "error", "message", "non_field_errors")) message else "$label: $message" }
            }.joinToString("\n").ifBlank { fallback }
        }.getOrDefault(fallback)
    }
}
