package io.github.vvb2060.ims.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vvb2060.ims.R
import io.github.vvb2060.ims.model.Feature
import io.github.vvb2060.ims.model.FeatureValue
import io.github.vvb2060.ims.model.FeatureValueType
import io.github.vvb2060.ims.model.ShizukuStatus
import io.github.vvb2060.ims.model.SimSelection
import io.github.vvb2060.ims.model.SystemInfo
import io.github.vvb2060.ims.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Composable
    override fun content() {
        val context = LocalContext.current

        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        val systemInfo by viewModel.systemInfo.collectAsStateWithLifecycle()
        val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
        val allSimList by viewModel.allSimList.collectAsStateWithLifecycle()

        val scope = rememberCoroutineScope()
        var selectedSim by remember { mutableStateOf<SimSelection?>(null) }
        var showShizukuUpdateDialog by remember { mutableStateOf(false) }
        val featureSwitches = remember { mutableStateMapOf<Feature, FeatureValue>() }

        LaunchedEffect(shizukuStatus) {
            if (shizukuStatus == ShizukuStatus.NEED_UPDATE) {
                showShizukuUpdateDialog = true
            }
        }
        LaunchedEffect(allSimList) {
            if (selectedSim == null) {
                selectedSim = allSimList.firstOrNull()
            }
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
                )
                SimCardSelectionCard(selectedSim, allSimList, onSelectSim = {
                    selectedSim = it
                }, onRefreshSimList = {
                    viewModel.loadSimList()
                    Toast.makeText(context, R.string.sim_list_refresh, Toast.LENGTH_SHORT).show()
                })
                FeaturesCard(
                    isSelectAllSim = selectedSim?.subId == -1,
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
                    loadFeatureHistory = {
                        featureSwitches.clear()
                        val savedConfig = selectedSim?.let { viewModel.loadConfiguration(it.subId) }
                        if (savedConfig != null) {
                            featureSwitches.putAll(savedConfig)
                            Toast.makeText(context, R.string.load_config_history_success, Toast.LENGTH_SHORT).show()
                        } else {
                            featureSwitches.putAll(viewModel.loadDefaultPreferences())
                            Toast.makeText(context, R.string.load_config_default_success, Toast.LENGTH_SHORT).show()
                        }
                    },
                    resetFeatures = {
                        featureSwitches.clear()
                        featureSwitches.putAll(viewModel.loadDefaultPreferences())
                        Toast.makeText(context, R.string.load_config_default_success, Toast.LENGTH_SHORT).show()
                    }
                )
                ToolsCard(
                    onRestartIms = {
                        if (selectedSim == null || selectedSim!!.subId == -1) {
                            Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                            return@ToolsCard
                        }
                        if (shizukuStatus != ShizukuStatus.READY) {
                            Toast.makeText(context, R.string.shizuku_not_running_msg, Toast.LENGTH_LONG).show()
                            return@ToolsCard
                        }
                        viewModel.restartImsRegistration(selectedSim!!)
                    },
                    onDumpConfig = {
                        if (selectedSim == null || selectedSim!!.subId == -1) {
                            Toast.makeText(context, R.string.select_single_sim, Toast.LENGTH_SHORT).show()
                            return@ToolsCard
                        }
                        startActivity(
                            Intent(
                                this@MainActivity,
                                DumpActivity::class.java
                            ).putExtra(DumpActivity.EXTRA_SUB_ID, selectedSim!!.subId)
                        )
                    }
                )
                ApplyButton(selectedSim != null) {
                    if (shizukuStatus != ShizukuStatus.READY) {
                        Toast.makeText(
                            context,
                            R.string.shizuku_not_running_msg,
                            Toast.LENGTH_LONG
                        ).show()
                        return@ApplyButton
                    }
                    viewModel.onApplyConfiguration(selectedSim!!, featureSwitches)
                }
                ResetButton {
                    if (shizukuStatus != ShizukuStatus.READY) {
                        Toast.makeText(
                            context,
                            R.string.shizuku_not_running_msg,
                            Toast.LENGTH_LONG
                        ).show()
                        return@ResetButton
                    }
                    viewModel.onResetConfiguration(selectedSim!!)
                }
                Tips()
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))

                if (showShizukuUpdateDialog) {
                    ShizukuUpdateDialog {
                        showShizukuUpdateDialog = false
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateShizukuStatus()
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
    onLogcatClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

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
                IconButton(onClick = {
                    uriHandler.openUri("https://github.com/Mystery00/TurboIMS")
                }) {
                    Icon(painterResource(R.drawable.ic_github), null)
                }
                IconButton(onClick = onLogcatClick) {
                    Icon(imageVector = Icons.Default.BugReport, null)
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
                stringResource(R.string.system_build_version, systemInfo.systemVersion),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.security_patch_version, systemInfo.securityPatchVersion),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
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

            Text(
                stringResource(R.string.shizuku_status, shizukuStatusText),
                fontSize = 14.sp,
                color = shizukuStatusColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onRefresh) {
                    Text(text = stringResource(id = R.string.refresh_permission))
                }
                if (shizukuStatus == ShizukuStatus.NO_PERMISSION) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onRequestShizukuPermission) {
                        Text(text = stringResource(id = R.string.request_permission))
                    }
                }
            }

        }
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
                IconButton(onClick = onRefreshSimList) {
                    Icon(
                        imageVector = Icons.Rounded.Cached,
                        contentDescription = "Refresh"
                    )
                }
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
    featureSwitches: Map<Feature, FeatureValue>,
    onFeatureSwitchChange: (Feature, FeatureValue) -> Unit,
    loadCurrentConfig: () -> Unit,
    loadFeatureHistory: () -> Unit,
    resetFeatures: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(id = R.string.features_config),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.weight(1F))
                if (!isSelectAllSim) {
                    IconButton(onClick = loadCurrentConfig) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "Load Current"
                        )
                    }
                    IconButton(onClick = loadFeatureHistory) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "Load History"
                        )
                    }
                }
                IconButton(onClick = resetFeatures) {
                    Icon(
                        imageVector = Icons.Rounded.SettingsBackupRestore,
                        contentDescription = "Reload"
                    )
                }
            }

            val showFeatures = Feature.entries.toMutableList()
            if (isSelectAllSim) {
                showFeatures.remove(Feature.CARRIER_NAME)
                showFeatures.remove(Feature.COUNTRY_ISO)
            }
            showFeatures.forEachIndexed { index, feature ->
                val title = stringResource(feature.showTitleRes)
                val description = stringResource(feature.showDescriptionRes)
                when (feature.valueType) {
                    FeatureValueType.STRING -> {
                        StringFeatureItem(
                            title = title,
                            description = description,
                            initInput = (featureSwitches[feature]?.data ?: "") as String,
                            onInputChange = {
                                onFeatureSwitchChange(
                                    feature,
                                    FeatureValue(it, feature.valueType)
                                )
                            },
                        )
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
                if (index < showFeatures.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp)
                }
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
            placeholder = {
                Text(description)
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ToolsCard(
    onRestartIms: () -> Unit,
    onDumpConfig: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(id = R.string.tools),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onRestartIms,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.restart_ims))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onDumpConfig,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(text = stringResource(id = R.string.dump_config))
                }
            }
        }
    }
}

@Composable
fun ResetButton(
    onResetConfiguration: () -> Unit,
) {
    Button(
        onClick = onResetConfiguration,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
    ) {
        Text(
            stringResource(R.string.reset_config),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun ApplyButton(
    isApplyButtonEnabled: Boolean,
    onApplyConfiguration: () -> Unit,
) {
    Button(
        onClick = onApplyConfiguration,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        enabled = isApplyButtonEnabled,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(
            stringResource(R.string.apply_config),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun Tips() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            stringResource(id = R.string.tip),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
        )
        val lines = stringArrayResource(id = R.array.tips)
        for (text in lines) {
            val t = text.removePrefix("!")
            Text(
                t,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = if (text.startsWith("!")) FontWeight.Bold else null
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
