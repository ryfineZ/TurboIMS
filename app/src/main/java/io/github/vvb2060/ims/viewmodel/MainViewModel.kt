package io.github.vvb2060.ims.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
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
import kotlinx.coroutines.delay
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
            val previousStatus = _shizukuStatus.value
            if (Shizuku.isPreV11()) {
                _shizukuStatus.value = ShizukuStatus.NEED_UPDATE
                return@launch
            }
            val status = when {
                !Shizuku.pingBinder() -> ShizukuStatus.NOT_RUNNING
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> ShizukuStatus.NO_PERMISSION
                else -> ShizukuStatus.READY
            }
            _shizukuStatus.value = status
            if (
                status == ShizukuStatus.READY &&
                (
                    previousStatus != ShizukuStatus.READY ||
                        _allSimList.value.isEmpty() ||
                        _allSimList.value.all { it.subId == -1 }
                    )
            ) {
                loadSimList()
            }
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
     * 主 SIM 优先显示，“所有 SIM 卡”放在列表末尾。
     */
    fun loadSimList() {
        viewModelScope.launch {
            loadSimListInternal()
        }
    }

    /**
     * 手动刷新 SIM 列表并返回是否读取到了至少 1 张真实 SIM。
     * 返回 false 代表当前只剩“所有 SIM”占位项。
     */
    suspend fun refreshSimListNow(): Boolean = loadSimListInternal()

    private suspend fun loadSimListInternal(): Boolean {
        val shizukuReady =
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        val retryCount = if (shizukuReady) 3 else 1
        var simInfoList: List<SimSelection> = emptyList()
        for (attempt in 0 until retryCount) {
            simInfoList = ShizukuProvider.readSimInfoList(application)
            if (simInfoList.isNotEmpty()) {
                break
            }
            if (attempt < retryCount - 1) {
                delay(250)
            }
        }

        val primarySubId = resolvePrimarySubId()
        val sortedSimList = simInfoList.sortedWith(
            compareByDescending<SimSelection> { it.subId == primarySubId }
                .thenBy { it.simSlotIndex }
                .thenBy { it.subId }
        )
        val resultList = sortedSimList.toMutableList()
        // 添加默认的 "所有 SIM 卡" 选项 (subId = -1) 到末尾
        val title = application.getString(R.string.all_sim)
        resultList.add(SimSelection(-1, "", "", -1, title))
        _allSimList.value = resultList
        return simInfoList.isNotEmpty()
    }

    private fun resolvePrimarySubId(): Int {
        val dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) return dataSubId
        val voiceSubId = SubscriptionManager.getDefaultVoiceSubscriptionId()
        if (voiceSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) return voiceSubId
        val smsSubId = SubscriptionManager.getDefaultSmsSubscriptionId()
        if (smsSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) return smsSubId
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID
    }

    /**
     * 加载当前应用和系统的基本信息。
     */
    private fun loadSystemInfo() {
        viewModelScope.launch {
            _systemInfo.value = SystemInfo(
                appVersionName = BuildConfig.VERSION_NAME,
                androidVersion = "Android ${Build.VERSION.RELEASE} / ${Build.DISPLAY}",
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
    suspend fun onApplyConfiguration(selectedSim: SimSelection, map: Map<Feature, FeatureValue>): String? {
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
        val enable5GPlusIcon = (map[Feature.FIVE_G_PLUS_ICON]?.data ?: true) as Boolean
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
            enable5GPlusIcon,
            enableShow4GForLTE
        )
        bundle.putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, selectedSim.subId)

        // 调用 Shizuku 服务进行实际修改
        val resultMsg = ShizukuProvider.overrideImsConfig(application, bundle)
        if (resultMsg == null) {
            // 仅在应用成功后保存配置，避免本地状态与系统状态不一致
            saveConfiguration(selectedSim.subId, map)
        }
        return resultMsg
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

    suspend fun readImsRegistrationStatus(subId: Int): Boolean? {
        if (subId < 0) return null
        return ShizukuProvider.readImsRegistrationStatus(application, subId)
    }

    suspend fun registerIms(subId: Int): Boolean? {
        if (subId < 0) return null
        val resultMsg = ShizukuProvider.restartImsRegistration(application, subId)
        if (resultMsg != null) {
            toast(application.getString(R.string.ims_restart_failed, resultMsg), false)
            return null
        }
        val status = readImsRegistrationStatus(subId)
        if (status == true) {
            toast(application.getString(R.string.ims_register_success))
        } else {
            toast(application.getString(R.string.ims_register_pending), false)
        }
        return status
    }

    /**
     * 重置选中 SIM 卡的配置到运营商默认状态。
     */
    suspend fun onResetConfiguration(selectedSim: SimSelection): Boolean {
        val bundle = ImsModifier.buildResetBundle()
        bundle.putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, selectedSim.subId)
        val resultMsg = ShizukuProvider.overrideImsConfig(application, bundle)
        if (resultMsg == null) {
            toast(application.getString(R.string.config_success_reset_message))
            return true
        }
        toast(application.getString(R.string.config_failed, resultMsg), false)
        return false
    }

    private fun toast(msg: String, short: Boolean = true) {
        toast?.cancel()
        toast =
            Toast.makeText(application, msg, if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG)
        toast?.show()
    }
}
