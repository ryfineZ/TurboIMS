package io.github.vvb2060.ims

import android.app.Application

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        // Fallback cleanup in case replacement broadcast is delayed or skipped.
        UpdateApkCleanup.cleanupIfUpdated(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        LogcatRepository.stopAndClear()
    }
}
