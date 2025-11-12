package io.github.vvb2060.ims

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

private const val PREFS_NAME = "ims_config"

data class MainUiState(
    val androidVersion: String = "",
    val shizukuStatus: ShizukuStatus = ShizukuStatus.CHECKING,
    val isQpr2Beta3OrHigher: Boolean = false,
    val selectedSim: SimSelection = SimSelection.SIM1,
    val featureSwitches: Map<Feature, Boolean> = Feature.values().associateWith { true },
    val isApplyButtonEnabled: Boolean = false,
    val showSimSelectionDialog: Boolean = false,
    val showConfigAppliedDialog: Boolean = false,
    val showShizukuUpdateDialog: Boolean = false,
)

enum class ShizukuStatus {
    CHECKING,
    NOT_RUNNING,
    NO_PERMISSION,
    READY
}

enum class SimSelection(val subId: Int) {
    SIM1(1),
    SIM2(2),
    ALL(-1)
}

enum class Feature(val key: String) {
    VOLTE("volte"),
    VOWIFI("vowifi"),
    VT("vt"),
    VONR("vonr"),
    CROSS_SIM("cross_sim"),
    UT("ut"),
    FIVE_G_NR("5g_nr")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val prefs: SharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val binderListener = Shizuku.OnBinderReceivedListener { updateShizukuStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { updateShizukuStatus() }

    init {
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
            val status = when {
                !Shizuku.pingBinder() -> ShizukuStatus.NOT_RUNNING
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> ShizukuStatus.NO_PERMISSION
                else -> ShizukuStatus.READY
            }
            _uiState.update {
                it.copy(
                    shizukuStatus = status,
                    isApplyButtonEnabled = status == ShizukuStatus.READY
                )
            }
        }
    }

    fun requestShizukuPermission(requestCode: Int) {
        if (Shizuku.isPreV11()) {
            _uiState.update { it.copy(showShizukuUpdateDialog = true) }
            return
        }
        Shizuku.requestPermission(requestCode)
    }

    private fun loadPreferences() {
        val featureSwitches = Feature.entries.associateWith {
            prefs.getBoolean(it.key, true)
        }
        _uiState.update { it.copy(featureSwitches = featureSwitches) }
    }

    private fun savePreferences() {
        prefs.edit().apply {
            uiState.value.featureSwitches.forEach { (feature, enabled) ->
                putBoolean(feature.key, enabled)
            }
            apply()
        }
    }

    private fun updateAndroidVersionInfo() {
        val version = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val isQpr2Beta3OrHigher = Build.VERSION.SDK_INT >= 36
        _uiState.update { it.copy(androidVersion = version, isQpr2Beta3OrHigher = isQpr2Beta3OrHigher) }
    }

    fun onFeatureSwitchChange(feature: Feature, isChecked: Boolean) {
        val updatedSwitches = _uiState.value.featureSwitches.toMutableMap()
        updatedSwitches[feature] = isChecked
        _uiState.update { it.copy(featureSwitches = updatedSwitches) }
    }

    fun onSelectSim(simSelection: SimSelection) {
        _uiState.update { it.copy(selectedSim = simSelection, showSimSelectionDialog = false) }
    }

    fun onApplyConfiguration(context: Context) {
        savePreferences()
        prefs.edit().putInt("selected_subid", uiState.value.selectedSim.subId).apply()
        ShizukuProvider.startInstrument(context)
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(showConfigAppliedDialog = true) }
        }
    }

    fun openSimSelectionDialog() {
        _uiState.update { it.copy(showSimSelectionDialog = true) }
    }

    fun dismissSimSelectionDialog() {
        _uiState.update { it.copy(showSimSelectionDialog = false) }
    }

    fun dismissConfigAppliedDialog() {
        _uiState.update { it.copy(showConfigAppliedDialog = false) }
    }
    fun dismissShizukuUpdateDialog() {
        _uiState.update { it.copy(showShizukuUpdateDialog = false) }
    }
}
