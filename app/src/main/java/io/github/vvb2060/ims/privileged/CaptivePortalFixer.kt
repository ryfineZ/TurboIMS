package io.github.vvb2060.ims.privileged

import android.app.Activity
import android.app.IActivityManager
import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import android.os.ServiceManager
import android.provider.Settings
import android.system.Os
import android.util.Log
import rikka.shizuku.ShizukuBinderWrapper

class CaptivePortalFixer : Instrumentation() {
    companion object {
        private const val TAG = "CaptivePortalFixer"
        private const val KEY_CAPTIVE_PORTAL_HTTP_URL = "captive_portal_http_url"
        private const val KEY_CAPTIVE_PORTAL_HTTPS_URL = "captive_portal_https_url"

        const val CN_HTTP_URL = "http://connectivitycheck.gstatic.cn/generate_204"
        const val CN_HTTPS_URL = "https://connectivitycheck.gstatic.cn/generate_204"

        const val BUNDLE_RESULT = "result"
        const val BUNDLE_RESULT_MSG = "result_msg"
        const val BUNDLE_HTTP_URL = "http_url"
        const val BUNDLE_HTTPS_URL = "https_url"
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        val result = Bundle()
        val binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
        val am = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(binder))
        am.startDelegateShellPermissionIdentity(Os.getuid(), null)
        try {
            val resolver = context.contentResolver
            val setHttp = Settings.Global.putString(
                resolver,
                KEY_CAPTIVE_PORTAL_HTTP_URL,
                CN_HTTP_URL
            )
            val setHttps = Settings.Global.putString(
                resolver,
                KEY_CAPTIVE_PORTAL_HTTPS_URL,
                CN_HTTPS_URL
            )
            val currentHttp = Settings.Global.getString(resolver, KEY_CAPTIVE_PORTAL_HTTP_URL).orEmpty()
            val currentHttps = Settings.Global.getString(resolver, KEY_CAPTIVE_PORTAL_HTTPS_URL).orEmpty()
            result.putString(BUNDLE_HTTP_URL, currentHttp)
            result.putString(BUNDLE_HTTPS_URL, currentHttps)

            val verified = setHttp && setHttps && currentHttp == CN_HTTP_URL && currentHttps == CN_HTTPS_URL
            if (!verified) {
                throw IllegalStateException(
                    "verify failed, http=$currentHttp, https=$currentHttps, putHttp=$setHttp, putHttps=$setHttps"
                )
            }
            result.putBoolean(BUNDLE_RESULT, true)
        } catch (t: Throwable) {
            Log.e(TAG, "apply captive portal urls failed", t)
            result.putBoolean(BUNDLE_RESULT, false)
            result.putString(BUNDLE_RESULT_MSG, t.message ?: t.javaClass.simpleName)
        } finally {
            am.stopDelegateShellPermissionIdentity()
        }
        finish(Activity.RESULT_OK, result)
    }
}
