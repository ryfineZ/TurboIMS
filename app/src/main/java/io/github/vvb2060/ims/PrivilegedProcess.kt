package io.github.vvb2060.ims

import android.annotation.SuppressLint
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
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

class PrivilegedProcess : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        Log.i("PrivilegedProcess", "onCreate called")

        // 等待 Shizuku binder 准备好
        val maxRetries = 50 // 最多等待 5 秒
        for (i in 0..<maxRetries) {
            if (Shizuku.pingBinder()) {
                Log.i("PrivilegedProcess", "Shizuku binder is ready")
                break
            }
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                break
            }
        }

        try {
            overrideConfig()
            Log.i("PrivilegedProcess", "overrideConfig completed successfully")
        } catch (e: Exception) {
            Log.e("PrivilegedProcess", "Failed to override config", e)
        }
        finish(0, Bundle())
    }

    @SuppressLint("MissingPermission", "NewApi")
    @Throws(Exception::class)
    private fun overrideConfig() {
        Log.i("PrivilegedProcess", "overrideConfig started")
        val binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
        val am = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(binder))
        Log.i("PrivilegedProcess", "Starting shell permission delegation")
        am.startDelegateShellPermissionIdentity(Os.getuid(), null)
        try {
            val cm = context.getSystemService(CarrierConfigManager::class.java)
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val values: PersistableBundle = getConfig(getContext())

            // 读取用户选择的 SubId
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val selectedSubId = prefs.getInt("selected_subid", 1)
            Log.i("PrivilegedProcess", "Selected SubId: $selectedSubId")

            val subIds: IntArray?
            if (selectedSubId == -1) {
                // 应用到所有 SIM 卡
                subIds =
                    sm.javaClass.getMethod("getActiveSubscriptionIdList").invoke(sm) as IntArray?
                Log.i("PrivilegedProcess", "Applying to all SIM cards: " + subIds.contentToString())
            } else {
                // 只应用到选中的 SIM 卡
                subIds = intArrayOf(selectedSubId)
                Log.i("PrivilegedProcess", "Applying to SIM: $selectedSubId")
            }

            for (subId in subIds!!) {
                Log.i("PrivilegedProcess", "Processing SubId: $subId")
                val bundle = cm.getConfigForSubId(subId, "vvb2060_config_version")
                val currentVersion = bundle.getInt("vvb2060_config_version", 0)
                Log.i(
                    "PrivilegedProcess",
                    "Current version: " + currentVersion + ", BuildConfig: " + BuildConfig.VERSION_CODE
                )

                if (currentVersion != BuildConfig.VERSION_CODE) {
                    values.putInt("vvb2060_config_version", BuildConfig.VERSION_CODE)
                    // 使用反射调用 overrideConfig
                    try {
                        cm.javaClass.getMethod(
                            "overrideConfig",
                            Int::class.javaPrimitiveType,
                            PersistableBundle::class.java
                        )
                            .invoke(cm, subId, values)
                        Log.i("PrivilegedProcess", "Applied config (2-param) to SubId: $subId")
                    } catch (e: NoSuchMethodException) {
                        // 如果不存在两参数方法，尝试三参数方法
                        cm.javaClass.getMethod(
                            "overrideConfig",
                            Int::class.javaPrimitiveType,
                            PersistableBundle::class.java,
                            Boolean::class.javaPrimitiveType
                        )
                            .invoke(cm, subId, values, false)
                        Log.i("PrivilegedProcess", "Applied config (3-param) to SubId: $subId")
                    }
                } else {
                    Log.i("PrivilegedProcess", "Config already up-to-date for SubId: $subId")
                }
            }
        } finally {
            am.stopDelegateShellPermissionIdentity()
            Log.i("PrivilegedProcess", "Stopped shell permission delegation")
        }
    }

    companion object {
        private const val PREFS_NAME = "ims_config"

        @SuppressLint("InlinedApi")
        private fun getConfig(context: Context): PersistableBundle {
            // 读取用户配置
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val enableVoLTE = prefs.getBoolean("volte", true)
            val enableVoWiFi = prefs.getBoolean("vowifi", true)
            val enableVT = prefs.getBoolean("vt", true)
            val enableVoNR = prefs.getBoolean("vonr", true)
            val enableCrossSIM = prefs.getBoolean("cross_sim", true)
            val enableUT = prefs.getBoolean("ut", true)
            val enable5GNR = prefs.getBoolean("5g_nr", true)

            val bundle = PersistableBundle()

            // VoLTE 配置
            if (enableVoLTE) {
                bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
                bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true)
                bundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false)
                bundle.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false)
            }

            // VT (视频通话) 配置
            if (enableVT) {
                bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true)
            }

            // UT 补充服务配置
            if (enableUT) {
                bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true)
            }

            // 跨 SIM 通话配置
            if (enableCrossSIM) {
                bundle.putBoolean(
                    CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
                    true
                )
                bundle.putBoolean(
                    CarrierConfigManager.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL,
                    true
                )
            }

            // VoWiFi 配置
            if (enableVoWiFi) {
                bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true)
                bundle.putBoolean(
                    CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL,
                    true
                )
                bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true)
                bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true)
                // KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL
                bundle.putBoolean("show_wifi_calling_icon_in_status_bar_bool", true)
                // KEY_WFC_SPN_FORMAT_IDX_INT
                bundle.putInt("wfc_spn_format_idx_int", 6)
            }

            // VoNR (5G 语音) 配置
            if (enableVoNR) {
                bundle.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true)
                bundle.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true)
            }

            // 5G NR 配置
            if (enable5GNR) {
                bundle.putIntArray(
                    CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                    intArrayOf(
                        CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA,
                        CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA
                    )
                )
                bundle.putIntArray(
                    CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,  // Boundaries: [-140 dBm, -44 dBm]
                    intArrayOf(
                        -128,  /* SIGNAL_STRENGTH_POOR */
                        -118,  /* SIGNAL_STRENGTH_MODERATE */
                        -108,  /* SIGNAL_STRENGTH_GOOD */
                        -98,  /* SIGNAL_STRENGTH_GREAT */
                    )
                )
            }

            return bundle
        }
    }
}
