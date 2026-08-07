package com.sportynix.app.data.repository

import com.sportynix.app.core.datastore.SessionManager
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.remote.api.AuthApiService
import com.sportynix.app.data.remote.dto.ForgotPasswordRequestDto
import com.sportynix.app.data.remote.dto.LoginRequestDto
import com.sportynix.app.data.remote.dto.ResendOtpRequestDto
import com.sportynix.app.data.remote.dto.ResetPasswordRequestDto
import com.sportynix.app.data.remote.dto.SignUpRequestDto
import com.sportynix.app.data.remote.dto.VerifyOtpRequestDto
import com.sportynix.app.data.remote.dto.VerifyPasswordResetOtpRequestDto
import com.sportynix.app.data.remote.dto.EmailOtpRequestDto
import com.sportynix.app.data.remote.dto.EmailRequestDto
import com.sportynix.app.data.remote.dto.GoogleAuthRequestDto
import com.sportynix.app.domain.model.ForgotPasswordResult
import com.sportynix.app.domain.model.User
import com.sportynix.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.JsonParser
import retrofit2.Response

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    private fun errorMessage(response: Response<*>, fallback: String): String {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
        if (raw.isBlank()) return response.message().ifBlank { fallback }
        return runCatching {
            val root = JsonParser.parseString(raw).asJsonObject
            val code = root.get("code")?.takeIf { it.isJsonPrimitive }?.asString
            val message = listOf("error", "detail", "message", "non_field_errors")
                .firstNotNullOfOrNull { key ->
                    root.get(key)?.let { value ->
                        when {
                            value.isJsonArray -> value.asJsonArray.firstOrNull()?.asString
                            value.isJsonPrimitive -> value.asString
                            else -> null
                        }
                    }
                } ?: root.entrySet().filter { it.key != "code" }.flatMap { (key, value) ->
                    val messages = if (value.isJsonArray) value.asJsonArray.mapNotNull { runCatching { it.asString }.getOrNull() }
                    else listOfNotNull(runCatching { value.asString }.getOrNull())
                    messages.map { "${key.replace('_', ' ').replaceFirstChar(Char::uppercase)}: $it" }
                }.joinToString("\n").ifBlank { fallback }
            listOfNotNull(code, message).distinct().joinToString(": ")
        }.getOrDefault(fallback)
    }

    private suspend fun saveAuthenticatedResponse(body: com.sportynix.app.data.remote.dto.AuthResponseDto): ApiResult<User> {
        val token = body.accessToken ?: body.tokens?.access
        val refresh = body.refreshToken ?: body.tokens?.refresh
        val user = body.user?.toDomain()
        if (token.isNullOrBlank() || refresh.isNullOrBlank() || user == null || user.id.isBlank()) {
            return ApiResult.Error(message = "Invalid response from server")
        }
        sessionManager.saveSession(token, refresh, user.id, user.email, user.name)
        return ApiResult.Success(user)
    }

    override suspend fun checkUsernameAvailability(username: String): ApiResult<Boolean> {
        return try {
            val response = apiService.checkUsernameAvailability(username)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.available)
            } else {
                ApiResult.Success(false)
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Could not verify username availability")
        }
    }

    override suspend fun login(usernameOrEmail: String, pass: String): ApiResult<User> {
        return try {
            val response = apiService.login(LoginRequestDto(usernameOrEmail, pass))
            if (response.isSuccessful && response.body() != null) {
                saveAuthenticatedResponse(response.body()!!)
            } else if (response.code() == 401) {
                ApiResult.Unauthorized
            } else {
                ApiResult.ServerError(response.code(), errorMessage(response, "Login failed. Please check credentials."))
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network connection error")
        }
    }

    override suspend fun signUp(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        dob: String,
        pass: String,
        referralCode: String?
    ): ApiResult<String> {
        return try {
            val request = SignUpRequestDto(
                username = username,
                firstName = firstName,
                lastName = lastName,
                email = email,
                phoneNumber = phone,
                dateOfBirth = dob,
                pass = pass,
                termsAccepted = true,
                referralCode = referralCode
            )
            val response = apiService.signUp(request)
            if (response.isSuccessful && response.body() != null) {
                val sessionId = response.body()!!.sessionId ?: ""
                ApiResult.Success(sessionId)
            } else {
                ApiResult.ServerError(response.code(), errorMessage(response, "Registration failed"))
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error during sign up")
        }
    }

    override suspend fun verifySignUpOtp(sessionId: String, otpCode: String): ApiResult<User> {
        return try {
            val response = apiService.verifySignUpOtp(VerifyOtpRequestDto(sessionId, otpCode))
            if (response.isSuccessful && response.body() != null) {
                saveAuthenticatedResponse(response.body()!!)
            } else {
                ApiResult.ServerError(response.code(), errorMessage(response, "OTP verification failed"))
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error during OTP verification")
        }
    }

    override suspend fun resendOtp(sessionId: String): ApiResult<String> {
        return try {
            val response = apiService.resendOtp(ResendOtpRequestDto(sessionId))
            if (response.isSuccessful && response.body() != null) {
                val newSessionId = response.body()!!.sessionId ?: sessionId
                ApiResult.Success(newSessionId)
            } else {
                ApiResult.ServerError(response.code(), errorMessage(response, "Resend OTP failed"))
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Failed to resend OTP")
        }
    }

    override suspend fun forgotPassword(email: String): ApiResult<ForgotPasswordResult> {
        return try {
            val response = apiService.forgotPassword(ForgotPasswordRequestDto(email))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                ApiResult.Success(ForgotPasswordResult(body.sessionId.orEmpty(), body.message, body.isSocialUser == true, body.canSetPassword == true))
            } else {
                ApiResult.ServerError(response.code(), errorMessage(response, "Forgot password request failed"))
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error")
        }
    }

    override suspend fun verifyPasswordResetOtp(email: String, otpCode: String): ApiResult<Unit> {
        return try {
            val response = apiService.verifyPasswordResetOtp(VerifyPasswordResetOtpRequestDto(email, otpCode))
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.ServerError(response.code(), errorMessage(response, "Invalid or expired verification code"))
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error verifying code")
        }
    }

    override suspend fun resetPassword(
        email: String,
        otpCode: String,
        newPass: String,
        confirmPass: String
    ): ApiResult<Unit> {
        return try {
            val request = ResetPasswordRequestDto(email, otpCode, newPass, confirmPass)
            val response = apiService.resetPassword(request)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), errorMessage(response, "Reset password failed"))
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error during password reset")
        }
    }

    override suspend fun verifyEmailOtp(email: String, code: String): ApiResult<Unit> = try {
        val response = apiService.verifyEmailOtp(EmailOtpRequestDto(email.trim(), code.trim()))
        if (response.isSuccessful) ApiResult.Success(Unit)
        else ApiResult.ServerError(response.code(), errorMessage(response, "Verification failed"))
    } catch (e: Exception) { ApiResult.Error(message = e.localizedMessage ?: "Network error during verification") }

    override suspend fun resendEmailOtp(email: String): ApiResult<Unit> = try {
        val response = apiService.resendEmailOtp(EmailRequestDto(email.trim()))
        if (response.isSuccessful) ApiResult.Success(Unit)
        else ApiResult.ServerError(response.code(), errorMessage(response, "Failed to resend code"))
    } catch (e: Exception) { ApiResult.Error(message = e.localizedMessage ?: "Network error while resending code") }

    override suspend fun googleAuth(idToken: String, dateOfBirth: String?, termsAccepted: Boolean?): ApiResult<User> = try {
        val response = apiService.googleAuth(GoogleAuthRequestDto(idToken, dateOfBirth, termsAccepted))
        if (response.isSuccessful && response.body() != null) saveAuthenticatedResponse(response.body()!!)
        else ApiResult.ServerError(response.code(), errorMessage(response, "Google sign in failed"))
    } catch (e: Exception) { ApiResult.Error(message = e.localizedMessage ?: "Google sign in failed") }

    override suspend fun logout(): ApiResult<Unit> {
        try {
            apiService.logout()
        } catch (_: Exception) {}
        sessionManager.clearSession()
        return ApiResult.Success(Unit)
    }

    override fun isLoggedIn(): Flow<Boolean> = sessionManager.isLoggedIn

    override suspend fun getCurrentUser(): ApiResult<User> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                val userDomain = response.body()!!.toDomain()
                sessionManager.saveSession(
                    accessToken = sessionManager.accessToken.firstOrNull() ?: "",
                    refreshToken = sessionManager.refreshToken.firstOrNull() ?: "",
                    userId = userDomain.id,
                    email = userDomain.email,
                    name = userDomain.name
                )
                ApiResult.Success(userDomain)
            } else if (response.code() == 401) {
                ApiResult.Unauthorized
            } else {
                val cachedEmail = sessionManager.userEmail.firstOrNull()
                val cachedName = sessionManager.userName.firstOrNull()
                val cachedId = sessionManager.userId.firstOrNull()
                if (!cachedEmail.isNullOrEmpty()) {
                    ApiResult.Success(User(id = cachedId ?: "", name = cachedName ?: "", email = cachedEmail))
                } else {
                    ApiResult.ServerError(response.code(), response.message() ?: "Failed to load user profile")
                }
            }
        } catch (e: Exception) {
            val cachedEmail = sessionManager.userEmail.firstOrNull()
            val cachedName = sessionManager.userName.firstOrNull()
            val cachedId = sessionManager.userId.firstOrNull()
            if (!cachedEmail.isNullOrEmpty()) {
                ApiResult.Success(User(id = cachedId ?: "", name = cachedName ?: "", email = cachedEmail))
            } else {
                ApiResult.Error(message = e.localizedMessage ?: "Network error loading profile")
            }
        }
    }
}
