package io.github.vvb2060.ims.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vvb2060.ims.BuildConfig
import io.github.vvb2060.ims.R
import io.github.vvb2060.ims.model.Feature
import io.github.vvb2060.ims.model.FeatureValue
import io.github.vvb2060.ims.model.FeatureValueType
import io.github.vvb2060.ims.model.ShizukuStatus
import io.github.vvb2060.ims.model.SimSelection
import io.github.vvb2060.ims.model.SystemInfo
import io.github.vvb2060.ims.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val COUNTRY_ISO_OPTION_DEFAULT = "__default__"
private const val COUNTRY_ISO_OPTION_OTHER = "__other__"
private const val REPO_URL = "https://github.com/ryfineZ/TurboIMS"
private const val REPO_ISSUE_URL = "https://github.com/ryfineZ/TurboIMS/issues/new"
private const val REPO_OWNER = "ryfineZ"
private const val REPO_NAME = "TurboIMS"
private const val RELEASES_LATEST_API_URL =
    "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
private const val UPDATE_APK_MIME_TYPE = "application/vnd.android.package-archive"
private const val UNKNOWN_INSTALLER_SOURCE_SETTINGS_SCHEME = "package:"
private const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
private const val ALIPAY_PACKAGE_NAME = "com.eg.android.AlipayGphone"
private const val ALIPAY_DONATION_URL = "https://qr.alipay.com/2m610390t3ynw5ypko3li1b"
private const val ALIPAY_SCAN_QR_ACTIVITY =
    "com.alipay.mobile.quinox.splash.ShareScanQRDispenseActivity"
private const val DONATION_QR_MIME_TYPE = "image/png"

private data class ReleaseInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

private data class UpdateDialogState(
    val currentVersion: String,
    val latest: ReleaseInfo,
)

private data class ApplyResultDialogState(
    val success: Boolean,
    val message: String,
)

private data class SavedDonationQr(
    val uri: Uri,
    val path: String,
)

private data class CountryIsoOption(
    val key: String,
    val isoCode: String?,
    val mcc: String?,
    val labelRes: Int,
)

private val countryIsoOptions = listOf(
    CountryIsoOption(COUNTRY_ISO_OPTION_DEFAULT, null, null, R.string.country_iso_option_default),
    CountryIsoOption("cn", "cn", "460", R.string.country_iso_option_china_mainland),
    CountryIsoOption("hk", "hk", "454", R.string.country_iso_option_hong_kong),
    CountryIsoOption("tw", "tw", "466", R.string.country_iso_option_taiwan),
    CountryIsoOption("us", "us", "310-316", R.string.country_iso_option_us),
    CountryIsoOption("jp", "jp", "440-441", R.string.country_iso_option_japan),
    CountryIsoOption("gb", "gb", "234-235", R.string.country_iso_option_uk),
    CountryIsoOption("kr", "kr", "450", R.string.country_iso_option_korea),
    CountryIsoOption("sg", "sg", "525", R.string.country_iso_option_singapore),
    CountryIsoOption(COUNTRY_ISO_OPTION_OTHER, null, null, R.string.country_iso_option_other),
)

private val fiveGFeatureSet = setOf(
    Feature.FIVE_G_NR,
    Feature.FIVE_G_THRESHOLDS,
    Feature.FIVE_G_PLUS_ICON,
    Feature.VONR,
)

private val fourGFeatureSet = setOf(
    Feature.VOLTE,
    Feature.SHOW_4G_FOR_LTE,
)

private fun switchFeatureCategoryOrder(feature: Feature): Int {
    return when {
        fiveGFeatureSet.contains(feature) -> 0
        fourGFeatureSet.contains(feature) -> 1
        else -> 2
    }
}

private fun defaultFeatureValue(feature: Feature): FeatureValue {
    return FeatureValue(feature.defaultValue, feature.valueType)
}

private fun buildCompleteFeatureMap(source: Map<Feature, FeatureValue>): LinkedHashMap<Feature, FeatureValue> {
    val completed = linkedMapOf<Feature, FeatureValue>()
    Feature.entries.forEach { feature ->
        completed[feature] = source[feature] ?: defaultFeatureValue(feature)
    }
    return completed
}

private fun syncFeatureState(
    target: MutableMap<Feature, FeatureValue>,
    source: Map<Feature, FeatureValue>,
) {
    target.clear()
    target.putAll(buildCompleteFeatureMap(source))
}

private fun buildIssueBody(
    context: Context,
    systemInfo: SystemInfo,
    shizukuStatus: ShizukuStatus,
    issueFailureLogs: String,
): String {
    val shizukuStatusText = when (shizukuStatus) {
        ShizukuStatus.CHECKING -> context.getString(R.string.shizuku_checking)
        ShizukuStatus.NOT_RUNNING -> context.getString(R.string.shizuku_not_running)
        ShizukuStatus.NO_PERMISSION -> context.getString(R.string.shizuku_no_permission)
        ShizukuStatus.READY -> context.getString(R.string.shizuku_ready)
        else -> "UNKNOWN"
    }
    return buildString {
        appendLine("App Version: ${systemInfo.appVersionName}")
        appendLine("Device Model: ${systemInfo.deviceModel}")
        appendLine("Android Version: ${systemInfo.androidVersion}")
        appendLine("Patch Date: ${systemInfo.securityPatchVersion}")
        appendLine("Shizuku Status: $shizukuStatusText")
        if (issueFailureLogs.isNotBlank()) {
            appendLine()
            appendLine("Switch Failure Logs:")
            append(issueFailureLogs)
        }
    }.trim()
}

