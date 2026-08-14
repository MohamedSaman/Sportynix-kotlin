package com.sportynix.app.core.version

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class ForceUpdateInfo(
    val needsUpdate: Boolean,
    val latestVersion: String? = null,
    val currentVersion: String? = null,
    val storeUrl: String? = null
)

@Singleton
class VersionCheckService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun normalizeVersion(version: String?): List<Int> {
        if (version.isNullOrBlank()) return emptyList()
        return version.split(".").map { segment ->
            val match = Regex("\\d+").find(segment)
            match?.value?.toIntOrNull() ?: 0
        }
    }

    private fun isVersionGreater(latestVersion: String?, currentVersion: String?): Boolean {
        val latest = normalizeVersion(latestVersion)
        val current = normalizeVersion(currentVersion)
        val maxLength = maxOf(latest.size, current.size)

        for (index in 0 until maxLength) {
            val latestPart = latest.getOrElse(index) { 0 }
            val currentPart = current.getOrElse(index) { 0 }

            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }

    suspend fun checkForForceUpdate(): ForceUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val packageName = context.packageName
            val storeUrl = "https://play.google.com/store/apps/details?id=$packageName"
            val currentVersion = getCurrentVersion()

            val appUpdateManager = AppUpdateManagerFactory.create(context)
            val appUpdateInfo = getAppUpdateInfo(appUpdateManager)

            if (appUpdateInfo == null) {
                Timber.w("[VersionCheckService] Could not retrieve AppUpdateInfo from Play Store")
                return@withContext ForceUpdateInfo(needsUpdate = false)
            }

            val updateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val latestVersionCode = appUpdateInfo.availableVersionCode()
            val currentVersionCode = getCurrentVersionCode()

            val needsUpdate = updateAvailable && latestVersionCode > currentVersionCode

            Timber.d(
                "[VersionCheckService] current=$currentVersion, latestCode=$latestVersionCode, needsUpdate=$needsUpdate"
            )

            ForceUpdateInfo(
                needsUpdate = needsUpdate,
                latestVersion = latestVersionCode.toString(),
                currentVersion = currentVersion,
                storeUrl = storeUrl
            )
        } catch (error: Exception) {
            Timber.w(error, "[VersionCheckService] Force update check failed")
            ForceUpdateInfo(needsUpdate = false)
        }
    }

    private suspend fun getAppUpdateInfo(
        appUpdateManager: com.google.android.play.core.appupdate.AppUpdateManager
    ): AppUpdateInfo? = suspendCancellableCoroutine { cont ->
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info -> cont.resume(info) }
            .addOnFailureListener { e ->
                Timber.w(e, "[VersionCheckService] AppUpdateInfo fetch failed")
                cont.resume(null)
            }
    }

    private fun getCurrentVersion(): String {
        return try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    private fun getCurrentVersionCode(): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun openPlayStore() {
        val packageName = context.packageName
        val storeUrl = "https://play.google.com/store/apps/details?id=$packageName"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fall back to browser if Play Store app not available
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
