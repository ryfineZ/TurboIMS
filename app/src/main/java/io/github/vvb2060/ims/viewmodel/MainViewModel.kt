package io.github.vvb2060.ims.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.vvb2060.ims.BuildConfig
import io.github.vvb2060.ims.R
import io.github.vvb2060.ims.ShizukuProvider
import io.github.vvb2060.ims.model.Feature
import io.github.vvb2060.ims.model.FeatureConfigMapper
import io.github.vvb2060.ims.model.FeatureValue
import io.github.vvb2060.ims.model.FeatureValueType
import io.github.vvb2060.ims.model.ShizukuStatus
import io.github.vvb2060.ims.model.SimSelection
import io.github.vvb2060.ims.model.SystemInfo
import io.github.vvb2060.ims.privileged.ImsModifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

/**
 * 主界面的 ViewModel，负责管理 UI 状态和业务逻辑。
 * 包括 Shizuku 状态监听、系统信息加载、SIM 卡信息加载以及 IMS 配置的读写。
 */
class MainViewModel(private val application: Application) : AndroidViewModel(application) {
    private var toast: Toast? = null

    // 系统信息状态流
    private val _systemInfo = MutableStateFlow(SystemInfo())
    val systemInfo: StateFlow<SystemInfo> = _systemInfo.asStateFlow()

