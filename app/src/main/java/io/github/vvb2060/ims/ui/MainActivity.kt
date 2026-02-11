package io.github.vvb2060.ims.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
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

private data class ReleaseInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

private data class UpdateDialogState(
    val currentVersion: String,
    val latest: ReleaseInfo,
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

        val scope = rememberCoroutineScope()
        var selectedSim by remember { mutableStateOf<SimSelection?>(null) }
        var showShizukuUpdateDialog by remember { mutableStateOf(false) }
        var pendingAutoSelectSimAfterReady by remember { mutableStateOf(false) }
        var imsRegistrationStatus by remember { mutableStateOf<Boolean?>(null) }
        var imsRegistrationLoading by remember { mutableStateOf(false) }
        var checkingUpdate by remember { mutableStateOf(false) }
        var updateDialogState by remember { mutableStateOf<UpdateDialogState?>(null) }
        val featureSwitches = remember { mutableStateMapOf<Feature, FeatureValue>() }

        LaunchedEffect(shizukuStatus) {
            if (shizukuStatus == ShizukuStatus.NEED_UPDATE) {
                showShizukuUpdateDialog = true
            }
            pendingAutoSelectSimAfterReady = shizukuStatus == ShizukuStatus.READY
        }
        LaunchedEffect(allSimList) {
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
        LaunchedEffect(selectedSim, shizukuStatus) {
            if (selectedSim != null) {
                featureSwitches.clear()
                val currentConfig = if (shizukuStatus == ShizukuStatus.READY && selectedSim!!.subId >= 0) {
                    viewModel.loadCurrentConfiguration(selectedSim!!.subId)
                } else {
                    null
                }
                if (currentConfig != null) {
                    featureSwitches.putAll(currentConfig)
                } else {
                    val savedConfig = viewModel.loadConfiguration(selectedSim!!.subId)
                    if (savedConfig != null) {
                        featureSwitches.putAll(savedConfig)
                    } else {
                        featureSwitches.putAll(viewModel.loadDefaultPreferences())
                    }
                }

                imsRegistrationStatus =
                    if (shizukuStatus == ShizukuStatus.READY && selectedSim!!.subId >= 0) {
                        viewModel.readImsRegistrationStatus(selectedSim!!.subId)
                    } else {
                        null
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
                        viewModel.loadSimList()
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
                )
                if (shizukuStatus == ShizukuStatus.READY) {
                    SimCardSelectionCard(selectedSim, allSimList, onSelectSim = {
                        selectedSim = it
                    }, onRefreshSimList = {
                        viewModel.loadSimList()
                        Toast.makeText(context, R.string.sim_list_refresh, Toast.LENGTH_SHORT).show()
                    })
                    FeaturesCard(
                        isSelectAllSim = selectedSim?.subId == -1,
                        selectedSim = selectedSim,
                        imsRegistrationStatus = imsRegistrationStatus,
                        imsRegistrationLoading = imsRegistrationLoading,
                        onImsRegistrationToggle = { targetChecked ->
                            if (!targetChecked) return@FeaturesCard
                            val sim = selectedSim
                            if (sim == null || sim.subId < 0) {
                                Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                return@FeaturesCard
                            }
                            if (shizukuStatus != ShizukuStatus.READY) {
                                Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                                return@FeaturesCard
                            }
                            scope.launch {
                                imsRegistrationLoading = true
                                val registered = viewModel.registerIms(sim.subId)
                                imsRegistrationStatus = registered
                                imsRegistrationLoading = false
                            }
                        },
                        featureSwitches,
                        onFeatureSwitchChange = { feature, value ->
                            featureSwitches[feature] = value
                        },
                        loadCurrentConfig = {
                            scope.launch {
                                if (selectedSim == null) return@launch
                                if (shizukuStatus != ShizukuStatus.READY) {
                                    Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                if (selectedSim!!.subId < 0) {
                                    Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                featureSwitches.clear()
                                val currentConfig = viewModel.loadCurrentConfiguration(selectedSim!!.subId)
                                if (currentConfig != null) {
                                    featureSwitches.putAll(currentConfig)
                                    Toast.makeText(context, R.string.load_config_current_success, Toast.LENGTH_SHORT).show()
                                } else {
                                    featureSwitches.putAll(viewModel.loadDefaultPreferences())
                                    Toast.makeText(context, R.string.load_config_default_success, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        resetFeatures = {
                            featureSwitches.clear()
                            featureSwitches.putAll(viewModel.loadDefaultPreferences())
                            Toast.makeText(context, R.string.load_config_default_success, Toast.LENGTH_SHORT).show()
                        }
                    )
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
                            viewModel.onApplyConfiguration(sim, featureSwitches)
                        },
                        onResetConfiguration = {
                            val sim = selectedSim
                            if (sim == null) {
                                Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                                return@MainActionButtons
                            }
                            if (shizukuStatus != ShizukuStatus.READY) {
                                Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                                return@MainActionButtons
                            }
                            scope.launch {
                                val success = viewModel.onResetConfiguration(sim)
                                if (!success) return@launch

                                featureSwitches.clear()
                                if (sim.subId < 0) {
                                    featureSwitches.putAll(viewModel.loadDefaultPreferences())
                                } else {
                                    val currentConfig = viewModel.loadCurrentConfiguration(sim.subId)
                                    if (currentConfig != null) {
                                        featureSwitches.putAll(currentConfig)
                                    } else {
                                        featureSwitches.putAll(viewModel.loadDefaultPreferences())
                                    }
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
                        }
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
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(ClipboardManager::class.java)
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
                        onClick = {
                            val issueBody = buildString {
                                appendLine("App Version: ${systemInfo.appVersionName}")
                                appendLine("Device Model: ${systemInfo.deviceModel}")
                                appendLine("Android Version: ${systemInfo.androidVersion}")
                                appendLine("Patch Date: ${systemInfo.securityPatchVersion}")
                                append("Shizuku Status: $shizukuStatusText")
                            }
                            clipboardManager?.setPrimaryClip(
                                ClipData.newPlainText(
                                    "turboims_system_info",
                                    issueBody
                                )
                            )
                            Toast.makeText(context, R.string.issue_info_copied, Toast.LENGTH_SHORT).show()
                            uriHandler.openUri(REPO_ISSUE_URL)
                        },
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
    selectedSim: SimSelection?,
    imsRegistrationStatus: Boolean?,
    imsRegistrationLoading: Boolean,
    onImsRegistrationToggle: (Boolean) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!isSelectAllSim) {
                    Button(
                        onClick = loadCurrentConfig,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_sync_short), fontSize = 15.sp)
                    }
                }
                Button(
                    onClick = resetFeatures,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Rounded.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_default_short), fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            val showFeatures = Feature.entries.toMutableList()
            if (isSelectAllSim) {
                showFeatures.remove(Feature.CARRIER_NAME)
                showFeatures.remove(Feature.COUNTRY_ISO)
            }
            val orderedFeatures = showFeatures.sortedWith(
                compareBy<Feature>(
                    { if (it.valueType == FeatureValueType.BOOLEAN) 0 else 1 },
                    { switchFeatureCategoryOrder(it) },
                    { Feature.entries.indexOf(it) },
                )
            )
            if (!isSelectAllSim) {
                BooleanFeatureItem(
                    title = stringResource(R.string.ims_registration_status),
                    description = stringResource(R.string.ims_registration_status_desc),
                    checked = imsRegistrationStatus == true,
                    enabled = !imsRegistrationLoading,
                    onCheckedChange = onImsRegistrationToggle,
                )
                if (orderedFeatures.isNotEmpty()) {
                    HorizontalDivider(thickness = 0.5.dp)
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
    onResetConfiguration: () -> Unit,
    onDumpConfig: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onApplyConfiguration,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
        ) {
            Text(text = stringResource(id = R.string.apply_config))
        }
        Button(
            onClick = onResetConfiguration,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(text = stringResource(id = R.string.reset_config))
        }
        Button(
            onClick = onDumpConfig,
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
