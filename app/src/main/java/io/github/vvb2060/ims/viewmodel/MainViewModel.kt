package io.github.vvb2060.ims.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * 主界面的 ViewModel，负责管理 UI 状态和业务逻辑。
 * 包括 Shizuku 状态监听、系统信息加载、SIM 卡信息加载以及 IMS 配置的读写。
 */
class MainViewModel(private val application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainViewModel"
        private const val RUNTIME_PREFS = "runtime_state"
        private const val COUNTRY_MCC_PREF_KEY = "__country_mcc_override__"
        private const val TIKTOK_RANDOM_ISO_PREF_KEY = "__tiktok_random_iso__"
        private const val KEY_LAST_BOOT_COUNT = "last_boot_count"
        private const val ISSUE_FAILURE_LOG_FILE = "issue_failure_logs.txt"
        private const val ISSUE_FAILURE_LOG_MAX_LINES = 200
        private const val CAPTIVE_PORTAL_CHECK_TIMEOUT_MS = 2_500
        private val DEFAULT_CAPTIVE_PORTAL_TEST_URLS = listOf(
            "http://connectivitycheck.gstatic.cn/generate_204",
            "https://www.google.cn/generate_204",
            "https://connectivitycheck.gstatic.com/generate_204",
            "https://www.google.com/generate_204"
        )
    }

    private data class BootRestoreResult(
        val attempted: Int,
        val success: Int,
        val failed: Int,
    )

    data class ImsRegisterResult(
        val registered: Boolean?,
        val backendErrorMessage: String?,
    )

    enum class CaptivePortalFixMode {
        NEED_FIX,
        CAN_RESTORE,
        NORMAL,
    }

    data class CaptivePortalFixState(
        val mode: CaptivePortalFixMode,
        val httpUrl: String,
        val httpsUrl: String,
    )

    private var toast: Toast? = null
    private val runtimePrefs = application.getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE)
    private val issueFailureLogMutex = Mutex()
    private var pendingConfigRestoreAfterBoot = false
    private var restoringConfigAfterBoot = false
    private val canUsePersistentOverride by lazy {
        val flags = application.applicationInfo.flags
        (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private fun normalizeMcc(raw: String): String {
        return raw.filter { it.isDigit() }.take(3)
    }

    private fun normalizeIso(raw: String): String {
        return raw.trim().lowercase(Locale.US).filter { it.isLetterOrDigit() }.take(8)
    }

    private fun nowShortTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun isChinaDomesticSim(selectedSim: SimSelection): Boolean {
        val iccId = selectedSim.iccId.trim()
        if (iccId.startsWith("8986")) return true
        return normalizeMcc(selectedSim.mcc) == "460"
    }

    private fun resolveIsoByMcc(mccRaw: String, fallbackIsoRaw: String): String? {
        val mcc = normalizeMcc(mccRaw)
        val fallbackIso = normalizeIso(fallbackIsoRaw)
        if (mcc.isBlank()) return fallbackIso.ifBlank { null }
        val mccInt = mcc.toIntOrNull()
        val iso = when {
            mcc == "460" -> "cn"
            mcc == "454" -> "hk"
            mcc == "466" -> "tw"
            mccInt != null && mccInt in 310..316 -> "us"
            mccInt != null && mccInt in 440..441 -> "jp"
            mccInt != null && mccInt in 234..235 -> "gb"
            mcc == "450" -> "kr"
            mcc == "525" -> "sg"
            else -> fallbackIso
        }
        return iso.ifBlank { null }
    }

    private fun loadOrCreateTikTokRandomIso(subId: Int): String {
        val prefs = application.getSharedPreferences("sim_config_$subId", Context.MODE_PRIVATE)
        val existing = prefs.getString(TIKTOK_RANDOM_ISO_PREF_KEY, "")
            .orEmpty()
            .filter { it.isDigit() }
            .take(6)
        if (existing.length >= 3) return existing
        val generated = (10000..99999).random().toString()
        prefs.edit { putString(TIKTOK_RANDOM_ISO_PREF_KEY, generated) }
        return generated
    }

    private fun clearTikTokRandomIso(subId: Int) {
        application.getSharedPreferences("sim_config_$subId", Context.MODE_PRIVATE).edit {
            remove(TIKTOK_RANDOM_ISO_PREF_KEY)
        }
    }

    private fun resolveCountryIsoOverrideForApply(
        selectedSim: SimSelection,
        enableTikTokFix: Boolean,
    ): String? {
        if (selectedSim.subId < 0) return null
        val linkedIso = resolveIsoByMcc(selectedSim.mcc, selectedSim.countryIso)
        if (!enableTikTokFix) {
            clearTikTokRandomIso(selectedSim.subId)
            return linkedIso
        }
        // 仅国内 SIM 启用随机数字 ISO；海外 SIM 维持正常国家 ISO。
        if (!isChinaDomesticSim(selectedSim)) {
            clearTikTokRandomIso(selectedSim.subId)
            return linkedIso
        }
        return loadOrCreateTikTokRandomIso(selectedSim.subId)
    }

    fun resolveCountryIsoOverridePreview(
        selectedSim: SimSelection,
        map: Map<Feature, FeatureValue>,
    ): String? {
        if (selectedSim.subId < 0) return null
        val enableTikTokFix = (map[Feature.TIKTOK_NETWORK_FIX]?.data as? Boolean) == true
        return resolveCountryIsoOverrideForApply(selectedSim, enableTikTokFix)
    }

    // 系统信息状态流
    private val _systemInfo = MutableStateFlow(SystemInfo())
    val systemInfo: StateFlow<SystemInfo> = _systemInfo.asStateFlow()

    // Shizuku 运行状态流
    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.CHECKING)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus.asStateFlow()

    // 所有可用 SIM 卡列表流
    private val _allSimList = MutableStateFlow<List<SimSelection>>(emptyList())
    val allSimList: StateFlow<List<SimSelection>> = _allSimList.asStateFlow()

    // 失败日志（用于 issue 提交）
    private val _issueFailureLogs = MutableStateFlow("")
    val issueFailureLogs: StateFlow<String> = _issueFailureLogs.asStateFlow()

    // Shizuku Binder 接收监听器（服务连接/授权后触发）
    private val binderListener = Shizuku.OnBinderReceivedListener { updateShizukuStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { updateShizukuStatus() }

    init {
        pendingConfigRestoreAfterBoot = checkAndMarkBootChanged()
        loadSimList()
        loadSystemInfo()
        refreshIssueFailureLogs()
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
                loadSimListInternal()
            }
            if (status == ShizukuStatus.READY) {
                maybeRestoreSavedConfigurationAfterBoot()
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
        if (!shizukuReady) {
            val title = application.getString(R.string.all_sim)
            _allSimList.value = listOf(SimSelection(-1, "", "", -1, title))
            return false
        }
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
    suspend fun onApplyConfiguration(
        selectedSim: SimSelection,
        map: Map<Feature, FeatureValue>,
        countryMccOverride: String? = null,
    ): String? {
        // 构建传递给底层 ImsModifier 的配置 Bundle
        val carrierName: String? = null
        val enableTikTokFix = (map[Feature.TIKTOK_NETWORK_FIX]?.data ?: false) as Boolean
        val countryISO =
            if (selectedSim.subId == -1) null else resolveCountryIsoOverrideForApply(
                selectedSim,
                enableTikTokFix
            )
        val countryMcc: String? = null
        val countryMnc =
            if (selectedSim.subId == -1) null else selectedSim.mnc
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
            countryMcc,
            countryMnc,
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
        bundle.putBoolean(ImsModifier.BUNDLE_PREFER_PERSISTENT, canUsePersistentOverride)

        // 调用 Shizuku 服务进行实际修改
        val resultMsg = ShizukuProvider.overrideImsConfig(application, bundle)
        if (resultMsg == null) {
            // 仅在应用成功后保存配置，避免本地状态与系统状态不一致
            saveConfiguration(selectedSim.subId, map, countryMccOverride)
        }
        return resultMsg
    }

    /**
     * 将配置保存到 SharedPreferences 中以便下次加载。
     */
    private fun saveConfiguration(
        subId: Int,
        map: Map<Feature, FeatureValue>,
        countryMccOverride: String?,
    ) {
        val prefs = application.getSharedPreferences("sim_config_$subId", Context.MODE_PRIVATE)
        val keepTikTokRandomIso = prefs.getString(TIKTOK_RANDOM_ISO_PREF_KEY, null)
        val tiktokEnabled = (map[Feature.TIKTOK_NETWORK_FIX]?.data as? Boolean) == true
        prefs.edit {
            clear() // 清除旧配置
            map.forEach { (feature, value) ->
                when (value.valueType) {
                    FeatureValueType.BOOLEAN -> putBoolean(feature.name, value.data as Boolean)
                    FeatureValueType.STRING -> putString(feature.name, value.data as String)
                }
            }
            if (tiktokEnabled && !keepTikTokRandomIso.isNullOrBlank()) {
                putString(TIKTOK_RANDOM_ISO_PREF_KEY, keepTikTokRandomIso)
            } else {
                remove(TIKTOK_RANDOM_ISO_PREF_KEY)
            }
            remove(COUNTRY_MCC_PREF_KEY)
        }
    }

    fun loadSavedCountryMccOverride(subId: Int): String {
        if (subId < 0) return ""
        return application
            .getSharedPreferences("sim_config_$subId", Context.MODE_PRIVATE)
            .getString(COUNTRY_MCC_PREF_KEY, "")
            .orEmpty()
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

    suspend fun registerIms(subId: Int): ImsRegisterResult {
        if (subId < 0) return ImsRegisterResult(null, "invalid subId")
        val resultMsg = ShizukuProvider.restartImsRegistration(application, subId)
        if (resultMsg != null) {
            toast(application.getString(R.string.ims_restart_failed, resultMsg), false)
            return ImsRegisterResult(null, resultMsg)
        }
        val status = readImsRegistrationStatus(subId)
        if (status == true) {
            toast(application.getString(R.string.ims_register_success))
        } else {
            toast(application.getString(R.string.ims_register_pending), false)
        }
        return ImsRegisterResult(status, null)
    }

    suspend fun applyCaptivePortalCnUrls(): String? {
        val resultMsg = ShizukuProvider.applyCaptivePortalCnUrls(application)
        if (resultMsg != null) {
            appendSwitchFailureLog(
                action = "CAPTIVE_PORTAL_FIX",
                subId = null,
                stage = "apply_cn_urls",
                backendMessage = resultMsg
            )
        }
        return resultMsg
    }

    suspend fun restoreCaptivePortalDefaultUrls(): String? {
        val resultMsg = ShizukuProvider.restoreCaptivePortalDefaultUrls(application)
        if (resultMsg != null) {
            appendSwitchFailureLog(
                action = "CAPTIVE_PORTAL_RESTORE",
                subId = null,
                stage = "restore_default_urls",
                backendMessage = resultMsg
            )
        }
        return resultMsg
    }

    suspend fun queryCaptivePortalFixState(): CaptivePortalFixState? {
        val config = ShizukuProvider.queryCaptivePortalConfig(application) ?: return null
        val currentConfigReachable = isPortalConfigReachable(config.httpUrl, config.httpsUrl)
        val mode = when {
            config.isOverridden && currentConfigReachable -> CaptivePortalFixMode.CAN_RESTORE
            config.isOverridden -> CaptivePortalFixMode.NEED_FIX
            isDefaultPortalCheckReachable() -> CaptivePortalFixMode.NORMAL
            else -> CaptivePortalFixMode.NEED_FIX
        }
        return CaptivePortalFixState(
            mode = mode,
            httpUrl = config.httpUrl,
            httpsUrl = config.httpsUrl
        )
    }

    private suspend fun isDefaultPortalCheckReachable(): Boolean {
        return withContext(Dispatchers.IO) {
            DEFAULT_CAPTIVE_PORTAL_TEST_URLS.any { url -> isPortalUrlReachable(url) }
        }
    }

    private suspend fun isPortalConfigReachable(httpUrl: String, httpsUrl: String): Boolean {
        val targets = buildList {
            if (httpUrl.isNotBlank()) add(httpUrl)
            if (httpsUrl.isNotBlank()) add(httpsUrl)
        }
        if (targets.isEmpty()) return false
        return withContext(Dispatchers.IO) {
            targets.any { url -> isPortalUrlReachable(url) }
        }
    }

    private fun isPortalUrlReachable(url: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CAPTIVE_PORTAL_CHECK_TIMEOUT_MS
                readTimeout = CAPTIVE_PORTAL_CHECK_TIMEOUT_MS
                requestMethod = "GET"
                instanceFollowRedirects = false
                useCaches = false
            }
            val code = connection.responseCode
            code == HttpURLConnection.HTTP_NO_CONTENT
        } catch (t: Throwable) {
            Log.w(TAG, "portal reachability check failed: $url, msg=${t.message}")
            false
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 重置选中 SIM 卡的配置到运营商默认状态。
     */
    suspend fun onResetConfiguration(selectedSim: SimSelection): Boolean {
        val bundle = ImsModifier.buildResetBundle()
        bundle.putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, selectedSim.subId)
        bundle.putBoolean(ImsModifier.BUNDLE_PREFER_PERSISTENT, canUsePersistentOverride)
        val resultMsg = ShizukuProvider.overrideImsConfig(application, bundle)
        if (resultMsg == null) {
            application.getSharedPreferences(
                "sim_config_${selectedSim.subId}",
                Context.MODE_PRIVATE
            ).edit {
                remove(COUNTRY_MCC_PREF_KEY)
                remove(TIKTOK_RANDOM_ISO_PREF_KEY)
            }
            toast(application.getString(R.string.config_success_reset_message))
            return true
        }
        toast(application.getString(R.string.config_failed, resultMsg), false)
        return false
    }

    fun runShizukuDiagnostics(
        selectedSim: SimSelection,
        visibleSimList: List<SimSelection>,
        appFeatureMap: Map<Feature, FeatureValue>,
    ): Flow<String> = flow {
        suspend fun emitLine(line: String) {
            emit("[${nowShortTime()}] $line")
        }

        fun formatSimHeadline(sim: SimSelection): String {
            val iccTail = sim.iccId.takeLast(4).ifBlank { "----" }
            return "${sim.showTitle} | subId=${sim.subId} | ICCID尾号=$iccTail"
        }

        fun toOnOff(value: Boolean?): String {
            return when (value) {
                true -> "ON"
                false -> "OFF"
                null -> "N/A"
            }
        }

        fun resolvePortalModeTag(httpUrl: String, httpsUrl: String): String {
            val lowerHttp = httpUrl.lowercase(Locale.ROOT)
            val lowerHttps = httpsUrl.lowercase(Locale.ROOT)
            return if (lowerHttp.contains(".cn/") || lowerHttps.contains(".cn/")) "cn" else "com"
        }

        fun networkTypeName(type: Int): String {
            return when (type) {
                TelephonyManager.NETWORK_TYPE_NR -> "NR(5G)"
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE(4G)"
                19 -> "LTE_CA(4G+)"
                TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
                TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
                TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
                TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS(3G)"
                TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE(2G)"
                TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS(2G)"
                TelephonyManager.NETWORK_TYPE_GSM -> "GSM(2G)"
                TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
                TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
                TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
                TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
                TelephonyManager.NETWORK_TYPE_UNKNOWN -> "UNKNOWN"
                else -> "TYPE_$type"
            }
        }

        emitLine("=== 开始诊断：运营商/IMS 网络能力 ===")
        emitLine(formatSimHeadline(selectedSim))

        val binderReady = Shizuku.pingBinder()
        val permissionGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        emitLine("[1/8] Shizuku 检查: binder=${if (binderReady) "已连接" else "未连接"}, permission=${if (permissionGranted) "已授权" else "未授权"}")
        if (!binderReady || !permissionGranted) {
            emitLine("❌ 诊断中止：Shizuku 未就绪。")
            emitLine("=== 诊断结束 ===")
            return@flow
        }

        val realSims = visibleSimList.filter { it.subId >= 0 }
        emitLine("[2/8] SIM 拓扑: 检测到 ${realSims.size} 张可用 SIM")
        realSims.forEach { sim ->
            emitLine(
                "- ${formatSimHeadline(sim)} | MCC/MNC=${sim.mcc}/${sim.mnc}, ISO=${sim.countryIso.ifBlank { "-" }}"
            )
        }

        emitLine("[3/8] IMS 注册状态检测")
        val imsStatusBySubId = linkedMapOf<Int, Boolean?>()
        realSims.forEach { sim ->
            val status = runCatching { readImsRegistrationStatus(sim.subId) }.getOrNull()
            imsStatusBySubId[sim.subId] = status
            val text = when (status) {
                true -> "已注册"
                false -> "未注册"
                null -> "读取失败/未知"
            }
            emitLine("- subId=${sim.subId}: IMS $text")
        }

        emitLine("[4/8] CarrierConfig 关键能力读取 (目标 SIM)")
        val keyToLabel = linkedMapOf(
            "carrier_volte_available_bool" to "VoLTE",
            "carrier_wfc_ims_available_bool" to "VoWiFi",
            "carrier_vt_available_bool" to "ViLTE 视频通话",
            "carrier_supports_ss_over_ut_bool" to "UT补充服务",
            "carrier_cross_sim_ims_available_bool" to "跨SIM通话",
            "enable_cross_sim_calling_on_opportunistic_data_bool" to "机会数据跨 SIM 通话",
            "vonr_enabled_bool" to "VoNR",
            "vonr_setting_visibility_bool" to "VoNR 开关可见",
            "sim_country_iso_override_string" to "SIM ISO 覆盖",
        )
        val diagReadKeys = linkedSetOf<String>().apply {
            addAll(keyToLabel.keys)
            addAll(FeatureConfigMapper.readKeys)
        }.toTypedArray()
        val configBundle = runCatching {
            ShizukuProvider.readCarrierConfig(
                application,
                selectedSim.subId,
                diagReadKeys
            )
        }.getOrNull()
        if (configBundle == null) {
            emitLine("❌ 读取 CarrierConfig 失败")
        } else {
            keyToLabel.forEach { (key, label) ->
                val value = when {
                    configBundle.containsKey(key) -> configBundle.get(key)
                    else -> null
                }
                emitLine("- $label ($key) = ${value ?: "N/A"}")
            }
        }

        emitLine("[5/8] 实时网络状态 (目标 SIM)")
        val telephony = application.getSystemService(TelephonyManager::class.java)
            ?.createForSubscriptionId(selectedSim.subId)
        val dataTypeResult = runCatching {
            telephony?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
        val voiceTypeResult = runCatching {
            telephony?.voiceNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
        val dataType = dataTypeResult.getOrDefault(TelephonyManager.NETWORK_TYPE_UNKNOWN)
        val voiceType = voiceTypeResult.getOrDefault(TelephonyManager.NETWORK_TYPE_UNKNOWN)
        val realtime5g = dataType == TelephonyManager.NETWORK_TYPE_NR ||
            voiceType == TelephonyManager.NETWORK_TYPE_NR
        emitLine("- 当前数据制式 = ${networkTypeName(dataType)}")
        emitLine("- 当前语音制式 = ${networkTypeName(voiceType)}")
        emitLine("- 实时 5G 状态 = ${if (realtime5g) "ON" else "OFF"}")
        val realtimeReadError = dataTypeResult.exceptionOrNull() ?: voiceTypeResult.exceptionOrNull()
        if (realtimeReadError != null) {
            emitLine("- 实时网络读取受限: ${realtimeReadError.javaClass.simpleName} (${realtimeReadError.message ?: "no message"})")
        }
        emitLine("- 说明：实时驻网状态与 CarrierConfig 开关不是同一层含义。")

        emitLine("[6/8] App 开关值 vs CarrierConfig 读回 (目标 SIM)")
        if (configBundle == null) {
            emitLine("❌ 因 CarrierConfig 读取失败，无法映射功能项")
        } else {
            val mapped = FeatureConfigMapper.fromBundle(configBundle)
            val rows = listOf(
                Feature.VOLTE to "VoLTE",
                Feature.VOWIFI to "VoWiFi",
                Feature.VT to "ViLTE",
                Feature.CROSS_SIM to "跨SIM通话",
                Feature.UT to "UT补充服务",
                Feature.VONR to "VoNR",
                Feature.FIVE_G_NR to "5G NR",
                Feature.FIVE_G_PLUS_ICON to "5GA/5G+ 图标",
                Feature.SHOW_4G_FOR_LTE to "LTE显示为4G",
                Feature.TIKTOK_NETWORK_FIX to "TikTok 修复",
            )
            rows.forEach { (feature, label) ->
                val appEnabled = appFeatureMap[feature]?.data as? Boolean
                val systemEnabled = mapped[feature]?.data as? Boolean
                val mismatch = if (appEnabled != null && systemEnabled != null && appEnabled != systemEnabled) " ⚠️不一致" else ""
                emitLine("- $label | App=${toOnOff(appEnabled)} | CarrierConfig=${toOnOff(systemEnabled)}$mismatch")
            }
        }

        emitLine("[7/8] 网络验证与国家码覆盖状态")
        val captiveState = runCatching { queryCaptivePortalFixState() }.getOrNull()
        if (captiveState == null) {
            emitLine("- 网络验证状态读取失败")
        } else {
            emitLine("- 网络验证模式 = ${resolvePortalModeTag(captiveState.httpUrl, captiveState.httpsUrl)}")
            emitLine("- HTTP 验证地址 = ${captiveState.httpUrl}")
            emitLine("- HTTPS 验证地址 = ${captiveState.httpsUrl}")
        }
        val savedMcc = loadSavedCountryMccOverride(selectedSim.subId)
        emitLine("- 本地保存 MCC 覆盖 = ${savedMcc.ifBlank { "(空)" }}")

        emitLine("[8/8] 结论")
        val selectedImsStatus = imsStatusBySubId[selectedSim.subId]
        when (selectedImsStatus) {
            true -> emitLine("✅ 目标 SIM 当前 IMS 已注册，基础链路正常。")
            false -> emitLine("⚠️ 目标 SIM 当前 IMS 未注册，建议优先检查 VoLTE/VoWiFi/APN(ims) 与运营商侧开通状态。")
            null -> emitLine("⚠️ 目标 SIM 的 IMS 状态读取失败，建议重试并检查 Shizuku 授权状态。")
        }
        emitLine("=== 诊断结束 ===")
    }

    private fun toast(msg: String, short: Boolean = true) {
        toast?.cancel()
        toast =
            Toast.makeText(application, msg, if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG)
        toast?.show()
    }

    private fun checkAndMarkBootChanged(): Boolean {
        val currentBootCount = try {
            Settings.Global.getInt(
                application.contentResolver,
                Settings.Global.BOOT_COUNT,
                -1
            )
        } catch (t: Throwable) {
            Log.w(TAG, "failed to read boot count", t)
            -1
        }
        if (currentBootCount < 0) return false
        val lastBootCount = runtimePrefs.getInt(KEY_LAST_BOOT_COUNT, -1)
        runtimePrefs.edit { putInt(KEY_LAST_BOOT_COUNT, currentBootCount) }
        return lastBootCount != -1 && lastBootCount != currentBootCount
    }

    suspend fun appendSwitchFailureLog(
        action: String,
        subId: Int?,
        stage: String,
        backendMessage: String,
    ) {
        val normalizedMessage = backendMessage.trim()
        if (normalizedMessage.isBlank()) return
        withContext(Dispatchers.IO) {
            issueFailureLogMutex.withLock {
                val file = issueFailureLogFile()
                val currentLines = readIssueFailureLogRawLines(file)
                val fingerprint = buildFailureLogFingerprint(
                    action = action,
                    subId = subId,
                    stage = stage,
                    backendMessage = normalizedMessage
                )
                if (currentLines.any { it.substringBefore("|") == fingerprint }) {
                    _issueFailureLogs.value = extractIssueLogText(currentLines)
                    return@withLock
                }
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())
                val messageForStore = normalizedMessage.replace(Regex("\\s+"), " ")
                val line = buildString {
                    append(fingerprint)
                    append("|")
                    append(timestamp)
                    append(" | action=")
                    append(action)
                    append(" | subId=")
                    append(subId ?: "-")
                    append(" | stage=")
                    append(stage)
                    append(" | msg=")
                    append(messageForStore)
                }
                val updated = (currentLines + line).takeLast(ISSUE_FAILURE_LOG_MAX_LINES)
                file.parentFile?.mkdirs()
                file.writeText(updated.joinToString("\n"))
                _issueFailureLogs.value = extractIssueLogText(updated)
            }
        }
    }

    private fun refreshIssueFailureLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            issueFailureLogMutex.withLock {
                val file = issueFailureLogFile()
                val lines = readIssueFailureLogRawLines(file)
                _issueFailureLogs.value = extractIssueLogText(lines)
            }
        }
    }

    private fun issueFailureLogFile(): File {
        return File(application.cacheDir, ISSUE_FAILURE_LOG_FILE)
    }

    private fun readIssueFailureLogRawLines(file: File): List<String> {
        if (!file.exists()) return emptyList()
        return file.readLines().map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun extractIssueLogText(lines: List<String>): String {
        return lines.joinToString("\n") { line ->
            if (line.contains("|")) line.substringAfter("|").trim() else line
        }.trim()
    }

    private fun buildFailureLogFingerprint(
        action: String,
        subId: Int?,
        stage: String,
        backendMessage: String,
    ): String {
        val normalized = buildString {
            append(action.trim().lowercase(Locale.ROOT))
            append("|")
            append(subId ?: -99999)
            append("|")
            append(stage.trim().lowercase(Locale.ROOT))
            append("|")
            append(backendMessage.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " "))
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }.take(16)
    }

    private suspend fun maybeRestoreSavedConfigurationAfterBoot() {
        if (!pendingConfigRestoreAfterBoot || restoringConfigAfterBoot) return
        restoringConfigAfterBoot = true
        try {
            val result = restoreSavedConfigurationAfterBoot()
            pendingConfigRestoreAfterBoot = false
            if (result.attempted > 0) {
                Log.i(
                    TAG,
                    "auto restore saved config after boot finished: attempted=${result.attempted}, success=${result.success}, failed=${result.failed}"
                )
            }
        } finally {
            restoringConfigAfterBoot = false
        }
    }

    private suspend fun restoreSavedConfigurationAfterBoot(): BootRestoreResult {
        var attempted = 0
        var success = 0
        var failed = 0
        val simList = _allSimList.value.filter { it.subId >= 0 }
        for (sim in simList) {
            val saved = loadConfiguration(sim.subId) ?: continue
            attempted++
            val resultMsg = onApplyConfiguration(sim, saved)
            if (resultMsg == null) {
                success++
            } else {
                failed++
                Log.w(
                    TAG,
                    "auto restore saved config failed for subId=${sim.subId}, msg=$resultMsg"
                )
            }
        }
        return BootRestoreResult(attempted, success, failed)
    }
}
