package io.github.vvb2060.ims.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import io.github.vvb2060.ims.BuildConfig
import io.github.vvb2060.ims.Feature
import io.github.vvb2060.ims.FeatureValueType
import io.github.vvb2060.ims.MainViewModel
import io.github.vvb2060.ims.R
import io.github.vvb2060.ims.ShizukuStatus
import io.github.vvb2060.ims.SimSelection
import io.github.vvb2060.ims.ui.theme.TurbolImsTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TurbolImsTheme {
                val context = LocalContext.current
                val uriHandler = LocalUriHandler.current

                val readPhoneStatePermission =
                    rememberPermissionState(Manifest.permission.READ_PHONE_STATE)
                val scrollBehavior =
                    TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

                val androidVersion by viewModel.androidVersion.collectAsStateWithLifecycle()
                val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
                val allSimList by viewModel.allSimList.collectAsStateWithLifecycle()
                val featureSwitches by viewModel.featureSwitches.collectAsStateWithLifecycle()

                var selectedSim by remember { mutableStateOf<SimSelection?>(null) }
                var showShizukuUpdateDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (readPhoneStatePermission.status != PermissionStatus.Granted) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.phone_no_permission_msg),
                            Toast.LENGTH_SHORT
                        ).show()
                        readPhoneStatePermission.launchPermissionRequest()
                    }
                }
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
                            actions = {
                                IconButton(onClick = {
                                    uriHandler.openUri("https://github.com/Mystery00/TurboIMS")
                                }) {
                                    Icon(painterResource(R.drawable.ic_github), null)
                                }
                            },
                        )
                    }) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SystemInfoCard(
                            androidVersion,
                            shizukuStatus,
                            onRefresh = {
                                viewModel.updateShizukuStatus()
                                viewModel.loadSimList()
                            },
                            onRequestShizukuPermission = {
                                viewModel.requestShizukuPermission(0)
                            },
                        )
                        SimCardSelectionCard(selectedSim, allSimList, onSelectSim = {
                            selectedSim = it
                        }, onRefreshSimList = {
                            viewModel.loadSimList()
                        })
                        FeaturesCard(featureSwitches, viewModel::onFeatureSwitchChange)
                        ApplyButton(selectedSim != null) {
                            if (shizukuStatus != ShizukuStatus.READY) {
                                Toast.makeText(
                                    context,
                                    R.string.shizuku_not_running_msg,
                                    Toast.LENGTH_LONG
                                ).show()
                                return@ApplyButton
                            }
                            viewModel.onApplyConfiguration(context, selectedSim!!)
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
                            viewModel.onResetConfiguration(context)
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
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateShizukuStatus()
    }
}

@Composable
fun SystemInfoCard(
    androidVersion: String,
    shizukuStatus: ShizukuStatus,
    onRefresh: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(id = R.string.system_info),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.android_version, androidVersion),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.security_patch_version, Build.VERSION.SECURITY_PATCH),
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
                    modifier = Modifier
                        .weight(1F)
                        .padding(bottom = 16.dp)
                )
                IconButton(onClick = onRefreshSimList) {
                    Icon(painterResource(R.drawable.ic_refresh), null)
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

@Composable
fun FeaturesCard(
    featureSwitches: Map<Feature, Any>,
    onFeatureSwitchChange: (Feature, Any) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(id = R.string.features_config),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Feature.entries.forEachIndexed { index, feature ->
                val title = stringResource(feature.showTitleRes)
                val description = stringResource(feature.showDescriptionRes)
                when (feature.valueType) {
                    FeatureValueType.STRING -> {
                        StringFeatureItem(
                            title = title,
                            description = description,
                            initInput = (featureSwitches[feature] ?: "") as String,
                            onInputChange = { onFeatureSwitchChange(feature, it) },
                        )
                    }

                    FeatureValueType.BOOLEAN -> {
                        BooleanFeatureItem(
                            title = title,
                            description = description,
                            checked = (featureSwitches[feature] ?: true) as Boolean,
                            onCheckedChange = { onFeatureSwitchChange(feature, it) }
                        )
                    }
                }
                if (index < Feature.entries.lastIndex) {
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
        IconButton(
            onClick = {
                onInputChange(input)
            }
        ) {
            Icon(painterResource(R.drawable.ic_done), null)
        }
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