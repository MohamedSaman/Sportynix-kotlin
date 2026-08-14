package com.sportynix.app.core.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleUserInfo(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val photo: String?
)

data class GoogleSignInResult(
    val idToken: String?,
    val user: GoogleUserInfo
)

@Singleton
class GoogleAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var initialized = false
    private lateinit var googleSignInClient: GoogleSignInClient

    fun init() {
        if (initialized) return

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken("957336473509-dopudheo790qo6inggq5cukagkfpir3t.apps.googleusercontent.com")
            // .requestServerAuthCode("957336473509-dopudheo790qo6inggq5cukagkfpir3t.apps.googleusercontent.com", true)
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
        initialized = true
    }

    fun getSignInIntent(): Intent {
        if (!initialized) init()
        // Sign out first to clear any previous state and ensure fresh account picker
        googleSignInClient.signOut()
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(data: Intent?): Result<GoogleSignInResult> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)

            val idToken = account.idToken
            val email = account.email ?: ""
            val id = account.id ?: ""
            val firstName = account.givenName ?: ""
            val lastName = account.familyName ?: ""
            val photo = account.photoUrl?.toString()

            val userInfo = GoogleUserInfo(id, email, firstName, lastName, photo)
            Result.success(GoogleSignInResult(idToken, userInfo))
        } catch (e: ApiException) {
            Timber.e(e, "Google SignIn Error, code: \${e.statusCode}")
            
            // Map specific errors like SIGN_IN_CANCELLED (12501)
            if (e.statusCode == 12501) {
                Timber.d("Google SignIn cancelled by user")
                Result.failure(CancellationException("Sign in cancelled"))
            } else {
                googleSignInClient.signOut()
                Result.failure(Exception("Google sign in failed: \${e.message}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Google SignIn Error")
            googleSignInClient.signOut()
            Result.failure(e)
        }
    }

    fun signOut() {
        if (!initialized) init()
        googleSignInClient.signOut().addOnCompleteListener {
            Timber.d("Google SignOut completed")
        }
    }

    fun getCurrentUser(): GoogleSignInAccount? {
        if (!initialized) init()
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun isInitialized(): Boolean = initialized

    class CancellationException(message: String) : Exception(message) {
        val isCancelled = true
    }
}