class MainActivity : BaseActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingUpdateDownloadId: Long = -1L
    private var pendingUpdateFileName: String? = null
    private var updateReceiverRegistered = false
    private val updateDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == -1L || downloadId != pendingUpdateDownloadId) return
            pendingUpdateDownloadId = -1L
            handleUpdateDownloadComplete(downloadId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(updateDownloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        updateReceiverRegistered = true
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        val systemInfo by viewModel.systemInfo.collectAsStateWithLifecycle()
        val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
        val allSimList by viewModel.allSimList.collectAsStateWithLifecycle()
        val issueFailureLogs by viewModel.issueFailureLogs.collectAsStateWithLifecycle()
        val uriHandler = LocalUriHandler.current
        val clipboardManager = context.getSystemService(ClipboardManager::class.java)

        val scope = rememberCoroutineScope()
        var selectedSim by remember { mutableStateOf<SimSelection?>(null) }
        var showShizukuUpdateDialog by remember { mutableStateOf(false) }
        var pendingAutoSelectSimAfterReady by remember { mutableStateOf(false) }
        val imsRegistrationStatusMap = remember { mutableStateMapOf<Int, Boolean?>() }
        val imsRegistrationLoadingMap = remember { mutableStateMapOf<Int, Boolean>() }
        var applyingConfiguration by remember { mutableStateOf(false) }
        var checkingUpdate by remember { mutableStateOf(false) }
        var updateDialogState by remember { mutableStateOf<UpdateDialogState?>(null) }
        var applyResultDialogState by remember { mutableStateOf<ApplyResultDialogState?>(null) }
        var showDonationSheet by remember { mutableStateOf(false) }
        var donationFeedbackMessage by remember { mutableStateOf<String?>(null) }
        val featureSwitches = remember { mutableStateMapOf<Feature, FeatureValue>() }
        val committedFeatureSwitches = remember { mutableStateMapOf<Feature, FeatureValue>() }
        val submitIssueAction: () -> Unit = {
            val issueBody = buildIssueBody(
                context = context,
                systemInfo = systemInfo,
                shizukuStatus = shizukuStatus,
                issueFailureLogs = issueFailureLogs
            )
            clipboardManager?.setPrimaryClip(
                ClipData.newPlainText(
                    "turboims_issue_info",
                    issueBody
                )
            )
            Toast.makeText(context, R.string.issue_info_copied, Toast.LENGTH_SHORT).show()
            uriHandler.openUri(REPO_ISSUE_URL)
        }

        LaunchedEffect(shizukuStatus) {
            if (shizukuStatus == ShizukuStatus.NEED_UPDATE) {
                showShizukuUpdateDialog = true
            }
            pendingAutoSelectSimAfterReady = shizukuStatus == ShizukuStatus.READY
        }
        LaunchedEffect(allSimList) {
            val validSubIds = allSimList.filter { it.subId >= 0 }.map { it.subId }.toSet()
            imsRegistrationStatusMap.keys.toList()
                .filterNot { validSubIds.contains(it) }
                .forEach { imsRegistrationStatusMap.remove(it) }
            imsRegistrationLoadingMap.keys.toList()
                .filterNot { validSubIds.contains(it) }
                .forEach { imsRegistrationLoadingMap.remove(it) }
            val currentSelected = selectedSim
            if (currentSelected == null) {
                selectedSim = allSimList.firstOrNull()
                return@LaunchedEffect
            }
            val stillExists = allSimList.any {
                it.subId == currentSelected.subId && it.simSlotIndex == currentSelected.simSlotIndex
            }
            if (!stillExists) {
                selectedSim = allSimList.firstOrNull()
            }
        }
        LaunchedEffect(allSimList, pendingAutoSelectSimAfterReady) {
            if (!pendingAutoSelectSimAfterReady) return@LaunchedEffect
            val firstSingleSim = allSimList.firstOrNull { it.subId >= 0 } ?: return@LaunchedEffect
            if (selectedSim == null || selectedSim?.subId == -1) {
                selectedSim = firstSingleSim
            }
            pendingAutoSelectSimAfterReady = false
        }
        LaunchedEffect(selectedSim, shizukuStatus, allSimList) {
            if (selectedSim != null) {
                committedFeatureSwitches.clear()
                val currentConfig = if (shizukuStatus == ShizukuStatus.READY && selectedSim!!.subId >= 0) {
                    viewModel.loadCurrentConfiguration(selectedSim!!.subId)
                } else {
                    null
                }
                if (currentConfig != null) {
                    committedFeatureSwitches.putAll(currentConfig)
                } else {
                    val savedConfig = viewModel.loadConfiguration(selectedSim!!.subId)
                    if (savedConfig != null) {
                        committedFeatureSwitches.putAll(savedConfig)
                    } else {
                        committedFeatureSwitches.putAll(viewModel.loadDefaultPreferences())
                    }
                }
                syncFeatureState(featureSwitches, committedFeatureSwitches)
                if (selectedSim!!.subId >= 0) {
                    imsRegistrationStatusMap[selectedSim!!.subId] =
                        if (shizukuStatus == ShizukuStatus.READY) {
                            viewModel.readImsRegistrationStatus(selectedSim!!.subId)
                        } else {
                            null
                        }
                } else {
                    allSimList.filter { it.subId >= 0 }.forEach { sim ->
                        imsRegistrationStatusMap[sim.subId] =
                            if (shizukuStatus == ShizukuStatus.READY) {
                                viewModel.readImsRegistrationStatus(sim.subId)
                            } else {
                                null
                            }
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                stringResource(id = R.string.app_name),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(id = R.string.for_pixel),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                SystemInfoCard(
                    systemInfo,
                    shizukuStatus,
                    onRefresh = {
                        viewModel.updateShizukuStatus()
                        if (shizukuStatus == ShizukuStatus.READY) {
                            scope.launch {
                                val hasValidSim = viewModel.refreshSimListNow()
                                val messageRes = if (hasValidSim) {
                                    R.string.sim_list_refresh
                                } else {
                                    R.string.sim_list_refresh_failed_restart_shizuku
                                }
                                val duration = if (hasValidSim) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                                Toast.makeText(context, messageRes, duration).show()
                            }
                        }
                    },
                    onRequestShizukuPermission = {
                        viewModel.requestShizukuPermission(0)
                    },
                    onLogcatClick = {
                        startActivity(
                            Intent(
                                this@MainActivity,
                                LogcatActivity::class.java
                            )
                        )
                    },
                    checkingUpdate = checkingUpdate,
                    onCheckUpdate = {
                        if (checkingUpdate) return@SystemInfoCard
                        scope.launch {
                            checkingUpdate = true
                            Toast.makeText(context, R.string.update_checking, Toast.LENGTH_SHORT).show()
                            val currentVersion = BuildConfig.VERSION_NAME
                            val result = fetchLatestReleaseInfo()
                            checkingUpdate = false
                            val release = result.getOrNull()
                            if (release == null) {
                                Toast.makeText(
                                    context,
                                    this@MainActivity.getString(
                                        R.string.update_check_failed,
                                        result.exceptionOrNull()?.message ?: "unknown error"
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }
                            if (!isVersionNewer(release.version, currentVersion)) {
                                Toast.makeText(context, R.string.update_latest, Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            updateDialogState = UpdateDialogState(
                                currentVersion = currentVersion,
                                latest = release
                            )
                        }
                    },
                    onIssueClick = submitIssueAction,
                    onDonateClick = {
                        showDonationSheet = true
                    },
                )
                if (shizukuStatus == ShizukuStatus.READY) {
                    SimCardSelectionCard(selectedSim, allSimList, onSelectSim = {
                        selectedSim = it
                    }, onRefreshSimList = {
                        scope.launch {
                            val hasValidSim = viewModel.refreshSimListNow()
                            val messageRes = if (hasValidSim) {
                                R.string.sim_list_refresh
                            } else {
                                R.string.sim_list_refresh_failed_restart_shizuku
                            }
                            val duration = if (hasValidSim) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                            Toast.makeText(context, messageRes, duration).show()
                        }
                    })
                    FeaturesCard(
                        isSelectAllSim = selectedSim?.subId == -1,
                        allSimList = allSimList,
                        selectedSim = selectedSim,
                        imsRegistrationStatusBySubId = imsRegistrationStatusMap,
                        imsRegistrationLoadingBySubId = imsRegistrationLoadingMap,
                        featureSwitchesEnabled = !applyingConfiguration,
                        onImsRegistrationToggle = { subId, targetChecked ->
                            if (!targetChecked) return@FeaturesCard
                            if (applyingConfiguration) return@FeaturesCard
                            if (shizukuStatus != ShizukuStatus.READY) {
                                Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                                return@FeaturesCard
                            }
                            val sim = allSimList.firstOrNull { it.subId == subId }
                            if (sim == null) {
                                Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                return@FeaturesCard
                            }
                            scope.launch {
                                applyingConfiguration = true
                                imsRegistrationLoadingMap[subId] = true
                                val oldVolteUi =
                                    featureSwitches[Feature.VOLTE] ?: defaultFeatureValue(Feature.VOLTE)
                                val oldVowifiUi =
                                    featureSwitches[Feature.VOWIFI] ?: defaultFeatureValue(Feature.VOWIFI)
                                val oldVolteCommitted =
                                    committedFeatureSwitches[Feature.VOLTE] ?: defaultFeatureValue(Feature.VOLTE)
                                val oldVowifiCommitted =
                                    committedFeatureSwitches[Feature.VOWIFI] ?: defaultFeatureValue(Feature.VOWIFI)
                                try {
                                    val enabledValue = FeatureValue(true, FeatureValueType.BOOLEAN)
                                    featureSwitches[Feature.VOLTE] = enabledValue
                                    featureSwitches[Feature.VOWIFI] = enabledValue
                                    committedFeatureSwitches[Feature.VOLTE] = enabledValue
                                    committedFeatureSwitches[Feature.VOWIFI] = enabledValue
                                    Toast.makeText(
                                        context,
                                        R.string.ims_register_apply_then_register,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val applyResultMsg = viewModel.onApplyConfiguration(
                                        sim,
                                        buildCompleteFeatureMap(committedFeatureSwitches)
                                    )
                                    if (applyResultMsg != null) {
                                        viewModel.appendSwitchFailureLog(
                                            action = "IMS_REGISTER",
                                            subId = sim.subId,
                                            stage = "apply_before_register",
                                            backendMessage = applyResultMsg
                                        )
                                        featureSwitches[Feature.VOLTE] = oldVolteUi
                                        featureSwitches[Feature.VOWIFI] = oldVowifiUi
                                        committedFeatureSwitches[Feature.VOLTE] = oldVolteCommitted
                                        committedFeatureSwitches[Feature.VOWIFI] = oldVowifiCommitted
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.ims_register_apply_failed, applyResultMsg),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch
                                    }
                                    val registerResult = viewModel.registerIms(sim.subId)
                                    imsRegistrationStatusMap[subId] = registerResult.registered
                                    if (registerResult.backendErrorMessage != null) {
                                        viewModel.appendSwitchFailureLog(
                                            action = "IMS_REGISTER",
                                            subId = sim.subId,
                                            stage = "restart_ims",
                                            backendMessage = registerResult.backendErrorMessage
                                        )
                                    }
                                } finally {
                                    imsRegistrationLoadingMap[subId] = false
                                    applyingConfiguration = false
                                }
                            }
                        },
                        featureSwitches,
                        onFeatureSwitchChange = { feature, value ->
                            when (feature.valueType) {
                                FeatureValueType.STRING -> {
                                    featureSwitches[feature] = value
                                }

                                FeatureValueType.BOOLEAN -> {
                                    val sim = selectedSim
                                    val previousUiValue =
                                        featureSwitches[feature] ?: defaultFeatureValue(feature)
                                    val previousCommittedValue =
                                        committedFeatureSwitches[feature] ?: defaultFeatureValue(feature)
                                    featureSwitches[feature] = value
                                    committedFeatureSwitches[feature] = value
                                    if (applyingConfiguration) {
                                        featureSwitches[feature] = previousUiValue
                                        committedFeatureSwitches[feature] = previousCommittedValue
                                        return@FeaturesCard
                                    }
                                    if (sim == null || shizukuStatus != ShizukuStatus.READY) {
                                        featureSwitches[feature] = previousUiValue
                                        committedFeatureSwitches[feature] = previousCommittedValue
                                        Toast.makeText(
                                            context,
                                            R.string.shizuku_not_running_msg,
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@FeaturesCard
                                    }
                                    scope.launch {
                                        applyingConfiguration = true
                                        try {
                                            val resultMsg = viewModel.onApplyConfiguration(
                                                sim,
                                                buildCompleteFeatureMap(committedFeatureSwitches)
                                            )
                                            if (resultMsg != null) {
                                                if ((value.data as? Boolean) == true) {
                                                    viewModel.appendSwitchFailureLog(
                                                        action = feature.name,
                                                        subId = sim.subId.takeIf { it >= 0 },
                                                        stage = "apply_switch_enable",
                                                        backendMessage = resultMsg
                                                    )
                                                }
                                                featureSwitches[feature] = previousUiValue
                                                committedFeatureSwitches[feature] = previousCommittedValue
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.config_failed, resultMsg),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } finally {
                                            applyingConfiguration = false
                                        }
                                    }
                                }
                            }
                        },
                        loadCurrentConfig = {
                            scope.launch {
                                if (applyingConfiguration) return@launch
                                if (selectedSim == null) return@launch
                                if (shizukuStatus != ShizukuStatus.READY) {
                                    Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                if (selectedSim!!.subId < 0) {
                                    Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                applyingConfiguration = true
                                try {
                                    val currentConfig = viewModel.loadCurrentConfiguration(selectedSim!!.subId)
                                    if (currentConfig != null) {
                                        syncFeatureState(committedFeatureSwitches, currentConfig)
                                        syncFeatureState(featureSwitches, committedFeatureSwitches)
                                        Toast.makeText(context, R.string.load_config_current_success, Toast.LENGTH_SHORT).show()
                                    } else {
                                        syncFeatureState(committedFeatureSwitches, viewModel.loadDefaultPreferences())
                                        syncFeatureState(featureSwitches, committedFeatureSwitches)
                                        Toast.makeText(context, R.string.load_config_default_success, Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    applyingConfiguration = false
                                }
                            }
                        },
                        resetFeatures = {
                            val sim = selectedSim
                            if (sim == null || sim.subId < 0) {
                                Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                return@FeaturesCard
                            }
                            if (shizukuStatus != ShizukuStatus.READY) {
                                Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                                return@FeaturesCard
                            }
                            if (applyingConfiguration) {
                                return@FeaturesCard
                            }
                            scope.launch {
                                applyingConfiguration = true
                                try {
                                    val success = viewModel.onResetConfiguration(sim)
                                    if (!success) return@launch
                                    val currentConfig = viewModel.loadCurrentConfiguration(sim.subId)
                                    if (currentConfig != null) {
                                        syncFeatureState(committedFeatureSwitches, currentConfig)
                                        syncFeatureState(featureSwitches, committedFeatureSwitches)
                                        imsRegistrationStatusMap[sim.subId] =
                                            viewModel.readImsRegistrationStatus(sim.subId)
                                    } else {
                                        syncFeatureState(committedFeatureSwitches, viewModel.loadDefaultPreferences())
                                        syncFeatureState(featureSwitches, committedFeatureSwitches)
                                    }
                                } finally {
                                    applyingConfiguration = false
                                }
                            }
                        }
                    )
                    if (selectedSim?.subId != -1) {
                        MainActionButtons(
                            onApplyConfiguration = {
                                val sim = selectedSim
                                if (sim == null) {
                                    Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                    return@MainActionButtons
                                }
                                if (shizukuStatus != ShizukuStatus.READY) {
                                    Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                                    return@MainActionButtons
                                }
                                if (applyingConfiguration) {
                                    return@MainActionButtons
                                }
                                scope.launch {
                                    applyingConfiguration = true
                                    Toast.makeText(context, R.string.config_apply_in_progress, Toast.LENGTH_SHORT).show()
                                    val mapToApply = buildCompleteFeatureMap(featureSwitches)
                                    val resultMsg = viewModel.onApplyConfiguration(sim, mapToApply)
                                    applyingConfiguration = false
                                    applyResultDialogState = if (resultMsg == null) {
                                        syncFeatureState(committedFeatureSwitches, mapToApply)
                                        ApplyResultDialogState(
                                            success = true,
                                            message = context.getString(R.string.config_apply_success_message)
                                        )
                                    } else {
                                        ApplyResultDialogState(
                                            success = false,
                                            message = context.getString(R.string.config_failed, resultMsg)
                                        )
                                    }
                                }
                            },
                            onDumpConfig = {
                                val sim = selectedSim
                                if (sim == null || sim.subId < 0) {
                                    Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                    return@MainActionButtons
                                }
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        DumpActivity::class.java
                                    ).putExtra(DumpActivity.EXTRA_SUB_ID, sim.subId)
                                )
                            },
                            applyingConfiguration = applyingConfiguration
                        )
                    }
                }
                if (issueFailureLogs.isNotBlank()) {
                    IssueReportHintCard(
                        onSubmitIssue = submitIssueAction
                    )
                }
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))

                if (showShizukuUpdateDialog) {
                    ShizukuUpdateDialog {
                        showShizukuUpdateDialog = false
                    }
                }
                if (updateDialogState != null) {
                    val state = updateDialogState!!
                    AlertDialog(
                        onDismissRequest = { updateDialogState = null },
                        title = {
                            Text(stringResource(R.string.update_found_title, state.latest.version))
                        },
                        text = {
                            Text(
                                text = stringResource(
                                    R.string.update_found_message,
                                    state.currentVersion,
                                    state.latest.version
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    updateDialogState = null
                                    startUpdateDownload(state.latest)
                                }
                            ) {
                                Text(stringResource(R.string.update_download_install))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { updateDialogState = null }) {
                                Text(stringResource(id = android.R.string.cancel))
                            }
                        }
                    )
                }
                if (applyResultDialogState != null) {
                    val state = applyResultDialogState!!
                    AlertDialog(
                        onDismissRequest = { applyResultDialogState = null },
                        title = {
                            Text(
                                text = stringResource(
                                    if (state.success) {
                                        R.string.config_apply_success_title
                                    } else {
                                        R.string.config_apply_failed_title
                                    }
                                )
                            )
                        },
                        text = {
                            Text(text = state.message)
                        },
                        confirmButton = {
                            TextButton(onClick = { applyResultDialogState = null }) {
                                Text(stringResource(id = android.R.string.ok))
                            }
                        },
                        dismissButton = if (state.success) {
                            {
                                TextButton(
                                    onClick = {
                                        applyResultDialogState = null
                                        showDonationSheet = true
                                    }
                                ) {
                                    Text(stringResource(R.string.donation_action))
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
                if (showDonationSheet) {
                    DonationBottomSheet(
                        onDismissRequest = {
                            showDonationSheet = false
                        },
                        onOpenWeChat = {
                            val savedQr = saveDonationQrToGallery(
                                context = context,
                                imageRes = R.drawable.donate_wechat,
                                filePrefix = "turboims_wechat",
                            )
                            donationFeedbackMessage = if (savedQr == null) {
                                context.getString(R.string.donation_save_failed)
                            } else {
                                val opened = openWeChatDonationPage(context)
                                if (opened) {
                                    context.getString(
                                        R.string.donation_open_wechat_hint_with_path,
                                        savedQr.path
                                    )
                                } else {
                                    context.getString(R.string.donation_open_wechat_failed)
                                }
                            }
                            showDonationSheet = false
                            Toast.makeText(context, donationFeedbackMessage, Toast.LENGTH_LONG).show()
                        },
                        onOpenAlipay = {
                            val savedQr = saveDonationQrToGallery(
                                context = context,
                                imageRes = R.drawable.donate_alipay,
                                filePrefix = "turboims_alipay",
                            )
                            donationFeedbackMessage = if (savedQr == null) {
                                context.getString(R.string.donation_save_failed)
                            } else {
                                val opened = openAlipayDonationPage(context, savedQr.uri)
                                if (opened) {
                                    context.getString(
                                        R.string.donation_open_alipay_hint_with_path,
                                        savedQr.path
                                    )
                                } else {
                                    context.getString(R.string.donation_open_alipay_failed)
                                }
                            }
                            showDonationSheet = false
                            Toast.makeText(context, donationFeedbackMessage, Toast.LENGTH_LONG).show()
                        },
                        onSaveImage = { imageRes, filePrefix ->
                            val savedPath = saveDonationQrToGallery(context, imageRes, filePrefix)
                            donationFeedbackMessage = if (savedPath != null) {
                                context.getString(R.string.donation_save_success_with_path, savedPath.path)
                            } else {
                                context.getString(R.string.donation_save_failed)
                            }
                            showDonationSheet = false
                            Toast.makeText(context, donationFeedbackMessage, Toast.LENGTH_LONG).show()
                        },
                    )
                }
                if (donationFeedbackMessage != null) {
                    AlertDialog(
                        onDismissRequest = { donationFeedbackMessage = null },
                        title = {
                            Text(text = stringResource(R.string.donation_feedback_title))
                        },
                        text = {
                            Text(text = donationFeedbackMessage!!)
                        },
                        confirmButton = {
                            TextButton(onClick = { donationFeedbackMessage = null }) {
                                Text(stringResource(id = android.R.string.ok))
                            }
                        }
                    )
                }
            }
        }
    }

    private fun startUpdateDownload(release: ReleaseInfo) {
        val manager = getSystemService(DownloadManager::class.java)
        if (manager == null) {
            Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show()
            return
        }
        val fileName = buildUpdateApkFileName(release.version)
        val request = DownloadManager.Request(release.downloadUrl.toUri())
            .setTitle("Turbo IMS ${release.version}")
            .setDescription(release.releaseNotes.ifBlank { release.version })
            .setMimeType(UPDATE_APK_MIME_TYPE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName)
        runCatching {
            pendingUpdateDownloadId = manager.enqueue(request)
            pendingUpdateFileName = fileName
        }.onSuccess {
            Toast.makeText(this, R.string.update_download_started, Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun handleUpdateDownloadComplete(downloadId: Long) {
        val manager = getSystemService(DownloadManager::class.java)
        if (manager == null) {
            Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show()
            return
        }
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = manager.query(query) ?: run {
            Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show()
            return
        }
        cursor.use {
            if (!it.moveToFirst()) {
                Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show()
                return
            }
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    Toast.makeText(this, R.string.update_download_complete, Toast.LENGTH_SHORT).show()
                    installDownloadedApk(downloadId)
                }

                else -> {
                    val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    Toast.makeText(
                        this,
                        getString(R.string.update_download_error_reason, reason.toString()),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun installDownloadedApk(downloadId: Long) {
        if (!packageManager.canRequestPackageInstalls()) {
            Toast.makeText(this, R.string.update_install_permission_required, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    (UNKNOWN_INSTALLER_SOURCE_SETTINGS_SCHEME + packageName).toUri()
                )
            )
            return
        }

        val manager = getSystemService(DownloadManager::class.java)
        var uri = manager?.getUriForDownloadedFile(downloadId)
        if (uri == null) {
            val fileName = pendingUpdateFileName ?: return
            val apkFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (!apkFile.exists()) {
                Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show()
                return
            }
            uri = FileProvider.getUriForFile(this, "$packageName.logcat_fileprovider", apkFile)
        }

        val installIntent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, UPDATE_APK_MIME_TYPE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { startActivity(installIntent) }.onFailure {
            Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun fetchLatestReleaseInfo(): Result<ReleaseInfo> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(RELEASES_LATEST_API_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "$REPO_NAME-UpdateChecker")
                }
                try {
                    val conn = connection
                    val responseCode = conn.responseCode
                    if (responseCode !in 200..299) {
                        throw IllegalStateException("HTTP $responseCode")
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val tagName = json.optString("tag_name").ifBlank {
                        json.optString("name")
                    }
                    val releaseNotes = json.optString("body", "")
                    val assets = json.optJSONArray("assets")
                    var apkUrl: String? = null
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.optJSONObject(i) ?: continue
                            val url = asset.optString("browser_download_url")
                            if (url.endsWith(".apk", ignoreCase = true)) {
                                apkUrl = url
                                break
                            }
                        }
                    }
                    if (tagName.isBlank()) {
                        throw IllegalStateException("invalid release tag")
                    }
                    if (apkUrl.isNullOrBlank()) {
                        throw IllegalStateException(getString(R.string.update_no_apk))
                    }
                    ReleaseInfo(tagName, apkUrl, releaseNotes)
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateShizukuStatus()
    }

    override fun onDestroy() {
        if (updateReceiverRegistered) {
            unregisterReceiver(updateDownloadReceiver)
            updateReceiverRegistered = false
        }
        super.onDestroy()
    }
}

/**
 *系统信息卡片
 * 显示软件版本、Android 版本、Shizuku 状态等。
 */
@Composable
fun SystemInfoCard(
    systemInfo: SystemInfo,
    shizukuStatus: ShizukuStatus,
    onRefresh: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    checkingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onLogcatClick: () -> Unit,
    onIssueClick: () -> Unit,
    onDonateClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val shizukuStatusText = when (shizukuStatus) {
        ShizukuStatus.CHECKING -> stringResource(R.string.shizuku_checking)
        ShizukuStatus.NOT_RUNNING -> stringResource(R.string.shizuku_not_running)
        ShizukuStatus.NO_PERMISSION -> stringResource(R.string.shizuku_no_permission)
        ShizukuStatus.READY -> stringResource(R.string.shizuku_ready)
        else -> ""
    }
    val shizukuStatusColor = when (shizukuStatus) {
        ShizukuStatus.NOT_RUNNING -> Color.Red
        ShizukuStatus.NO_PERMISSION -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(id = R.string.system_info),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.weight(1F))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    HeaderActionChip(
                        icon = painterResource(R.drawable.ic_github),
                        label = stringResource(R.string.action_repo),
                        onClick = {
                            uriHandler.openUri(REPO_URL)
                        },
                    )
                    HeaderActionChip(
                        icon = painterResource(R.drawable.ic_issue),
                        label = stringResource(R.string.action_issue),
                        onClick = onIssueClick,
                    )
                    HeaderActionChip(
                        icon = painterResource(R.drawable.ic_update),
                        label = stringResource(R.string.action_update),
                        enabled = !checkingUpdate,
                        onClick = onCheckUpdate,
                    )
                    HeaderActionChip(
                        icon = painterResource(R.drawable.ic_log),
                        label = stringResource(R.string.action_logcat),
                        onClick = onLogcatClick,
                    )
                }
            }
            Text(
                stringResource(R.string.app_version, systemInfo.appVersionName),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.device_model, systemInfo.deviceModel),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.android_version, systemInfo.androidVersion),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.security_patch_version, systemInfo.securityPatchVersion),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.shizuku_status, shizukuStatusText),
                    fontSize = 14.sp,
                    color = shizukuStatusColor
                )
                FeatureActionChip(
                    icon = Icons.Rounded.Cached,
                    label = stringResource(id = R.string.refresh_permission),
                    onClick = onRefresh,
                )
            }
            if (shizukuStatus == ShizukuStatus.NO_PERMISSION) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRequestShizukuPermission) {
                    Text(text = stringResource(id = R.string.request_permission))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.donation_support_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDonateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.donation_action),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HeaderActionChip(
    icon: Painter,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Column(
        modifier = Modifier
            .width(42.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = contentColor,
        )
        Text(
            text = label,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            color = contentColor,
        )
    }
}

/**
 * SIM 卡选择卡片
 * 列出所有可用的 SIM 卡供用户选择。
 */
@Composable
fun SimCardSelectionCard(
    selectedSim: SimSelection?,
    allSimList: List<SimSelection>,
    onSelectSim: (SimSelection) -> Unit,
    onRefreshSimList: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(id = R.string.sim_card),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.weight(1F))
                FeatureActionChip(
                    icon = Icons.Rounded.Cached,
                    label = stringResource(R.string.refresh_short),
                    onClick = onRefreshSimList,
                )
            }
            Column {
                allSimList.forEach { sim ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedSim == sim),
                                onClick = { onSelectSim(sim) }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedSim == sim),
                            onClick = { onSelectSim(sim) })
                        Text(sim.showTitle)
                    }
                }
            }
        }
    }
}

/**
 * 此时功能配置卡片
 * 动态加载并显示所支持的 IMS 功能开关。
 */
@Composable
fun FeaturesCard(
    isSelectAllSim: Boolean,
    allSimList: List<SimSelection>,
    selectedSim: SimSelection?,
    imsRegistrationStatusBySubId: Map<Int, Boolean?>,
    imsRegistrationLoadingBySubId: Map<Int, Boolean>,
    featureSwitchesEnabled: Boolean = true,
    onImsRegistrationToggle: (Int, Boolean) -> Unit,
    featureSwitches: Map<Feature, FeatureValue>,
    onFeatureSwitchChange: (Feature, FeatureValue) -> Unit,
    loadCurrentConfig: () -> Unit,
    resetFeatures: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (!isSelectAllSim) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = loadCurrentConfig,
                        enabled = featureSwitchesEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_sync_short), fontSize = 15.sp)
                    }
                    Button(
                        onClick = resetFeatures,
                        enabled = featureSwitchesEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.reset_config), fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.reset_config_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val showFeatures = Feature.entries.toMutableList()
            if (isSelectAllSim) {
                showFeatures.removeAll { it.valueType == FeatureValueType.STRING }
            }
            val orderedFeatures = showFeatures.sortedWith(
                compareBy<Feature>(
                    { if (it.valueType == FeatureValueType.BOOLEAN) 0 else 1 },
                    { switchFeatureCategoryOrder(it) },
                    { Feature.entries.indexOf(it) },
                )
            )
            Text(
                text = stringResource(R.string.all_sim_switch_only_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (!isSelectAllSim) {
                val selectedSubId = selectedSim?.subId
                val checked = selectedSubId != null && imsRegistrationStatusBySubId[selectedSubId] == true
                val loading = selectedSubId != null && imsRegistrationLoadingBySubId[selectedSubId] == true
                BooleanFeatureItem(
                    title = stringResource(R.string.ims_registration_status),
                    description = stringResource(R.string.ims_registration_status_desc),
                    checked = checked,
                    enabled = !loading && featureSwitchesEnabled,
                    onCheckedChange = { targetChecked ->
                        if (selectedSubId == null || selectedSubId < 0) return@BooleanFeatureItem
                        onImsRegistrationToggle(selectedSubId, targetChecked)
                    },
                )
                if (orderedFeatures.isNotEmpty()) {
                    HorizontalDivider(thickness = 0.5.dp)
                }
            } else {
                val realSims = allSimList.filter { it.subId >= 0 }
                realSims.forEachIndexed { index, sim ->
                    val checked = imsRegistrationStatusBySubId[sim.subId] == true
                    val loading = imsRegistrationLoadingBySubId[sim.subId] == true
                    BooleanFeatureItem(
                        title = "${stringResource(R.string.ims_registration_status)} · ${sim.showTitle}",
                        description = stringResource(R.string.ims_registration_status_desc),
                        checked = checked,
                        enabled = !loading && featureSwitchesEnabled,
                        onCheckedChange = { targetChecked ->
                            onImsRegistrationToggle(sim.subId, targetChecked)
                        },
                    )
                    val hasMoreRows = index < realSims.lastIndex
                    if (hasMoreRows || orderedFeatures.isNotEmpty()) {
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
            orderedFeatures.forEachIndexed { index, feature ->
                val title = stringResource(feature.showTitleRes)
                val description = stringResource(feature.showDescriptionRes)
                when (feature.valueType) {
                    FeatureValueType.STRING -> {
                        val inputValue = (featureSwitches[feature]?.data ?: "") as String
                        if (feature == Feature.COUNTRY_ISO) {
                            CountryIsoFeatureItem(
                                title = title,
                                description = description,
                                initInput = inputValue,
                                selectedSim = selectedSim,
                                onInputChange = {
                                    onFeatureSwitchChange(
                                        feature,
                                        FeatureValue(it, feature.valueType)
                                    )
                                },
                            )
                        } else if (feature == Feature.CARRIER_NAME) {
                            val currentCarrierName = selectedSim?.carrierName?.trim().orEmpty()
                            val displayCarrierName = if (inputValue.isBlank()) currentCarrierName else inputValue
                            StringFeatureItem(
                                title = title,
                                description = description,
                                initInput = displayCarrierName,
                                onInputChange = {
                                    onFeatureSwitchChange(
                                        feature,
                                        FeatureValue(it, feature.valueType)
                                    )
                                },
                            )
                        } else {
                            StringFeatureItem(
                                title = title,
                                description = description,
                                initInput = inputValue,
                                onInputChange = {
                                    onFeatureSwitchChange(
                                        feature,
                                        FeatureValue(it, feature.valueType)
                                    )
                                },
                            )
                        }
                    }

                    FeatureValueType.BOOLEAN -> {
                        BooleanFeatureItem(
                            title = title,
                            description = description,
                            checked = (featureSwitches[feature]?.data ?: feature.defaultValue) as Boolean,
                            enabled = featureSwitchesEnabled,
                            onCheckedChange = {
                                onFeatureSwitchChange(
                                    feature,
                                    FeatureValue(it, feature.valueType)
                                )
                            }
                        )
                    }
                }
                if (index < orderedFeatures.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun FeatureActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                label,
                fontSize = 12.sp,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

private fun buildUpdateApkFileName(version: String): String {
    val sanitized = version.replace(Regex("[^0-9A-Za-z._-]"), "_")
    return "TurboIMS-$sanitized.apk"
}

private fun parseVersionParts(version: String): List<Int> {
    val normalized = version.trim().removePrefix("v").removePrefix("V")
    val numeric = Regex("\\d+(?:\\.\\d+)*").find(normalized)?.value ?: return emptyList()
    return numeric.split('.').map { it.toIntOrNull() ?: 0 }
}

private fun isVersionNewer(latest: String, current: String): Boolean {
    val latestParts = parseVersionParts(latest)
    val currentParts = parseVersionParts(current)
    if (latestParts.isEmpty() || currentParts.isEmpty()) {
        return latest != current
    }
    val maxSize = maxOf(latestParts.size, currentParts.size)
    for (index in 0 until maxSize) {
        val l = latestParts.getOrElse(index) { 0 }
        val c = currentParts.getOrElse(index) { 0 }
        if (l > c) return true
        if (l < c) return false
    }
    return false
}

private fun normalizeCountryIso(value: String): String {
    return value.trim().lowercase(Locale.US)
}

private fun sanitizeCountryIsoInput(value: String): String {
    return value.lowercase(Locale.US).filter { it in 'a'..'z' }.take(2)
}

@Composable
private fun countryIsoOptionText(option: CountryIsoOption): String {
    if (option.isoCode.isNullOrBlank()) {
        return stringResource(option.labelRes)
    }
    val country = stringResource(option.labelRes)
    val mcc = option.mcc.orEmpty()
    val iso = option.isoCode
    return if (mcc.isNotBlank()) {
        stringResource(R.string.country_iso_option_format_mcc_iso, country, mcc, iso)
    } else {
        stringResource(R.string.country_iso_option_format_iso, country, iso)
    }
}

private fun findCountryIsoOption(iso: String): CountryIsoOption? {
    val normalized = normalizeCountryIso(iso)
    if (normalized.isBlank()) return null
    return countryIsoOptions.firstOrNull { it.isoCode == normalized }
}

private fun localeCountryNameFromIso(iso: String): String {
    val normalized = sanitizeCountryIsoInput(iso)
    if (normalized.isBlank()) return ""
    val countryLocale = Locale.Builder().setRegion(normalized.uppercase(Locale.US)).build()
    val countryName = countryLocale.getDisplayCountry(Locale.getDefault())
    return if (countryName.isNotBlank()) countryName else normalized.uppercase(Locale.US)
}

@Composable
private fun countryIsoDisplayText(iso: String, mcc: String?): String {
    val normalized = normalizeCountryIso(iso)
    if (normalized.isBlank()) return stringResource(R.string.country_iso_unknown)
    val matchedOption = findCountryIsoOption(normalized)
    if (matchedOption != null) {
        return countryIsoOptionText(matchedOption)
    }
    val countryName = localeCountryNameFromIso(normalized)
    val normalizedMcc = mcc?.trim().orEmpty()
    return if (normalizedMcc.isNotBlank()) {
        stringResource(R.string.country_iso_option_format_mcc_iso, countryName, normalizedMcc, normalized)
    } else {
        stringResource(R.string.country_iso_option_format_iso, countryName, normalized)
    }
}

@Composable
private fun countryIsoMenuItemText(option: CountryIsoOption, customInput: String): String {
    if (option.key != COUNTRY_ISO_OPTION_OTHER) {
        return countryIsoOptionText(option)
    }
    if (customInput.isBlank()) {
        return stringResource(option.labelRes)
    }
    return "${stringResource(option.labelRes)} (${countryIsoDisplayText(customInput, null)})"
}

private fun saveDonationQrToGallery(
    context: Context,
    @DrawableRes imageRes: Int,
    filePrefix: String,
): SavedDonationQr? {
    val bitmap = BitmapFactory.decodeResource(context.resources, imageRes) ?: return null
    val resolver = context.contentResolver
    val displayName = "${filePrefix}_${System.currentTimeMillis()}.png"
    val relativePath = "${Environment.DIRECTORY_DCIM}/TurboIMS"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: run {
        bitmap.recycle()
        return null
    }
    return runCatching {
        resolver.openOutputStream(imageUri)?.use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        } ?: error("openOutputStream failed")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publishValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(imageUri, publishValues, null, null)
        }
        val displayPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "$relativePath/$displayName"
        } else {
            "/sdcard/$relativePath/$displayName"
        }
        SavedDonationQr(uri = imageUri, path = displayPath)
    }.getOrElse {
        resolver.delete(imageUri, null, null)
        null
    }.also {
        bitmap.recycle()
    }
}

private fun openAlipayDonationPage(context: Context, donationQrUri: Uri): Boolean {
    val qrClipData = ClipData.newRawUri("turboims_alipay_qr", donationQrUri)
    val intents = listOf(
        Intent(
            Intent.ACTION_VIEW,
            "alipays://platformapi/startapp?saId=10000007&qrcode=${Uri.encode(ALIPAY_DONATION_URL)}".toUri()
        ).setPackage(ALIPAY_PACKAGE_NAME),
        Intent(Intent.ACTION_VIEW, ALIPAY_DONATION_URL.toUri()).setPackage(ALIPAY_PACKAGE_NAME),
        Intent(Intent.ACTION_VIEW, ALIPAY_DONATION_URL.toUri()),
        Intent(Intent.ACTION_VIEW)
            .setClassName(ALIPAY_PACKAGE_NAME, ALIPAY_SCAN_QR_ACTIVITY)
            .setDataAndType(donationQrUri, DONATION_QR_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = qrClipData },
        Intent(Intent.ACTION_SEND)
            .setClassName(ALIPAY_PACKAGE_NAME, ALIPAY_SCAN_QR_ACTIVITY)
            .setType(DONATION_QR_MIME_TYPE)
            .putExtra(Intent.EXTRA_STREAM, donationQrUri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = qrClipData },
    )
    return startDonationIntentCandidates(context, intents)
}

private fun openWeChatDonationPage(context: Context): Boolean {
    val launchIntent = context.packageManager
        .getLaunchIntentForPackage(WECHAT_PACKAGE_NAME)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (launchIntent != null) {
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrElse { false }
    }
    return false
}

private fun startDonationIntentCandidates(context: Context, intents: List<Intent>): Boolean {
    intents.forEach { intent ->
        val launchIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolved = runCatching {
            context.packageManager.resolveActivity(launchIntent, 0)
        }.getOrNull()
        if (resolved == null) {
            return@forEach
        }
        val started = runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrElse { false }
        if (started) {
            return true
        }
    }
    return false
}

@Composable
fun CountryIsoFeatureItem(
    title: String,
    description: String,
    initInput: String,
    selectedSim: SimSelection?,
    onInputChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionKey by remember { mutableStateOf(COUNTRY_ISO_OPTION_DEFAULT) }
    var customInput by remember { mutableStateOf("") }

    LaunchedEffect(initInput) {
        val normalized = normalizeCountryIso(initInput)
        val matchedOption = countryIsoOptions.firstOrNull { it.isoCode == normalized }
        when {
            normalized.isBlank() -> {
                selectedOptionKey = COUNTRY_ISO_OPTION_DEFAULT
                customInput = ""
            }

            matchedOption != null -> {
                selectedOptionKey = matchedOption.key
                customInput = ""
            }

            else -> {
                selectedOptionKey = COUNTRY_ISO_OPTION_OTHER
                customInput = sanitizeCountryIsoInput(normalized)
            }
        }
    }

    val overrideIso = normalizeCountryIso(initInput)
    val simIso = normalizeCountryIso(selectedSim?.countryIso.orEmpty())
    val currentSettingText = when {
        overrideIso.isNotBlank() -> countryIsoDisplayText(overrideIso, null)
        simIso.isNotBlank() -> countryIsoDisplayText(simIso, selectedSim?.mcc)
        else -> stringResource(R.string.country_iso_unknown)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    value = currentSettingText,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(
                            title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    placeholder = {
                        Text(description)
                    },
                    singleLine = true,
                    maxLines = 1,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    supportingText = {
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    countryIsoOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(countryIsoMenuItemText(option, customInput)) },
                            onClick = {
                                expanded = false
                                selectedOptionKey = option.key
                                when (option.key) {
                                    COUNTRY_ISO_OPTION_DEFAULT -> {
                                        customInput = ""
                                        onInputChange("")
                                    }

                                    COUNTRY_ISO_OPTION_OTHER -> {
                                        customInput = ""
                                        onInputChange("")
                                    }

                                    else -> {
                                        customInput = ""
                                        onInputChange(option.isoCode.orEmpty())
                                    }
                                }
                            },
                        )
                    }
                }
            }
            if (selectedOptionKey == COUNTRY_ISO_OPTION_OTHER) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = customInput,
                    onValueChange = {
                        val sanitized = sanitizeCountryIsoInput(it)
                        customInput = sanitized
                        onInputChange(sanitized)
                    },
                    label = {
                        Text(
                            stringResource(R.string.country_iso_custom_label),
                            fontSize = 14.sp
                        )
                    },
                    placeholder = {
                        Text(stringResource(R.string.country_iso_custom_placeholder))
                    },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onInputChange(customInput)
                        }
                    ),
                )
            }
        }
    }
}

