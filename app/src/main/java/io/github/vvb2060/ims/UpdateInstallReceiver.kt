package io.github.vvb2060.ims

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        UpdateApkCleanup.cleanupIfUpdated(context)
    }
}
