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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
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
import io.github.vvb2060.ims.privileged.ImsModifier
import io.github.vvb2060.ims.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
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
private val VERSION_DISPLAY_WITH_REV_REGEX = Regex("""\d+\.\d+\.\d+\.[rd]\d+""")
private val VERSION_DISPLAY_REGEX = Regex("""\d+\.\d+\.\d+""")

private data class ReleaseInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

private data class UpdateDialogState(
    val currentVersion: String,
    val latest: ReleaseInfo,
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

private enum class CaptivePortalAction {
    FIX,
    RESTORE,
    NONE,
}

private val countryIsoOptions = listOf(
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

private fun isChinaDomesticSim(sim: SimSelection?): Boolean {
    if (sim == null || sim.subId < 0) return false
    val iccId = sim.iccId.trim()
    if (iccId.startsWith("8986")) return true
    val mcc = sim.mcc.filter { it.isDigit() }.take(3)
    return mcc == "460"
}

private fun toDisplayVersion(rawVersion: String?): String {
    val text = rawVersion?.trim().orEmpty()
    if (text.isBlank()) return ""
    val normalized = text.removePrefix("v")
    return VERSION_DISPLAY_WITH_REV_REGEX.find(normalized)?.value
        ?: VERSION_DISPLAY_REGEX.find(normalized)?.value
        ?: normalized
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

private fun dumpValueText(value: Any?): String {
    return when (value) {
        null -> "null"
        is IntArray -> value.joinToString(prefix = "[", postfix = "]")
        is LongArray -> value.joinToString(prefix = "[", postfix = "]")
        is DoubleArray -> value.joinToString(prefix = "[", postfix = "]")
        is BooleanArray -> value.joinToString(prefix = "[", postfix = "]")
        is Array<*> -> value.joinToString(prefix = "[", postfix = "]")
        else -> value.toString()
    }
}

private fun buildEditableConfigSnapshotText(
    selectedSim: SimSelection,
    featureMap: Map<Feature, FeatureValue>,
    countryMccInput: String,
    resolvedCountryIsoForApply: String?,
    bundleForApply: Bundle,
    captivePortalState: MainViewModel.CaptivePortalFixState?,
): String {
    val sortedBundleKeys = bundleForApply.keySet().sorted()
    return buildString {
        appendLine("[selected_sim]")
        appendLine("sim.show_title=${selectedSim.showTitle}")
        appendLine("sim.sub_id=${selectedSim.subId}")
        appendLine("sim.slot_index=${selectedSim.simSlotIndex}")
        appendLine("sim.current_mcc=${selectedSim.mcc}")
        appendLine("sim.current_mnc=${selectedSim.mnc}")
        appendLine("sim.current_iso=${selectedSim.countryIso}")
        appendLine()

        appendLine("[feature_inputs]")
        Feature.entries.forEach { feature ->
            val value = featureMap[feature]?.data ?: feature.defaultValue
            appendLine("feature.${feature.name.lowercase(Locale.US)}=${dumpValueText(value)}")
        }
        appendLine("input.country_mcc=$countryMccInput")
        appendLine("apply.country_iso_resolved=${resolvedCountryIsoForApply ?: ""}")
        appendLine()

        appendLine("[carrier_config_bundle_for_apply]")
        if (sortedBundleKeys.isEmpty()) {
            appendLine("(empty)")
        } else {
            sortedBundleKeys.forEach { key ->
                appendLine("$key=${dumpValueText(bundleForApply.get(key))}")
            }
        }
        appendLine()

        appendLine("[network_verification]")
        if (captivePortalState == null) {
            appendLine("captive_portal.mode=unknown")
            appendLine("captive_portal.http_url=")
            appendLine("captive_portal.https_url=")
        } else {
            appendLine("captive_portal.mode=${captivePortalState.mode}")
            appendLine("captive_portal.http_url=${captivePortalState.httpUrl}")
            appendLine("captive_portal.https_url=${captivePortalState.httpsUrl}")
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
        var hasUpdateAvailable by remember { mutableStateOf(false) }
        var latestAvailableVersion by remember { mutableStateOf<String?>(null) }
        var fixingCaptivePortal by remember { mutableStateOf(false) }
        var checkingCaptivePortalStatus by remember { mutableStateOf(false) }
        var captivePortalFixState by remember { mutableStateOf<MainViewModel.CaptivePortalFixState?>(null) }
        var updateDialogState by remember { mutableStateOf<UpdateDialogState?>(null) }
        var showDonationSheet by remember { mutableStateOf(false) }
        var donationFeedbackMessage by remember { mutableStateOf<String?>(null) }
        var showDiagnosticsDialog by remember { mutableStateOf(false) }
        var diagnosticsRunning by remember { mutableStateOf(false) }
        var diagnosticsJob by remember { mutableStateOf<Job?>(null) }
        val diagnosticsLines = remember { mutableStateListOf<String>() }
        val featureSwitches = remember { mutableStateMapOf<Feature, FeatureValue>() }
        val committedFeatureSwitches = remember { mutableStateMapOf<Feature, FeatureValue>() }
        val countryMccDraftBySubId = remember { mutableStateMapOf<Int, String>() }
        val committedCountryMccBySubId = remember { mutableStateMapOf<Int, String>() }
        val countryIsoApplySignalBySubId = remember { mutableStateMapOf<Int, Int>() }
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
            if (shizukuStatus == ShizukuStatus.READY) {
                checkingCaptivePortalStatus = true
                captivePortalFixState = viewModel.queryCaptivePortalFixState()
                checkingCaptivePortalStatus = false
            } else {
                checkingCaptivePortalStatus = false
                captivePortalFixState = null
            }
        }
        LaunchedEffect(Unit) {
            if (checkingUpdate) return@LaunchedEffect
            checkingUpdate = true
            val currentVersion = BuildConfig.VERSION_NAME
            val result = fetchLatestReleaseInfo()
            checkingUpdate = false
            val release = result.getOrNull()
            hasUpdateAvailable = release != null && isVersionNewer(release.version, currentVersion)
            latestAvailableVersion = if (hasUpdateAvailable) release?.version else null
        }
        LaunchedEffect(allSimList) {
            val validSubIds = allSimList.filter { it.subId >= 0 }.map { it.subId }.toSet()
            imsRegistrationStatusMap.keys.toList()
                .filterNot { validSubIds.contains(it) }
                .forEach { imsRegistrationStatusMap.remove(it) }
            imsRegistrationLoadingMap.keys.toList()
                .filterNot { validSubIds.contains(it) }
                .forEach { imsRegistrationLoadingMap.remove(it) }
            countryMccDraftBySubId.keys.toList()
                .filterNot { validSubIds.contains(it) }
                .forEach { countryMccDraftBySubId.remove(it) }
            committedCountryMccBySubId.keys.toList()
                .filterNot { validSubIds.contains(it) }
                .forEach { committedCountryMccBySubId.remove(it) }
            countryIsoApplySignalBySubId.keys.toList()
                .filterNot { validSubIds.contains(it) }
                .forEach { countryIsoApplySignalBySubId.remove(it) }
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
            val currentSelected = selectedSim ?: return@LaunchedEffect
            committedFeatureSwitches.clear()
            val currentConfig = if (shizukuStatus == ShizukuStatus.READY && currentSelected.subId >= 0) {
                viewModel.loadCurrentConfiguration(currentSelected.subId)
            } else {
                null
            }
            if (currentConfig != null) {
                committedFeatureSwitches.putAll(currentConfig)
            } else {
                val savedConfig = viewModel.loadConfiguration(currentSelected.subId)
                if (savedConfig != null) {
                    committedFeatureSwitches.putAll(savedConfig)
                } else {
                    committedFeatureSwitches.putAll(viewModel.loadDefaultPreferences())
                }
            }
            syncFeatureState(featureSwitches, committedFeatureSwitches)
            if (currentSelected.subId >= 0) {
                val savedMcc = viewModel.loadSavedCountryMccOverride(currentSelected.subId)
                countryMccDraftBySubId[currentSelected.subId] = savedMcc
                committedCountryMccBySubId[currentSelected.subId] = savedMcc
            }
            if (currentSelected.subId >= 0) {
                imsRegistrationStatusMap[currentSelected.subId] =
                    if (shizukuStatus == ShizukuStatus.READY) {
                        viewModel.readImsRegistrationStatus(currentSelected.subId)
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

        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            contentWindowInsets = WindowInsets(0.dp),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .statusBarsPadding()
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
                    hasUpdateAvailable = hasUpdateAvailable,
                    latestAvailableVersion = latestAvailableVersion,
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
                                hasUpdateAvailable = false
                                latestAvailableVersion = null
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
                                hasUpdateAvailable = false
                                latestAvailableVersion = null
                                Toast.makeText(context, R.string.update_latest, Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            hasUpdateAvailable = true
                            latestAvailableVersion = release.version
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
                CaptivePortalFixCard(
                    shizukuStatus = shizukuStatus,
                    checkingCaptivePortalStatus = checkingCaptivePortalStatus,
                    fixingCaptivePortal = fixingCaptivePortal,
                    state = captivePortalFixState,
                    onFixCaptivePortal = {
                        if (fixingCaptivePortal) return@CaptivePortalFixCard
                        if (shizukuStatus != ShizukuStatus.READY) {
                            Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                            return@CaptivePortalFixCard
                        }
                        scope.launch {
                            val action = when (captivePortalFixState?.mode) {
                                MainViewModel.CaptivePortalFixMode.CAN_RESTORE -> CaptivePortalAction.RESTORE
                                MainViewModel.CaptivePortalFixMode.NORMAL -> CaptivePortalAction.NONE
                                else -> CaptivePortalAction.FIX
                            }
                            if (action == CaptivePortalAction.NONE) return@launch
                            fixingCaptivePortal = true
                            val resultMsg = when (action) {
                                CaptivePortalAction.FIX -> viewModel.applyCaptivePortalCnUrls()
                                CaptivePortalAction.RESTORE -> viewModel.restoreCaptivePortalDefaultUrls()
                                CaptivePortalAction.NONE -> null
                            }
                            fixingCaptivePortal = false
                            if (resultMsg == null) {
                                Toast.makeText(
                                    context,
                                    if (action == CaptivePortalAction.RESTORE) {
                                        R.string.captive_portal_restore_success
                                    } else {
                                        R.string.captive_portal_fix_success
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        if (action == CaptivePortalAction.RESTORE) {
                                            R.string.captive_portal_restore_failed
                                        } else {
                                            R.string.captive_portal_fix_failed
                                        },
                                        resultMsg
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            checkingCaptivePortalStatus = true
                            captivePortalFixState = viewModel.queryCaptivePortalFixState()
                            checkingCaptivePortalStatus = false
                        }
                    }
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
                        countryIsoApplySignal = selectedSim?.subId
                            ?.takeIf { it >= 0 }
                            ?.let { countryIsoApplySignalBySubId[it] ?: 0 }
                            ?: 0,
                        countryMccDraft = selectedSim?.subId
                            ?.takeIf { it >= 0 }
                            ?.let { countryMccDraftBySubId[it].orEmpty() }
                            .orEmpty(),
                        onCountryMccDraftChange = { newMcc ->
                            selectedSim?.subId
                                ?.takeIf { it >= 0 }
                                ?.let { countryMccDraftBySubId[it] = newMcc }
                        },
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
                        onTextFeatureCommit = { _ ->
                            scope.launch {
                                if (applyingConfiguration) return@launch
                                val sim = selectedSim
                                if (sim == null || sim.subId < 0) {
                                    Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                if (shizukuStatus != ShizukuStatus.READY) {
                                    Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                val mapToApply = buildCompleteFeatureMap(featureSwitches)
                                if (mapToApply == buildCompleteFeatureMap(committedFeatureSwitches)) {
                                    return@launch
                                }
                                applyingConfiguration = true
                                try {
                                    val resultMsg = viewModel.onApplyConfiguration(
                                        sim,
                                        mapToApply
                                    )
                                    if (resultMsg == null) {
                                        syncFeatureState(committedFeatureSwitches, mapToApply)
                                        countryIsoApplySignalBySubId[sim.subId] =
                                            (countryIsoApplySignalBySubId[sim.subId] ?: 0) + 1
                                    } else {
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
                                        countryMccDraftBySubId[sim.subId] = viewModel
                                            .loadSavedCountryMccOverride(sim.subId)
                                        committedCountryMccBySubId[sim.subId] =
                                            countryMccDraftBySubId[sim.subId].orEmpty()
                                        countryIsoApplySignalBySubId[sim.subId] =
                                            (countryIsoApplySignalBySubId[sim.subId] ?: 0) + 1
                                        imsRegistrationStatusMap[sim.subId] =
                                            viewModel.readImsRegistrationStatus(sim.subId)
                                    } else {
                                        syncFeatureState(committedFeatureSwitches, viewModel.loadDefaultPreferences())
                                        syncFeatureState(featureSwitches, committedFeatureSwitches)
                                        countryMccDraftBySubId[sim.subId] = ""
                                        committedCountryMccBySubId[sim.subId] = ""
                                        countryIsoApplySignalBySubId[sim.subId] =
                                            (countryIsoApplySignalBySubId[sim.subId] ?: 0) + 1
                                    }
                                } finally {
                                    applyingConfiguration = false
                                }
                            }
                        },
                        onDumpConfig = {
                            val sim = selectedSim
                            if (sim == null || sim.subId < 0) {
                                Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                return@FeaturesCard
                            }
                            scope.launch {
                                val mapToDump = buildCompleteFeatureMap(featureSwitches)
                                val resolvedCountryIso = viewModel.resolveCountryIsoOverridePreview(sim, mapToDump)
                                val bundleForApply = ImsModifier.buildBundle(
                                    carrierName = null,
                                    countryISO = resolvedCountryIso,
                                    countryMcc = null,
                                    countryMncHint = sim.mnc,
                                    enableVoLTE = (mapToDump[Feature.VOLTE]?.data ?: true) as Boolean,
                                    enableVoWiFi = (mapToDump[Feature.VOWIFI]?.data ?: true) as Boolean,
                                    enableVT = (mapToDump[Feature.VT]?.data ?: true) as Boolean,
                                    enableVoNR = (mapToDump[Feature.VONR]?.data ?: true) as Boolean,
                                    enableCrossSIM = (mapToDump[Feature.CROSS_SIM]?.data ?: true) as Boolean,
                                    enableUT = (mapToDump[Feature.UT]?.data ?: true) as Boolean,
                                    enable5GNR = (mapToDump[Feature.FIVE_G_NR]?.data ?: true) as Boolean,
                                    enable5GThreshold = (mapToDump[Feature.FIVE_G_THRESHOLDS]?.data ?: true) as Boolean,
                                    enable5GPlusIcon = (mapToDump[Feature.FIVE_G_PLUS_ICON]?.data ?: true) as Boolean,
                                    enableShow4GForLTE = (mapToDump[Feature.SHOW_4G_FOR_LTE]?.data ?: false) as Boolean,
                                )
                                val snapshotText = buildEditableConfigSnapshotText(
                                    selectedSim = sim,
                                    featureMap = mapToDump,
                                    countryMccInput = countryMccDraftBySubId[sim.subId].orEmpty(),
                                    resolvedCountryIsoForApply = resolvedCountryIso,
                                    bundleForApply = bundleForApply,
                                    captivePortalState = captivePortalFixState ?: viewModel.queryCaptivePortalFixState(),
                                )
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        DumpActivity::class.java
                                    )
                                        .putExtra(DumpActivity.EXTRA_SUB_ID, sim.subId)
                                        .putExtra(DumpActivity.EXTRA_PRESET_TEXT, snapshotText)
                                )
                            }
                        },
                        onRunDiagnostics = {
                            val sim = selectedSim
                            if (sim == null || sim.subId < 0) {
                                Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                return@FeaturesCard
                            }
                            if (shizukuStatus != ShizukuStatus.READY) {
                                Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                                return@FeaturesCard
                            }
                            diagnosticsJob?.cancel()
                            diagnosticsLines.clear()
                            showDiagnosticsDialog = true
                            diagnosticsRunning = true
                            diagnosticsJob = scope.launch {
                                try {
                                    val appMapSnapshot = buildCompleteFeatureMap(featureSwitches)
                                    viewModel.runShizukuDiagnostics(
                                        selectedSim = sim,
                                        visibleSimList = allSimList,
                                        appFeatureMap = appMapSnapshot
                                    ).collect { line ->
                                        diagnosticsLines.add(line)
                                    }
                                } catch (t: Throwable) {
                                    diagnosticsLines.add("❌ 诊断异常：${t.javaClass.simpleName} (${t.message ?: "unknown"})")
                                    Toast.makeText(
                                        context,
                                        R.string.diagnostics_failed,
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    diagnosticsRunning = false
                                }
                            }
                        },
                    )
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
                if (showDiagnosticsDialog) {
                    AlertDialog(
                        modifier = Modifier.fillMaxWidth(0.96f),
                        properties = DialogProperties(usePlatformDefaultWidth = false),
                        onDismissRequest = {
                            diagnosticsJob?.cancel()
                            diagnosticsRunning = false
                            showDiagnosticsDialog = false
                        },
                        title = {
                            Text(stringResource(R.string.diagnostics_menu))
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(
                                        if (diagnosticsRunning) {
                                            R.string.diagnostics_running
                                        } else {
                                            R.string.diagnostics_finished
                                        }
                                    ),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (diagnosticsRunning) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                val logText = diagnosticsLines.joinToString("\n")
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 220.dp, max = 480.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(10.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = logText.ifBlank { stringResource(R.string.diagnostics_empty) },
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                enabled = diagnosticsLines.isNotEmpty(),
                                onClick = {
                                    val content = diagnosticsLines.joinToString("\n")
                                    clipboardManager?.setPrimaryClip(
                                        ClipData.newPlainText("carrier_ims_diagnostics", content)
                                    )
                                    Toast.makeText(context, R.string.dump_copy_success, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(stringResource(R.string.dump_copy))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    diagnosticsJob?.cancel()
                                    diagnosticsRunning = false
                                    showDiagnosticsDialog = false
                                }
                            ) {
                                Text(stringResource(id = android.R.string.cancel))
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
            .setTitle("Carrier IMS ${release.version}")
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
    hasUpdateAvailable: Boolean,
    latestAvailableVersion: String?,
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
        ShizukuStatus.NOT_RUNNING -> MaterialTheme.colorScheme.error
        ShizukuStatus.NO_PERMISSION -> MaterialTheme.colorScheme.tertiary
        else -> Color(0xFF16A34A)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            BrandHeader()
            Spacer(modifier = Modifier.height(12.dp))
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
                        icon = painterResource(
                            if (hasUpdateAvailable) {
                                R.drawable.ic_update_available
                            } else {
                                R.drawable.ic_update
                            }
                        ),
                        label = stringResource(
                            if (hasUpdateAvailable) {
                                R.string.action_update_available
                            } else {
                                R.string.action_update
                            }
                        ),
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
            val versionAnnotated = buildAnnotatedString {
                append(stringResource(R.string.current_version, toDisplayVersion(systemInfo.appVersionName)))
                if (hasUpdateAvailable && !latestAvailableVersion.isNullOrBlank()) {
                    append(" · ")
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(
                            stringResource(
                                R.string.update_available_inline,
                                toDisplayVersion(latestAvailableVersion)
                            )
                        )
                    }
                }
            }
            Text(text = versionAnnotated, fontSize = 14.sp)
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
            Spacer(modifier = Modifier.height(4.dp))
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
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp)
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
private fun BrandHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.brand_name),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.brand_subtitle),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun CaptivePortalFixCard(
    shizukuStatus: ShizukuStatus,
    checkingCaptivePortalStatus: Boolean,
    fixingCaptivePortal: Boolean,
    state: MainViewModel.CaptivePortalFixState?,
    onFixCaptivePortal: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.captive_portal_fix_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.captive_portal_fix_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(10.dp))
            val isReady = shizukuStatus == ShizukuStatus.READY
            val currentMode = state?.mode ?: MainViewModel.CaptivePortalFixMode.NEED_FIX
            val buttonEnabled = isReady &&
                !fixingCaptivePortal &&
                !checkingCaptivePortalStatus &&
                currentMode != MainViewModel.CaptivePortalFixMode.NORMAL
            val statusTextRes = when {
                checkingCaptivePortalStatus -> R.string.captive_portal_fix_status_checking
                currentMode == MainViewModel.CaptivePortalFixMode.CAN_RESTORE -> R.string.captive_portal_fix_status_restorable
                currentMode == MainViewModel.CaptivePortalFixMode.NORMAL -> R.string.captive_portal_fix_status_normal
                else -> R.string.captive_portal_fix_status_need_fix
            }
            val actionTextRes = when {
                fixingCaptivePortal && currentMode == MainViewModel.CaptivePortalFixMode.CAN_RESTORE ->
                    R.string.captive_portal_restore_running
                fixingCaptivePortal -> R.string.captive_portal_fix_running
                checkingCaptivePortalStatus -> R.string.captive_portal_fix_checking
                currentMode == MainViewModel.CaptivePortalFixMode.CAN_RESTORE ->
                    R.string.captive_portal_fix_restore_action
                currentMode == MainViewModel.CaptivePortalFixMode.NORMAL ->
                    R.string.captive_portal_fix_normal_action
                else -> R.string.captive_portal_fix_action
            }
            Button(
                onClick = onFixCaptivePortal,
                enabled = buttonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = stringResource(actionTextRes)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(statusTextRes),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
            if (!isReady) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.captive_portal_fix_requires_shizuku),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
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
        Column(modifier = Modifier.padding(12.dp)) {
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
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Column {
                    allSimList.forEach { sim ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp)
                                .selectable(
                                    selected = (selectedSim == sim),
                                    onClick = { onSelectSim(sim) }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                modifier = Modifier.size(20.dp),
                                selected = (selectedSim == sim),
                                onClick = { onSelectSim(sim) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sim.showTitle)
                        }
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
    countryIsoApplySignal: Int,
    countryMccDraft: String,
    onCountryMccDraftChange: (String) -> Unit,
    onFeatureSwitchChange: (Feature, FeatureValue) -> Unit,
    onTextFeatureCommit: (Feature) -> Unit,
    resetFeatures: () -> Unit,
    onDumpConfig: () -> Unit,
    onRunDiagnostics: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            var featureMenuExpanded by remember(isSelectAllSim, selectedSim?.subId) { mutableStateOf(false) }
            var showRestoreConfirmDialog by remember(isSelectAllSim, selectedSim?.subId) {
                mutableStateOf(false)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.features_config),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!isSelectAllSim) {
                    Box {
                        IconButton(
                            onClick = { featureMenuExpanded = true },
                            enabled = featureSwitchesEnabled,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.tools)
                            )
                        }
                        DropdownMenu(
                            expanded = featureMenuExpanded,
                            onDismissRequest = { featureMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reset_config)) },
                                onClick = {
                                    featureMenuExpanded = false
                                    showRestoreConfirmDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dump_config)) },
                                onClick = {
                                    featureMenuExpanded = false
                                    onDumpConfig()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.diagnostics_menu)) },
                                onClick = {
                                    featureMenuExpanded = false
                                    onRunDiagnostics()
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (showRestoreConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showRestoreConfirmDialog = false },
                    title = { Text(stringResource(R.string.restore_confirm_title)) },
                    text = { Text(stringResource(R.string.restore_confirm_message)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showRestoreConfirmDialog = false
                                resetFeatures()
                            }
                        ) {
                            Text(stringResource(R.string.restore_confirm_action))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestoreConfirmDialog = false }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    }
                )
            }

            val showFeatures = Feature.entries.toMutableList().apply {
                removeAll { it.valueType == FeatureValueType.STRING }
                if (isSelectAllSim || !isChinaDomesticSim(selectedSim)) {
                    remove(Feature.TIKTOK_NETWORK_FIX)
                }
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
                                initMcc = countryMccDraft,
                                currentNetworkIso = selectedSim?.countryIso.orEmpty(),
                                currentNetworkMcc = selectedSim?.mcc.orEmpty(),
                                currentNetworkMnc = selectedSim?.mnc.orEmpty(),
                                selectedSubId = selectedSim?.subId ?: -1,
                                applySuccessSignal = countryIsoApplySignal,
                                onInputChange = { iso, mcc ->
                                    onFeatureSwitchChange(
                                        feature,
                                        FeatureValue(iso, feature.valueType)
                                    )
                                    onCountryMccDraftChange(mcc)
                                },
                                onCommitRequest = { onTextFeatureCommit(feature) },
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
                                onCommitInput = { onTextFeatureCommit(feature) },
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
                                onCommitInput = { onTextFeatureCommit(feature) },
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
    return normalizeCountryIso(value)
        .filter { it.isLetterOrDigit() }
        .take(8)
}

private fun sanitizeMccInput(value: String): String {
    val cleaned = value.trim().filter { it.isDigit() || it == '-' }
    if (cleaned.isEmpty()) return ""
    val firstDash = cleaned.indexOf('-')
    return if (firstDash == -1) {
        cleaned.take(7)
    } else {
        val left = cleaned.substring(0, firstDash).filter { it.isDigit() }.take(3)
        val right = cleaned.substring(firstDash + 1).filter { it.isDigit() }.take(3)
        if (right.isNotEmpty()) "$left-$right" else left
    }
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
    val normalized = sanitizeCountryIsoInput(iso)
    if (normalized.isBlank()) return null
    return countryIsoOptions.firstOrNull { it.isoCode == normalized }
}

private fun findCountryIsoOptionByMcc(mcc: String): CountryIsoOption? {
    val normalized = sanitizeMccInput(mcc)
    if (normalized.isBlank()) return null
    return countryIsoOptions.firstOrNull { option ->
        val optionMcc = option.mcc?.trim().orEmpty()
        if (optionMcc.isBlank()) return@firstOrNull false
        if (optionMcc == normalized) return@firstOrNull true
        if (!optionMcc.contains('-')) {
            return@firstOrNull optionMcc == normalized
        }
        val (start, end) = optionMcc.split('-', limit = 2)
        val inputInt = normalized.toIntOrNull() ?: return@firstOrNull false
        val startInt = start.toIntOrNull() ?: return@firstOrNull false
        val endInt = end.toIntOrNull() ?: return@firstOrNull false
        inputInt in startInt..endInt
    }
}

@Composable
private fun currentCountryOverrideSummary(
    overrideIso: String,
    overrideMcc: String,
    currentNetworkIso: String,
    currentNetworkMcc: String,
): String {
    val iso = normalizeCountryIso(overrideIso)
    val mcc = sanitizeMccInput(overrideMcc)
    if (iso.isBlank() && mcc.isBlank()) {
        val actualIso = normalizeCountryIso(currentNetworkIso)
        val actualMcc = sanitizeMccInput(currentNetworkMcc)
        if (actualIso.isBlank() && actualMcc.isBlank()) {
            return stringResource(R.string.country_iso_not_overridden)
        }
        if (actualIso.isBlank()) {
            return stringResource(R.string.country_iso_current_format_mcc_only, actualMcc)
        }
        val matchedByIso = findCountryIsoOption(actualIso)
        val matchedByMcc = if (actualMcc.isNotBlank()) findCountryIsoOptionByMcc(actualMcc) else null
        val countryName = when {
            matchedByIso != null -> stringResource(matchedByIso.labelRes)
            matchedByMcc != null -> stringResource(matchedByMcc.labelRes)
            else -> actualIso.uppercase(Locale.US)
        }
        return if (actualMcc.isNotBlank()) {
            stringResource(R.string.country_iso_option_format_mcc_iso, countryName, actualMcc, actualIso)
        } else {
            stringResource(R.string.country_iso_option_format_iso, countryName, actualIso)
        }
    }
    if (mcc.isNotBlank() && iso.isBlank()) {
        val matched = findCountryIsoOptionByMcc(mcc)
        if (matched?.isoCode != null) {
            val countryName = stringResource(matched.labelRes)
            return stringResource(R.string.country_iso_option_format_mcc_iso, countryName, mcc, matched.isoCode)
        }
        return stringResource(R.string.country_iso_current_format_mcc_only, mcc)
    }
    val matchedByIso = findCountryIsoOption(iso)
    val countryName = matchedByIso?.let { stringResource(it.labelRes) } ?: iso.uppercase(Locale.US)
    return if (mcc.isNotBlank()) {
        stringResource(R.string.country_iso_option_format_mcc_iso, countryName, mcc, iso)
    } else {
        stringResource(R.string.country_iso_option_format_iso, countryName, iso)
    }
}

@Composable
private fun countryIsoMenuItemText(
    option: CountryIsoOption,
): String {
    return countryIsoOptionText(option)
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
    initMcc: String,
    currentNetworkIso: String,
    currentNetworkMcc: String,
    currentNetworkMnc: String,
    selectedSubId: Int,
    applySuccessSignal: Int,
    onInputChange: (String, String) -> Unit,
    onCommitRequest: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionKey by remember(selectedSubId) { mutableStateOf(COUNTRY_ISO_OPTION_DEFAULT) }
    var customMccInput by remember(selectedSubId) { mutableStateOf("") }
    var customIsoInput by remember(selectedSubId) { mutableStateOf("") }
    val overrideIso = sanitizeCountryIsoInput(initInput)
    val overrideMcc = sanitizeMccInput(initMcc)
    val normalizedMnc = currentNetworkMnc.trim()
    var customMccHadFocus by remember(selectedSubId) { mutableStateOf(false) }
    var customIsoHadFocus by remember(selectedSubId) { mutableStateOf(false) }

    fun commitCustomInputs(
        rawMcc: String = customMccInput,
        rawIso: String = customIsoInput,
        linkIsoByMcc: Boolean = false,
    ) {
        val sanitizedMcc = sanitizeMccInput(rawMcc)
        val sanitizedIso = sanitizeCountryIsoInput(rawIso)
        val linkedIso = if (linkIsoByMcc) {
            if (sanitizedIso.isBlank()) {
                findCountryIsoOptionByMcc(sanitizedMcc)?.isoCode ?: sanitizedIso
            } else {
                sanitizedIso
            }
        } else {
            sanitizedIso
        }
        customMccInput = sanitizedMcc
        customIsoInput = linkedIso
        onInputChange(linkedIso, sanitizedMcc)
        onCommitRequest()
    }

    LaunchedEffect(initInput, initMcc, applySuccessSignal, selectedSubId) {
        val matchedOptionByIso = findCountryIsoOption(overrideIso)
        val matchedOptionByMcc = findCountryIsoOptionByMcc(overrideMcc)
        selectedOptionKey = when {
            overrideIso.isBlank() && overrideMcc.isBlank() -> {
                COUNTRY_ISO_OPTION_DEFAULT
            }

            matchedOptionByMcc != null && (overrideIso.isBlank() || overrideIso == matchedOptionByMcc.isoCode) -> {
                matchedOptionByMcc.key
            }

            matchedOptionByIso != null && (overrideMcc.isBlank() || matchedOptionByMcc?.key == matchedOptionByIso.key) -> {
                matchedOptionByIso.key
            }

            else -> {
                COUNTRY_ISO_OPTION_OTHER
            }
        }
        customMccInput = overrideMcc
        customIsoInput = overrideIso
    }
    val dropdownDisplayText = if (selectedOptionKey == COUNTRY_ISO_OPTION_DEFAULT) {
        stringResource(
            R.string.country_iso_current_value,
            currentCountryOverrideSummary(
                overrideIso = overrideIso,
                overrideMcc = overrideMcc,
                currentNetworkIso = currentNetworkIso,
                currentNetworkMcc = currentNetworkMcc,
            )
        )
    } else {
        countryIsoMenuItemText(
            countryIsoOptions.firstOrNull { it.key == selectedOptionKey } ?: countryIsoOptions.first()
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
                    value = dropdownDisplayText,
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
                        Text(stringResource(R.string.country_iso_quick_pick_placeholder))
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
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    countryIsoOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(countryIsoMenuItemText(option)) },
                            onClick = {
                                expanded = false
                                selectedOptionKey = option.key
                                when (option.key) {
                                    COUNTRY_ISO_OPTION_OTHER -> {
                                        customMccInput = overrideMcc
                                        customIsoInput = overrideIso
                                    }

                                    else -> {
                                        val selectedIso = option.isoCode.orEmpty()
                                        val selectedMcc = option.mcc.orEmpty()
                                        customMccInput = selectedMcc
                                        customIsoInput = selectedIso
                                        onInputChange(selectedIso, selectedMcc)
                                        onCommitRequest()
                                    }
                                }
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            customMccHadFocus = true
                        } else if (customMccHadFocus) {
                            customMccHadFocus = false
                            commitCustomInputs(linkIsoByMcc = true)
                        }
                    },
                value = customMccInput,
                onValueChange = { raw ->
                    selectedOptionKey = COUNTRY_ISO_OPTION_OTHER
                    customMccInput = sanitizeMccInput(raw)
                },
                label = { Text(stringResource(R.string.country_iso_mcc_label), fontSize = 14.sp) },
                placeholder = { Text(stringResource(R.string.country_iso_mcc_placeholder)) },
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitCustomInputs(linkIsoByMcc = true) }),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            customIsoHadFocus = true
                        } else if (customIsoHadFocus) {
                            customIsoHadFocus = false
                            commitCustomInputs()
                        }
                    },
                value = customIsoInput,
                onValueChange = { raw ->
                    selectedOptionKey = COUNTRY_ISO_OPTION_OTHER
                    customIsoInput = sanitizeCountryIsoInput(raw)
                },
                label = { Text(stringResource(R.string.country_iso_iso_label), fontSize = 14.sp) },
                placeholder = { Text(stringResource(R.string.country_iso_custom_placeholder)) },
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitCustomInputs() }),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = normalizedMnc,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.country_iso_mnc_label), fontSize = 14.sp) },
                placeholder = { Text(stringResource(R.string.country_iso_mnc_placeholder)) },
                singleLine = true,
                maxLines = 1,
                supportingText = {
                    Text(
                        text = stringResource(R.string.country_iso_mnc_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
fun StringFeatureItem(
    title: String,
    description: String,
    initInput: String,
    onInputChange: (String) -> Unit,
    onCommitInput: (String) -> Unit,
) {
    var input by remember { mutableStateOf(initInput) }
    var hadFocus by remember { mutableStateOf(false) }
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
                .weight(1F)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        hadFocus = true
                    } else if (hadFocus) {
                        hadFocus = false
                        onCommitInput(input)
                    }
                },
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
                    onCommitInput(input)
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
