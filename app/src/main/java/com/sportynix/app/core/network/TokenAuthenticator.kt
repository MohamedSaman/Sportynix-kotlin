package com.sportynix.app.core.network

import com.google.gson.Gson
import com.sportynix.app.BuildConfig
import com.sportynix.app.core.datastore.SessionManager
import com.sportynix.app.data.remote.dto.RefreshTokenRequestDto
import com.sportynix.app.data.remote.dto.RefreshTokenResponseDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager
) : Authenticator {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val refreshClient = OkHttpClient.Builder().build()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loops if authentication fails repeatedly
        if (responseCount(response) >= 3) {
            Timber.w("Max authentication retry attempts reached. Clearing session.")
            runBlocking { sessionManager.clearSession() }
            return null
        }

        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()

        synchronized(this) {
            val latestToken = runBlocking { sessionManager.getAccessTokenSync() }

            // If another parallel request already refreshed the token, reuse it immediately
            if (!latestToken.isNullOrEmpty() && latestToken != requestToken) {
                Timber.d("Token was refreshed by a concurrent request. Retrying with latest token.")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }

            val refreshToken = runBlocking { sessionManager.getRefreshTokenSync() }
            if (refreshToken.isNullOrEmpty()) {
                Timber.w("No refresh token available. Clearing session.")
                runBlocking { sessionManager.clearSession() }
                return null
            }

            Timber.d("Executing synchronized token refresh call...")
            val newAccessToken = refreshAccessTokenSync(refreshToken)

            return if (!newAccessToken.isNullOrEmpty()) {
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            } else {
                Timber.w("Refresh token call failed or expired. Logging out user.")
                runBlocking { sessionManager.clearSession() }
                null
            }
        }
    }

    private fun refreshAccessTokenSync(refreshToken: String): String? {
        return try {
            val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
            val requestBodyJson = gson.toJson(RefreshTokenRequestDto(refresh = refreshToken))
            val request = Request.Builder()
                .url("$baseUrl/api/token/refresh/")
                .post(requestBodyJson.toRequestBody(jsonMediaType))
                .build()

            val refreshResponse = refreshClient.newCall(request).execute()
            if (refreshResponse.isSuccessful && refreshResponse.body != null) {
                val responseStr = refreshResponse.body!!.string()
                val dto = gson.fromJson(responseStr, RefreshTokenResponseDto::class.java)
                runBlocking {
                    sessionManager.updateTokens(dto.access, refreshToken)
                }
                Timber.d("Token successfully refreshed!")
                dto.access
            } else {
                Timber.w("Token refresh HTTP failed with status code ${refreshResponse.code}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception occurred during token refresh execution")
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