@Composable
fun StringFeatureItem(
    title: String,
    description: String,
    initInput: String,
    onInputChange: (String) -> Unit
) {
    var input by remember { mutableStateOf(initInput) }
    LaunchedEffect(initInput) {
        input = initInput
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .weight(1F),
            value = input,
            onValueChange = {
                input = it
                onInputChange(it)
            },
            label = {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            supportingText = {
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            },
            singleLine = true,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onInputChange(input)
                }
            ),
        )
    }
}

@Composable
fun BooleanFeatureItem(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun MainActionButtons(
    onApplyConfiguration: () -> Unit,
    onDumpConfig: () -> Unit,
    applyingConfiguration: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onApplyConfiguration,
            enabled = !applyingConfiguration,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
        ) {
            Text(
                text = stringResource(
                    id = if (applyingConfiguration) {
                        R.string.apply_config_running
                    } else {
                        R.string.apply_config
                    }
                )
            )
        }
        Button(
            onClick = onDumpConfig,
            enabled = !applyingConfiguration,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text(text = stringResource(id = R.string.dump_config))
        }
    }
}

@Composable
private fun IssueReportHintCard(
    onSubmitIssue: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.issue_failure_hint_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_failure_hint_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Button(
                onClick = onSubmitIssue,
                modifier = Modifier.height(40.dp),
            ) {
                Text(text = stringResource(R.string.issue_failure_submit))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonationBottomSheet(
    onDismissRequest: () -> Unit,
    onOpenWeChat: () -> Unit,
    onOpenAlipay: () -> Unit,
    onSaveImage: (Int, String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.donation_sheet_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.donation_sheet_desc),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))
            DonationQrCard(
                label = stringResource(R.string.donation_wechat),
                imageRes = R.drawable.donate_wechat,
                onPayClick = onOpenWeChat,
                onSaveImage = {
                    onSaveImage(R.drawable.donate_wechat, "turboims_wechat")
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            DonationQrCard(
                label = stringResource(R.string.donation_alipay),
                imageRes = R.drawable.donate_alipay,
                onPayClick = onOpenAlipay,
                onSaveImage = {
                    onSaveImage(R.drawable.donate_alipay, "turboims_alipay")
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DonationQrCard(
    label: String,
    imageRes: Int,
    onPayClick: () -> Unit,
    onSaveImage: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = painterResource(imageRes),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onSaveImage)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    onClick = onPayClick,
                ) {
                    Text(text = stringResource(R.string.donation_pay_now))
                }
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    onClick = onSaveImage,
                ) {
                    Text(text = stringResource(R.string.donation_save_button))
                }
            }
            Text(
                text = stringResource(R.string.donation_save_tip),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun ShizukuUpdateDialog(dismissDialog: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismissDialog,
        title = { Text("Shizuku") },
        text = { Text(stringResource(id = R.string.update_shizuku)) },
        confirmButton = {
            TextButton(onClick = dismissDialog) {
                Text(stringResource(id = android.R.string.ok))
            }
        }
    )
}
