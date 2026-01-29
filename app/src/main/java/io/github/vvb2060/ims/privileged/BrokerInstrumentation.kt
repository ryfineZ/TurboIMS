package io.github.vvb2060.ims.privileged

import android.annotation.SuppressLint
import android.app.Activity
import android.app.IActivityManager
import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.os.ServiceManager
import android.system.Os
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.util.Log
import rikka.shizuku.ShizukuBinderWrapper

class BrokerInstrumentation : Instrumentation() {
    companion object {
        private const val TAG = "BrokerInstrumentation"
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
            val cm = context.getSystemService(CarrierConfigManager::class.java)
            val sm = context.getSystemService(SubscriptionManager::class.java)

            val selectedSubId = arguments.getInt(ImsModifier.BUNDLE_SELECT_SIM_ID, -1)
            arguments.remove(ImsModifier.BUNDLE_SELECT_SIM_ID)
            val reset = arguments.getBoolean(ImsModifier.BUNDLE_RESET, false)
            arguments.remove(ImsModifier.BUNDLE_RESET)

            val subIds: IntArray = if (selectedSubId == -1) {
                sm.javaClass.getMethod("getActiveSubscriptionIdList").invoke(sm) as IntArray
            } else {
                intArrayOf(selectedSubId)
            }

            val values = if (reset) null else toPersistableBundle(arguments)
            for (subId in subIds) {
                Log.i(TAG, "overrideConfig for subId $subId with values $values")
            try {
                cm.javaClass.getMethod(
                    "overrideConfig",
                    Int::class.javaPrimitiveType,
                    PersistableBundle::class.java,
                    Boolean::class.javaPrimitiveType
                ).invoke(cm, subId, values, false)
            } catch (_: NoSuchMethodException) {
                cm.javaClass.getMethod(
                    "overrideConfig",
                    Int::class.javaPrimitiveType,
                    PersistableBundle::class.java
                ).invoke(cm, subId, values)
            }
            }
            result.putBoolean(ImsModifier.BUNDLE_RESULT, true)
        } catch (t: Throwable) {
            Log.e(TAG, "failed to override config", t)
            result.putBoolean(ImsModifier.BUNDLE_RESULT, false)
            result.putString(ImsModifier.BUNDLE_RESULT_MSG, t.message ?: t.javaClass.simpleName)
        } finally {
            am.stopDelegateShellPermissionIdentity()
        }

        finish(Activity.RESULT_OK, result)
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    private fun toPersistableBundle(bundle: Bundle): PersistableBundle {
        val pb = PersistableBundle()
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            when (value) {
                is Int -> pb.putInt(key, value)
                is Long -> pb.putLong(key, value)
                is Double -> pb.putDouble(key, value)
                is String -> pb.putString(key, value)
                is Boolean -> pb.putBoolean(key, value)
                is IntArray -> pb.putIntArray(key, value)
                is LongArray -> pb.putLongArray(key, value)
                is DoubleArray -> pb.putDoubleArray(key, value)
                is BooleanArray -> pb.putBooleanArray(key, value)
                else -> {
                    if (value is Array<*> && value.isArrayOf<String>()) {
                        pb.putStringArray(key, value as Array<String>)
                    } else {
                        Log.i(TAG, "toPersistableBundle: unsupported type for key $key")
                    }
                }
            }
        }
        return pb
    }
}
