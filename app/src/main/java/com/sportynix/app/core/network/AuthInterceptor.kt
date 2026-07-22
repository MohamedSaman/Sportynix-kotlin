package com.sportynix.app.core.network

import com.sportynix.app.core.datastore.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip adding token if header 'No-Auth' is set
        if (originalRequest.header("No-Auth") != null) {
            val newRequest = originalRequest.newBuilder().removeHeader("No-Auth").build()
            return chain.proceed(newRequest)
        }

        val token = runBlocking { sessionManager.getAccessTokenSync() }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        requestBuilder.addHeader("Accept", "application/json")
        requestBuilder.addHeader("Content-Type", "application/json")

        return chain.proceed(requestBuilder.build())
    }
}
