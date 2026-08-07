package com.sportynix.app.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.sportynix.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleCredentialResult {
    data class Success(val idToken: String) : GoogleCredentialResult()
    data object Cancelled : GoogleCredentialResult()
    data class Failure(val message: String) : GoogleCredentialResult()
}

@Singleton
class GoogleAuthService @Inject constructor(@ApplicationContext context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(activityContext: Context): GoogleCredentialResult = try {
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val response = credentialManager.getCredential(
            activityContext,
            GetCredentialRequest.Builder().addCredentialOption(option).build()
        )
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleCredentialResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else GoogleCredentialResult.Failure("Unsupported Google credential")
    } catch (_: GetCredentialCancellationException) {
        GoogleCredentialResult.Cancelled
    } catch (e: Exception) {
        val raw = e.localizedMessage.orEmpty()
        val message = if (
            raw.contains("developer console", ignoreCase = true) ||
            raw.contains("28444") || raw.contains("configuration", ignoreCase = true)
        ) {
            "Google Sign-In is not configured for this app signature. Install the latest build or contact support."
        } else {
            raw.ifBlank { "Google Sign-In is temporarily unavailable. Please try again." }
        }
        GoogleCredentialResult.Failure(message)
    }

    suspend fun signOut() = credentialManager.clearCredentialState(ClearCredentialStateRequest())
}
