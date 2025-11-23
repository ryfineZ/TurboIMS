package io.github.vvb2060.ims

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

private const val PREFS_NAME = "ims_config"

enum class ShizukuStatus {
    CHECKING,
    NOT_RUNNING,
    NO_PERMISSION,
    READY,
    NEED_UPDATE,
}

data class SimSelection(
    val subId: Int,
    val displayName: String,
    val carrierName: String,
    val simSlotIndex: Int,
    val showTitle: String = buildString {
        append("SIM ")
        append(simSlotIndex + 1)
        append(": ")
        append(displayName)
        append(" (${carrierName})")
    }
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _androidVersion = MutableStateFlow("")
    val androidVersion: StateFlow<String> = _androidVersion.asStateFlow()

    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.CHECKING)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus.asStateFlow()

    private val _allSimList = MutableStateFlow<List<SimSelection>>(emptyList())
    val allSimList: StateFlow<List<SimSelection>> = _allSimList.asStateFlow()

    private val _featureSwitches = MutableStateFlow<Map<Feature, Any>>(emptyMap())
    val featureSwitches: StateFlow<Map<Feature, Any>> = _featureSwitches.asStateFlow()

    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val binderListener = Shizuku.OnBinderReceivedListener { updateShizukuStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { updateShizukuStatus() }

    init {
        loadSimList()
        loadPreferences()
        updateAndroidVersionInfo()
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

    private fun loadPreferences() {
        viewModelScope.launch {
            val featureSwitches = linkedMapOf<Feature, Any>()
            for (feature in Feature.entries) {
                when (feature.valueType) {
                    FeatureValueType.STRING -> {
                        featureSwitches.put(feature, prefs.getString(feature.key, "")!!)
                    }

                    FeatureValueType.BOOLEAN -> {
                        featureSwitches.put(feature, prefs.getBoolean(feature.key, true))
                    }
                }
            }
            _featureSwitches.value = featureSwitches
        }
    }

    @SuppressLint("MissingPermission", "NewApi")
    fun loadSimList() {
        viewModelScope.launch {
            val sm = application.getSystemService(SubscriptionManager::class.java)
            val list = sm.activeSubscriptionInfoList!!
            val resultList = list.map {
                SimSelection(
                    it.subscriptionId,
                    it.displayName.toString(),
                    it.carrierName.toString(),
                    it.simSlotIndex,
                )
            }
                .toMutableList()
            resultList.add(0, SimSelection(-1, "", "", -1, "所有SIM"))
            _allSimList.value = resultList
        }
    }

    private fun updateAndroidVersionInfo() {
        viewModelScope.launch {
            val version = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            _androidVersion.value = version
        }
    }

    fun onFeatureSwitchChange(feature: Feature, value: Any) {
        Log.d(TAG, "onFeatureSwitchChange: $feature, $value")
        viewModelScope.launch {
            val updatedSwitches = _featureSwitches.value.toMutableMap()
            updatedSwitches[feature] = value
            _featureSwitches.value = updatedSwitches
        }
    }

    fun onApplyConfiguration(context: Context, selectedSim: SimSelection) {
        prefs.edit().apply {
            putInt("selected_subid", selectedSim.subId)
            _featureSwitches.value.forEach { (feature, value) ->
                when (value) {
                    is String -> putString(feature.key, value)
                    is Boolean -> putBoolean(feature.key, value)
                }
            }
            apply()
        }
        ShizukuProvider.startInstrument(context)
    }

    fun onResetConfiguration(context: Context) {
        ShizukuProvider.startInstrument(context, true)
    }
}
