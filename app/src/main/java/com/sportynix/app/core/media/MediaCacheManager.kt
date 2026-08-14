package com.sportynix.app.core.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val cacheDir by lazy {
        val dir = File(context.cacheDir, "app-media-cache")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    suspend fun resolveForDisplay(remoteUrl: String?): String? = withContext(Dispatchers.IO) {
        if (remoteUrl.isNullOrBlank()) return@withContext null
        if (!remoteUrl.startsWith("http")) return@withContext remoteUrl

        val lookupKey = hashString(remoteUrl)
        val ext = getFileExtension(remoteUrl)
        val file = File(cacheDir, "\$lookupKey\$ext")

        if (file.exists()) {
            return@withContext "file://\${file.absolutePath}"
        }

        // Cache miss, download it
        return@withContext ensureCached(remoteUrl, lookupKey, file)
    }

    private suspend fun ensureCached(url: String, lookupKey: String, targetFile: File): String? {
        try {
            val tempFile = File(cacheDir, "\$lookupKey.tmp")
            if (tempFile.exists()) tempFile.delete()

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful && response.body != null) {
                val fos = FileOutputStream(tempFile)
                fos.write(response.body!!.bytes())
                fos.close()

                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)

                return "file://\${targetFile.absolutePath}"
            } else {
                Timber.w("Failed to cache media: HTTP \${response.code}")
            }
        } catch (e: Exception) {
            Timber.w(e, "Error caching media: \$url")
        }
        return url
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun hashString(value: String): String {
        return value.hashCode().toString(16)
    }

    private fun getFileExtension(url: String): String {
        val lastDot = url.lastIndexOf('.')
        if (lastDot > url.lastIndexOf('/')) {
            val ext = url.substring(lastDot)
            if (ext.length <= 5) return ext
        }
        return ".img"
    }
}
