package com.azuredevid.cassy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.ui.theme.CassyPandan
import com.azuredevid.cassy.ui.theme.CassyPutihTulang
import com.azuredevid.cassy.ui.theme.CassyTinta
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    viewModel: ServerConfigViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CassyPutihTulang,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.server_config_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_navigate_back),
                            tint = CassyTinta
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(24.dp)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.urlInput == "https://demo.abbdullaziz.online",
                    onClick = { viewModel.applyPreset("https://demo.abbdullaziz.online") },
                    label = { Text("Demo HTTPS") }
                )
                FilterChip(
                    selected = state.urlInput == "http://10.0.2.2:8080",
                    onClick = { viewModel.applyPreset("http://10.0.2.2:8080") },
                    label = { Text("Emulator Local") }
                )
            }

            OutlinedTextField(
                value = state.urlInput,
                onValueChange = viewModel::onUrlChanged,
                label = { Text(stringResource(R.string.server_config_url_label)) },
                singleLine = true,
                isError = state.urlError != null,
                supportingText = state.urlError?.let { err -> { Text(err, color = StatusErrFgLight) } },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (viewModel.save()) {
                        onNavigateBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CassyPandan, contentColor = Color.White),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.server_config_save))
            }
        }
    }
}
