package com.azuredevid.cassy.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.R
import org.koin.androidx.compose.koinViewModel
import com.azuredevid.cassy.ui.settings.components.MidtransCredentialsTab
import com.azuredevid.cassy.ui.settings.components.StoreBrandCmsTab
import com.azuredevid.cassy.ui.settings.components.StoreServiceModeTab
import com.azuredevid.cassy.ui.theme.LightAccent
import com.azuredevid.cassy.ui.theme.LightBorder
import com.azuredevid.cassy.ui.theme.LightCanvas
import com.azuredevid.cassy.ui.theme.LightInk
import com.azuredevid.cassy.ui.theme.LightMuted
import com.azuredevid.cassy.ui.theme.LightSecondaryInk
import com.azuredevid.cassy.ui.theme.LightSurface
import com.azuredevid.cassy.ui.theme.StatusErrFgLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSettingsScreen(
    viewModel: StoreSettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm = viewModel

    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                is StoreSettingsUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        withDismissAction = true
                    )
                }
            }
        }
    }

    // Top-level Toast/Snackbar for error/success message updates
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.onEvent(StoreSettingsUiEvent.DismissMessage)
        }
    }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.onEvent(StoreSettingsUiEvent.DismissMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LightCanvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pengaturan Tata Kelola Gerai & WPA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LightInk
                            )
                        )
                        Text(
                            text = "Midtrans BYOK, Tampilan Tamu WPA, dan Kebijakan Operasional Meja",
                            style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_navigate_back),
                            tint = LightInk
                        )
                    }
                },
                actions = {
                    if (!state.isSupervisorAuthenticated) {
                        OutlinedButton(
                            onClick = { vm.onEvent(StoreSettingsUiEvent.RequestSupervisorAuth) },
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .defaultMinSize(minHeight = 40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LightInk)
                        ) {
                            Text(
                                text = "Buka Kunci Supervisor",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(LightMuted, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Supervisor Terotentikasi",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = LightSecondaryInk
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            val tabs = listOf(
                "Kredensial Midtrans",
                "Tampilan Tamu WPA",
                "Mode Layanan & Meja (US4)"
            )

            PrimaryTabRow(
                selectedTabIndex = state.selectedTabIndex,
                containerColor = LightSurface,
                contentColor = LightInk,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(state.selectedTabIndex),
                        color = LightAccent
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state.selectedTabIndex == index,
                        onClick = { vm.onEvent(StoreSettingsUiEvent.SelectTab(index)) },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (state.selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.selectedTabIndex == index) LightInk else LightSecondaryInk
                                )
                            )
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }

            // Tab Body Content
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = LightAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Memuat data konfigurasi...",
                            style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                        )
                    }
                }
            } else {
                when (state.selectedTabIndex) {
                    0 -> MidtransCredentialsTab(
                        state = state,
                        onEvent = vm::onEvent,
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> StoreBrandCmsTab(
                        state = state,
                        onEvent = vm::onEvent,
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> StoreServiceModeTab(
                        state = state,
                        onEvent = vm::onEvent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Supervisor PIN Modal Dialog
    if (state.showSupervisorPinModal) {
        AlertDialog(
            onDismissRequest = { vm.onEvent(StoreSettingsUiEvent.DismissSupervisorPinModal) },
            title = {
                Text(
                    text = "Verifikasi Otorisasi Supervisor",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightInk
                    )
                )
            },
            text = {
                Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Akses dan modifikasi kredensial pembayaran Midtrans serta konfigurasi gerai memerlukan PIN supervisor.",
                        style = MaterialTheme.typography.bodySmall.copy(color = LightSecondaryInk)
                    )

                    OutlinedTextField(
                        value = state.supervisorPinInput,
                        onValueChange = { vm.onEvent(StoreSettingsUiEvent.UpdateSupervisorPinInput(it)) },
                        label = { Text("PIN Supervisor") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        keyboardActions = KeyboardActions(onDone = {
                            vm.onEvent(StoreSettingsUiEvent.SubmitSupervisorPin)
                        }),
                        singleLine = true,
                        isError = state.supervisorPinError != null,
                        supportingText = {
                            if (state.supervisorPinError != null) {
                                Text(
                                    text = state.supervisorPinError ?: "",
                                    color = StatusErrFgLight,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightAccent,
                            unfocusedBorderColor = LightBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.onEvent(StoreSettingsUiEvent.SubmitSupervisorPin) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightInk,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Verifikasi")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { vm.onEvent(StoreSettingsUiEvent.DismissSupervisorPinModal) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Batal")
                }
            },
            containerColor = LightSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

