package com.azuredevid.cassy.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.ui.scanner.CameraQrScannerView
import com.azuredevid.cassy.ui.theme.CassyPandan
import com.azuredevid.cassy.ui.theme.CassyPutihTulang
import com.azuredevid.cassy.ui.theme.CassyTinta
import com.azuredevid.cassy.ui.theme.LightBorder
import com.azuredevid.cassy.ui.theme.LightSecondaryInk
import com.azuredevid.cassy.ui.theme.LightSurface
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import org.koin.androidx.compose.koinViewModel

@Composable
fun StorePairingScreen(
    viewModel: StorePairingViewModel = koinViewModel(),
    onNavigateToServerConfig: () -> Unit = {},
    onPairingSuccess: (storeSlug: String, storeName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val logoDesc = stringResource(R.string.a11y_cassy_logo)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is StorePairingUiEffect.PairingSucceeded -> {
                    onPairingSuccess(effect.storeSlug, effect.storeName)
                }
            }
        }
    }

    if (state.isCameraActive) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraQrScannerView(
                onQrDetected = { payload ->
                    viewModel.onEvent(StorePairingUiEvent.HandleQrPayload(payload))
                }
            )
            Button(
                onClick = { viewModel.onEvent(StorePairingUiEvent.ToggleCamera(false)) },
                colors = ButtonDefaults.buttonColors(containerColor = CassyPandan, contentColor = Color.White),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.btn_close_camera))
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CassyPutihTulang)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LightSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(CassyPandan, RoundedCornerShape(12.dp))
                            .semantics { contentDescription = logoDesc },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    IconButton(
                        onClick = onNavigateToServerConfig,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.auth_btn_server_config),
                            tint = CassyTinta
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.pairing_screen_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CassyTinta
                    )
                )

                Text(
                    text = stringResource(R.string.pairing_screen_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(color = LightSecondaryInk)
                )

                OutlinedButton(
                    onClick = { viewModel.onEvent(StorePairingUiEvent.ToggleCamera(true)) },
                    enabled = !state.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.btn_scan_qr_camera))
                }

                if (state.qrScannedInfo != null) {
                    Text(
                        text = state.qrScannedInfo ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CassyPandan,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = LightBorder)
                    Text(
                        text = stringResource(R.string.or_divider),
                        style = MaterialTheme.typography.labelSmall.copy(color = LightSecondaryInk),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = LightBorder)
                }

                OutlinedTextField(
                    value = state.storeSlug,
                    onValueChange = { viewModel.onEvent(StorePairingUiEvent.UpdateStoreSlug(it)) },
                    label = { Text(stringResource(R.string.store_slug_label)) },
                    singleLine = true,
                    isError = state.storeSlugError != null,
                    supportingText = state.storeSlugError?.let { err -> { Text(err, color = StatusErrFgLight) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onEvent(StorePairingUiEvent.UpdateEmail(it)) },
                    label = { Text(stringResource(R.string.auth_email_label)) },
                    singleLine = true,
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { err -> { Text(err, color = StatusErrFgLight) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.pin,
                    onValueChange = { viewModel.onEvent(StorePairingUiEvent.UpdatePin(it)) },
                    label = { Text(stringResource(R.string.auth_pin_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.pinError != null,
                    supportingText = state.pinError?.let { err -> { Text(err, color = StatusErrFgLight) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.onEvent(StorePairingUiEvent.SubmitLogin) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.formError != null) {
                    Text(
                        text = state.formError ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = StatusErrFgLight)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { viewModel.onEvent(StorePairingUiEvent.SubmitLogin) },
                    enabled = !state.isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CassyPandan,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.auth_btn_sign_in),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
