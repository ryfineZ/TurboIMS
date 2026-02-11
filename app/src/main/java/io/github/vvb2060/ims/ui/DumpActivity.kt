package io.github.vvb2060.ims.ui

import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vvb2060.ims.R
import io.github.vvb2060.ims.viewmodel.DumpViewModel

class DumpActivity : BaseActivity() {
    private val viewModel: DumpViewModel by viewModels()

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val subId = intent.getIntExtra(EXTRA_SUB_ID, -1)
        var filterText by remember { mutableStateOf("") }

        LaunchedEffect(subId) {
            viewModel.loadDump(subId)
        }

        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(id = R.string.dump_title)) },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (state.text.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(state.text))
                                    Toast.makeText(context, R.string.dump_copy_success, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = stringResource(id = R.string.dump_copy))
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                when {
                    state.loading -> {
                        Text(
                            text = stringResource(id = R.string.dump_loading),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }

                    state.error != null -> {
                        Text(
                            text = stringResource(R.string.dump_error, state.error ?: ""),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    else -> {
                        val filteredText = if (filterText.isBlank()) {
                            state.text
                        } else {
                            state.text
                                .lineSequence()
                                .filter { it.contains(filterText, ignoreCase = true) }
                                .joinToString("\n")
                        }
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(
                                value = filterText,
                                onValueChange = { filterText = it },
                                label = { Text(stringResource(id = R.string.dump_filter_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = filteredText,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SUB_ID = "extra_sub_id"
    }
}