    // Shizuku 运行状态流
    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.CHECKING)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus.asStateFlow()

    // 所有可用 SIM 卡列表流
    private val _allSimList = MutableStateFlow<List<SimSelection>>(emptyList())
    val allSimList: StateFlow<List<SimSelection>> = _allSimList.asStateFlow()

    // Shizuku Binder 接收监听器（服务连接/授权后触发）
    private val binderListener = Shizuku.OnBinderReceivedListener { updateShizukuStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { updateShizukuStatus() }

    init {
        loadSimList()
        loadSystemInfo()
        updateShizukuStatus()
        Shizuku.addBinderReceivedListener(binderListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    override fun onCleared() {
        super.onCleared()
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }

    /**
     * 更新 Shizuku 的当前状态。
     * 检查服务是否运行、是否需要更新以及权限授予情况。
     */
    fun updateShizukuStatus() {
        viewModelScope.launch {
            if (Shizuku.isPreV11()) {
                _shizukuStatus.value = ShizukuStatus.NEED_UPDATE
            }
            val status = when {
                !Shizuku.pingBinder() -> ShizukuStatus.NOT_RUNNING
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> ShizukuStatus.NO_PERMISSION
                else -> ShizukuStatus.READY
            }
            _shizukuStatus.value = status
        }
    }

    /**
     * 请求 Shizuku 授权。
     */
    fun requestShizukuPermission(requestCode: Int) {
        viewModelScope.launch {
            if (Shizuku.isPreV11()) {
                _shizukuStatus.value = ShizukuStatus.NEED_UPDATE
            } else {
                Shizuku.requestPermission(requestCode)
            }
        }
    }

    /**
     * 加载默认的功能配置。
     * 当没有保存的配置时使用此默认值。
     */
    fun loadDefaultPreferences(): Map<Feature, FeatureValue> {
        val featureSwitches = linkedMapOf<Feature, FeatureValue>()
        for (feature in Feature.entries) {
            featureSwitches.put(feature, FeatureValue(feature.defaultValue, feature.valueType))
        }
        return featureSwitches
    }

    /**
     * 通过 Shizuku 读取设备上的 SIM 卡信息。
     * 并在列表头部添加“所有 SIM 卡”选项。
     */
    fun loadSimList() {
        viewModelScope.launch {
            val simInfoList = ShizukuProvider.readSimInfoList(application)
            val resultList = simInfoList.toMutableList()
            // 添加默认的 "所有 SIM 卡" 选项 (subId = -1)
            val title = application.getString(R.string.all_sim)
            resultList.add(0, SimSelection(-1, "", "", -1, title))
            _allSimList.value = resultList
        }
    }

    /**
     * 加载当前应用和系统的基本信息。
     */
    private fun loadSystemInfo() {
        viewModelScope.launch {
            _systemInfo.value = SystemInfo(
                appVersionName = BuildConfig.VERSION_NAME,
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                systemVersion = Build.DISPLAY,
                securityPatchVersion = Build.VERSION.SECURITY_PATCH,
            )
        }
    }

    /**
     * 应用 IMS 配置到选定的 SIM 卡。
     * 此操作会调用 ShizukuProvider 进行特权操作，并保存当前配置到本地。
     */
    fun onApplyConfiguration(selectedSim: SimSelection, map: Map<Feature, FeatureValue>) {
        viewModelScope.launch {
            // 保存配置到 SharedPreferences
            saveConfiguration(selectedSim.subId, map)

            // 构建传递给底层 ImsModifier 的配置 Bundle
            val carrierName =
                if (selectedSim.subId == -1) null else map[Feature.CARRIER_NAME]?.data as String?
            val countryISO =
                if (selectedSim.subId == -1) null else map[Feature.COUNTRY_ISO]?.data as String?
            val imsUserAgent =
                if (selectedSim.subId == -1) null else map[Feature.IMS_USER_AGENT]?.data as String?
            val enableVoLTE = (map[Feature.VOLTE]?.data ?: true) as Boolean
            val enableVoWiFi = (map[Feature.VOWIFI]?.data ?: true) as Boolean
            val enableVT = (map[Feature.VT]?.data ?: true) as Boolean
            val enableVoNR = (map[Feature.VONR]?.data ?: true) as Boolean
            val enableCrossSIM = (map[Feature.CROSS_SIM]?.data ?: true) as Boolean
            val enableUT = (map[Feature.UT]?.data ?: true) as Boolean
            val enable5GNR = (map[Feature.FIVE_G_NR]?.data ?: true) as Boolean
            val enable5GThreshold = (map[Feature.FIVE_G_THRESHOLDS]?.data ?: true) as Boolean
            val enableShow4GForLTE = (map[Feature.SHOW_4G_FOR_LTE]?.data ?: false) as Boolean

            val bundle = ImsModifier.buildBundle(
                carrierName,
                countryISO,
                imsUserAgent,
                enableVoLTE,
                enableVoWiFi,
                enableVT,
                enableVoNR,
                enableCrossSIM,
                enableUT,
                enable5GNR,
                enable5GThreshold,
                enableShow4GForLTE
            )
            bundle.putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, selectedSim.subId)

            // 调用 Shizuku 服务进行实际修改
            val resultMsg = ShizukuProvider.overrideImsConfig(application, bundle)
            if (resultMsg == null) {
                toast(application.getString(R.string.config_success_message))
            } else {
                toast(application.getString(R.string.config_failed, resultMsg), false)
            }
        }
    }

    /**
     * 将配置保存到 SharedPreferences 中以便下次加载。
     */
    private fun saveConfiguration(subId: Int, map: Map<Feature, FeatureValue>) {
        application.getSharedPreferences("sim_config_$subId", Context.MODE_PRIVATE).edit {
            clear() // 清除旧配置
            map.forEach { (feature, value) ->
                when (value.valueType) {
                    FeatureValueType.BOOLEAN -> putBoolean(feature.name, value.data as Boolean)
                    FeatureValueType.STRING -> putString(feature.name, value.data as String)
                }
            }
        }
    }

    /**
     * 加载指定 subId 的配置。如果不存在则返回 null。
     */
    fun loadConfiguration(subId: Int): Map<Feature, FeatureValue>? {
        val prefs = application.getSharedPreferences("sim_config_$subId", Context.MODE_PRIVATE)
        if (prefs.all.isEmpty()) return null

        val map = linkedMapOf<Feature, FeatureValue>()
        Feature.entries.forEach { feature ->
            if (prefs.contains(feature.name)) {
                when (feature.valueType) {
                    FeatureValueType.BOOLEAN -> {
                        val data = prefs.getBoolean(feature.name, feature.defaultValue as Boolean)
                        map[feature] = FeatureValue(data, feature.valueType)
                    }

                    FeatureValueType.STRING -> {
                        val data =
                            prefs.getString(feature.name, feature.defaultValue as String) ?: ""
                        map[feature] = FeatureValue(data, feature.valueType)
                    }
                }
            } else {
                map[feature] = FeatureValue(feature.defaultValue, feature.valueType)
            }
        }
        return map
    }

    suspend fun loadCurrentConfiguration(subId: Int): Map<Feature, FeatureValue>? {
        if (subId < 0) return null
        val bundle = ShizukuProvider.readCarrierConfig(
            application,
            subId,
            FeatureConfigMapper.readKeys
        ) ?: return null
        return FeatureConfigMapper.fromBundle(bundle)
    }

    /**
     * 重置选中 SIM 卡的配置到运营商默认状态。
     */
    fun onResetConfiguration(selectedSim: SimSelection) {
        viewModelScope.launch {
            val bundle = ImsModifier.buildResetBundle()
            bundle.putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, selectedSim.subId)
            val resultMsg = ShizukuProvider.overrideImsConfig(application, bundle)
            if (resultMsg == null) {
                toast(application.getString(R.string.config_success_reset_message))
            } else {
                toast(application.getString(R.string.config_failed, resultMsg), false)
            }
        }
    }

    fun restartImsRegistration(selectedSim: SimSelection) {
        viewModelScope.launch {
            val resultMsg = ShizukuProvider.restartImsRegistration(application, selectedSim.subId)
            if (resultMsg == null) {
                toast(application.getString(R.string.ims_restart_success))
            } else {
                toast(application.getString(R.string.ims_restart_failed, resultMsg), false)
            }
        }
    }

    private fun toast(msg: String, short: Boolean = true) {
        toast?.cancel()
        toast =
            Toast.makeText(application, msg, if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG)
        toast?.show()
    }
}
