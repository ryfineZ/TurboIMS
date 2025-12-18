package io.github.vvb2060.ims.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.vvb2060.ims.BuildConfig
import io.github.vvb2060.ims.R
import io.github.vvb2060.ims.ShizukuProvider
import io.github.vvb2060.ims.model.Feature
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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private var toast: Toast? = null

    private val _systemInfo = MutableStateFlow(SystemInfo())
    val systemInfo: StateFlow<SystemInfo> = _systemInfo.asStateFlow()

    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.CHECKING)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus.asStateFlow()

    private val _allSimList = MutableStateFlow<List<SimSelection>>(emptyList())
    val allSimList: StateFlow<List<SimSelection>> = _allSimList.asStateFlow()

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

    fun requestShizukuPermission(requestCode: Int) {
        viewModelScope.launch {
            if (Shizuku.isPreV11()) {
                _shizukuStatus.value = ShizukuStatus.NEED_UPDATE
            } else {
                Shizuku.requestPermission(requestCode)
            }
        }
    }

    fun loadDefaultPreferences(): Map<Feature, FeatureValue> {
        val featureSwitches = linkedMapOf<Feature, FeatureValue>()
        for (feature in Feature.entries) {
            featureSwitches.put(feature, FeatureValue(feature.defaultValue, feature.valueType))
        }
        return featureSwitches
    }

    fun loadSimList() {
        viewModelScope.launch {
            val simInfoList = ShizukuProvider.readSimInfoList(application)
            val resultList = simInfoList.toMutableList()
            val title = application.getString(R.string.all_sim)
            resultList.add(0, SimSelection(-1, "", "", -1, title))
            _allSimList.value = resultList
        }
    }

    private fun loadSystemInfo() {
        viewModelScope.launch {
            _systemInfo.value = SystemInfo(
                appVersionName = BuildConfig.VERSION_NAME,
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                systemVersion = Build.DISPLAY,
                securityPatchVersion = Build.VERSION.SECURITY_PATCH,
            )
        }
    }

    fun onApplyConfiguration(selectedSim: SimSelection, map: Map<Feature, FeatureValue>) {
        viewModelScope.launch {
            saveConfiguration(selectedSim.subId, map)

            val carrierName =
                if (selectedSim.subId == -1) null else map[Feature.CARRIER_NAME]?.data as String?
            val countryISO =
                if (selectedSim.subId == -1) null else map[Feature.COUNTRY_ISO]?.data as String?
            val enableVoLTE = map.getOrDefault(Feature.VOLTE, true) as Boolean
            val enableVoWiFi = map.getOrDefault(Feature.VOWIFI, true) as Boolean
            val enableVT = map.getOrDefault(Feature.VT, true) as Boolean
            val enableVoNR = map.getOrDefault(Feature.VONR, true) as Boolean
            val enableCrossSIM = map.getOrDefault(Feature.CROSS_SIM, true) as Boolean
            val enableUT = map.getOrDefault(Feature.UT, true) as Boolean
            val enable5GNR = map.getOrDefault(Feature.FIVE_G_NR, true) as Boolean
            val enable5GThreshold = map.getOrDefault(Feature.FIVE_G_THRESHOLDS, true) as Boolean

            val bundle = ImsModifier.buildBundle(
                carrierName,
                countryISO,
                enableVoLTE,
                enableVoWiFi,
                enableVT,
                enableVoNR,
                enableCrossSIM,
                enableUT,
                enable5GNR,
                enable5GThreshold
            )
            bundle.putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, selectedSim.subId)

            val resultMsg = ShizukuProvider.overrideImsConfig(application, bundle)
            if (resultMsg == null) {
                toast(application.getString(R.string.config_success_message))
            } else {
                toast(application.getString(R.string.config_failed, resultMsg), false)
            }
        }
    }

    private fun saveConfiguration(subId: Int, map: Map<Feature, FeatureValue>) {
        application.getSharedPreferences("sim_config_$subId", Context.MODE_PRIVATE).edit {
            clear() // Clear old config for this SIM
            map.forEach { (feature, value) ->
                when (value.valueType) {
                    FeatureValueType.BOOLEAN -> putBoolean(feature.name, value.data as Boolean)
                    FeatureValueType.STRING -> putString(feature.name, value.data as String)
                }
            }
        }
    }

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

    private fun toast(msg: String, short: Boolean = true) {
        toast?.cancel()
        toast =
            Toast.makeText(application, msg, if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG)
        toast?.show()
    }

}
