package io.github.vvb2060.ims.privileged

import android.app.Activity
import android.app.IActivityManager
import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import android.os.ServiceManager
import android.system.Os
import android.telephony.TelephonyFrameworkInitializer
import android.util.Log
import com.android.internal.telephony.ITelephony
import rikka.shizuku.ShizukuBinderWrapper

class ImsStatusReader : Instrumentation() {
    companion object {
        private const val TAG = "ImsStatusReader"
        const val BUNDLE_SELECT_SIM_ID = "select_sim_id"
        const val BUNDLE_RESULT = "result"
        const val BUNDLE_RESULT_MSG = "result_msg"
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        if (arguments == null) {
            finish(Activity.RESULT_CANCELED, Bundle())
            return
        }

        val result = Bundle()
        val binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
        val am = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(binder))
        am.startDelegateShellPermissionIdentity(Os.getuid(), null)
        try {
            val subId = arguments.getInt(BUNDLE_SELECT_SIM_ID, -1)
            if (subId < 0) {
                result.putBoolean(BUNDLE_RESULT, false)
                result.putString(BUNDLE_RESULT_MSG, "invalid subId")
            } else {
                val telephony = ITelephony.Stub.asInterface(
                    ShizukuBinderWrapper(
                        TelephonyFrameworkInitializer
                            .getTelephonyServiceManager()
                            .getTelephonyServiceRegisterer()
                            .get()!!
                    )
                )
                val isRegistered = telephony.isImsRegistered(subId)
                result.putBoolean(BUNDLE_RESULT, isRegistered)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "read ims status failed", t)
            result.putBoolean(BUNDLE_RESULT, false)
            result.putString(BUNDLE_RESULT_MSG, t.message ?: t.javaClass.simpleName)
        } finally {
            am.stopDelegateShellPermissionIdentity()
        }

        finish(Activity.RESULT_OK, result)
    }
}
