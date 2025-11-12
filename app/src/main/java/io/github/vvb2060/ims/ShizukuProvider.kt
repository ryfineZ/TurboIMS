package io.github.vvb2060.ims

import android.app.IActivityManager
import android.app.UiAutomationConnection
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.ServiceManager
import android.util.Log
import org.lsposed.hiddenapibypass.LSPass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider

class ShizukuProvider : ShizukuProvider() {
    override fun onCreate(): Boolean {
        LSPass.setHiddenApiExemptions("")
        // 不再自动触发，只在用户手动点击"应用配置"时才执行
        return super.onCreate()
    }

    companion object {
        fun startInstrument(context: Context) {
            try {
                Log.i("ShizukuProvider", "Starting instrumentation...")
                val binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
                val am = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(binder))
                val name = ComponentName(context, PrivilegedProcess::class.java)
                val flags = 0x00000001 // ActivityManager.INSTR_FLAG_NO_RESTART
                val connection = UiAutomationConnection()
                Log.i("ShizukuProvider", "Calling startInstrumentation with component: $name")
                am.startInstrumentation(name, null, flags, Bundle(), null, connection, 0, null)
                Log.i("ShizukuProvider", "Instrumentation started successfully")
            } catch (e: Exception) {
                Log.e("ShizukuProvider", "Failed to start instrumentation", e)
            }
        }
    }
}
