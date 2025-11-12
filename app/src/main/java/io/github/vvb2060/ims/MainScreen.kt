package io.github.vvb2060.ims

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(
    uiState: MainUiState,
    onFeatureSwitchChange: (Feature, Boolean) -> Unit,
    onApplyConfiguration: () -> Unit,
    onSelectSim: (SimSelection) -> Unit,
    openSimSelectionDialog: () -> Unit,
    dismissSimSelectionDialog: () -> Unit,
    dismissConfigAppliedDialog: () -> Unit,
    dismissShizukuUpdateDialog: () -> Unit,
    onRequestShizukuPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {
        Header()
        SystemInfoCard(uiState, onRequestShizukuPermission)
        SimCardSelectionCard(uiState, openSimSelectionDialog)
        FeaturesCard(uiState, onFeatureSwitchChange)
        ApplyButton(uiState, onApplyConfiguration)
        Tips()

        if (uiState.showSimSelectionDialog) {
            SimSelectionDialog(uiState, onSelectSim, dismissSimSelectionDialog)
        }

        if (uiState.showConfigAppliedDialog) {
            ConfigAppliedDialog(dismissConfigAppliedDialog)
        }

        if (uiState.showShizukuUpdateDialog) {
            ShizukuUpdateDialog(dismissShizukuUpdateDialog)
        }
    }
}

@Composable
fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A73E8))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("⚡ Turbo IMS", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                stringResource(id = R.string.for_pixel),
                fontSize = 14.sp,
                color = Color(0xFFE0E0E0),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SystemInfoCard(uiState: MainUiState, onRequestShizukuPermission: () -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(id = R.string.system_info),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A73E8)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.android_version, uiState.androidVersion),
                fontSize = 14.sp,
                color = Color(0xFF424242)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val shizukuStatusText = when (uiState.shizukuStatus) {
                ShizukuStatus.CHECKING -> stringResource(R.string.shizuku_checking)
                ShizukuStatus.NOT_RUNNING -> stringResource(R.string.shizuku_not_running)
                ShizukuStatus.NO_PERMISSION -> stringResource(R.string.shizuku_no_permission)
                ShizukuStatus.READY -> stringResource(R.string.shizuku_ready)
            }
            val shizukuStatusColor = when (uiState.shizukuStatus) {
                ShizukuStatus.NOT_RUNNING -> Color.Red
                ShizukuStatus.NO_PERMISSION -> Color(0xFFFF9800)
                else -> Color(0xFF4CAF50)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                 Text(
                    stringResource(R.string.shizuku_status, shizukuStatusText),
                    fontSize = 14.sp,
                    color = shizukuStatusColor
                )
                if (uiState.shizukuStatus == ShizukuStatus.NO_PERMISSION) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onRequestShizukuPermission) {
                        Text(text = stringResource(id = R.string.request_permission))
                    } 
                }
            }
           

            if (uiState.isQpr2Beta3OrHigher) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(id = R.string.qpr2_beta3_warning),
                    fontSize = 13.sp,
                    color = Color(0xFFF57C00),
                    modifier = Modifier
                        .background(Color(0xFFFFF3E0))
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun SimCardSelectionCard(uiState: MainUiState, openSimSelectionDialog: () -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(id = R.string.sim_card),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A73E8)
            )
            Spacer(modifier = Modifier.height(12.dp))
            val simInfoText = when (uiState.selectedSim) {
                SimSelection.SIM1 -> stringResource(id = R.string.sim_1)
                SimSelection.SIM2 -> stringResource(id = R.string.sim_2)
                SimSelection.ALL -> stringResource(id = R.string.apply_to_all_sims)
            }
            Text(simInfoText, fontSize = 14.sp, color = Color(0xFF757575))
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = openSimSelectionDialog,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1A73E8))
            ) {
                Text(stringResource(id = R.string.select_sim), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun FeaturesCard(uiState: MainUiState, onFeatureSwitchChange: (Feature, Boolean) -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(id = R.string.features_config),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A73E8)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Feature.entries.forEachIndexed { index, feature ->
                val featureName = when(feature) {
                    Feature.VOLTE -> R.string.volte
                    Feature.VOWIFI -> R.string.vowifi
                    Feature.VT -> R.string.vt
                    Feature.VONR -> R.string.vonr
                    Feature.CROSS_SIM -> R.string.cross_sim
                    Feature.UT -> R.string.ut
                    Feature.FIVE_G_NR -> R.string._5g_nr
                }
                val featureDesc = when(feature) {
                    Feature.VOLTE -> R.string.volte_desc
                    Feature.VOWIFI -> R.string.vowifi_desc
                    Feature.VT -> R.string.vt_desc
                    Feature.VONR -> R.string.vonr_desc
                    Feature.CROSS_SIM -> R.string.cross_sim_desc
                    Feature.UT -> R.string.ut_desc
                    Feature.FIVE_G_NR -> R.string._5g_nr_desc
                }
                FeatureItem(
                    title = stringResource(id = featureName),
                    description = stringResource(id = featureDesc),
                    checked = uiState.featureSwitches[feature] ?: true,
                    onCheckedChange = { onFeatureSwitchChange(feature, it) }
                )
                if (index < Feature.entries.size - 1) {
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun FeatureItem(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 13.sp, color = Color(0xFF757575))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ApplyButton(uiState: MainUiState, onApplyConfiguration: () -> Unit) {
    val buttonText = when (uiState.selectedSim) {
        SimSelection.SIM1 -> stringResource(id = R.string.apply_to_sim_1)
        SimSelection.SIM2 -> stringResource(id = R.string.apply_to_sim_2)
        SimSelection.ALL -> stringResource(id = R.string.apply_to_all)
    }
    Button(
        onClick = onApplyConfiguration,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        enabled = uiState.isApplyButtonEnabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
    ) {
        Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun Tips() {
    Text(
        stringResource(id = R.string.tips),
        fontSize = 12.sp,
        color = Color(0xFF757575),
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun SimSelectionDialog(
    uiState: MainUiState,
    onSelectSim: (SimSelection) -> Unit,
    dismissDialog: () -> Unit
) {
    AlertDialog(
        onDismissRequest = dismissDialog,
        title = { Text(stringResource(id = R.string.select_sim)) },
        text = {
            Column {
                val simOptions = listOf(
                    stringResource(id = R.string.sim_1) to SimSelection.SIM1,
                    stringResource(id = R.string.sim_2) to SimSelection.SIM2,
                    stringResource(id = R.string.apply_to_all_sims) to SimSelection.ALL
                )
                simOptions.forEach { (text, sim) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = (uiState.selectedSim == sim), onClick = { onSelectSim(sim) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (uiState.selectedSim == sim), onClick = { onSelectSim(sim) })
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = dismissDialog) {
                Text(stringResource(id = android.R.string.ok))
            }
        }
    )
}

@Composable
fun ConfigAppliedDialog(dismissDialog: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismissDialog,
        title = { Text(stringResource(id = R.string.config_applied)) },
        text = { Text(stringResource(id = R.string.config_success_message)) },
        confirmButton = {
            TextButton(onClick = dismissDialog) {
                Text(stringResource(id = R.string.later))
            }
        }
    )
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
