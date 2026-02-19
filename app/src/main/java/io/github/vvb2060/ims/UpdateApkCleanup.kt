package io.github.vvb2060.ims

import android.content.Context
import android.os.Environment
import java.io.File

object UpdateApkCleanup {
    private const val PREFS_NAME = "update_apk_cleanup"
    private const val KEY_PENDING_APK_NAME = "pending_apk_name"
    private const val KEY_FROM_VERSION = "from_version"
    private const val KEY_TARGET_VERSION = "target_version"

    fun markPendingInstall(
        context: Context,
        apkFileName: String,
        fromVersion: String,
        targetVersion: String?,
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_APK_NAME, apkFileName)
            .putString(KEY_FROM_VERSION, fromVersion)
            .putString(KEY_TARGET_VERSION, targetVersion)
            .apply()
    }

    fun cleanupIfUpdated(context: Context, currentVersion: String = BuildConfig.VERSION_NAME): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val apkFileName = prefs.getString(KEY_PENDING_APK_NAME, null).orEmpty()
        if (apkFileName.isBlank()) return false

        val fromVersion = prefs.getString(KEY_FROM_VERSION, null).orEmpty()
        val targetVersion = prefs.getString(KEY_TARGET_VERSION, null).orEmpty()
        val updated = when {
            targetVersion.isNotBlank() -> currentVersion == targetVersion
            fromVersion.isNotBlank() -> currentVersion != fromVersion
            else -> false
        }
        if (!updated) return false

        deleteDownloadedApk(context, apkFileName)
        prefs.edit().clear().apply()
        return true
    }

    private fun deleteDownloadedApk(context: Context, apkFileName: String) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
        val apkFile = File(dir, apkFileName)
        if (apkFile.exists()) {
            apkFile.delete()
        }
    }
}
