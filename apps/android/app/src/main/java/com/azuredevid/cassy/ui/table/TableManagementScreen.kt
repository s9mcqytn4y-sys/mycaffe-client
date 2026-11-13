package com.azuredevid.cassy.ui.table

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.core.hardware.TablePdfPosterGenerator
import com.azuredevid.cassy.core.util.QrBitmapGenerator
import com.azuredevid.cassy.data.model.TableBoardEntry
import com.azuredevid.cassy.data.model.TableInput
import com.azuredevid.cassy.ui.theme.StatusErrBgLight
import com.azuredevid.cassy.ui.theme.StatusErrFgLight
import com.azuredevid.cassy.ui.theme.StatusOkBgLight
import com.azuredevid.cassy.ui.theme.StatusOkFgLight
import com.azuredevid.cassy.ui.theme.StatusWarnBgLight
import com.azuredevid.cassy.ui.theme.StatusWarnFgLight
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableManagementScreen(
    onNavigateBack: () -> Unit,
    cashierPrinterIp: String = "192.168.10.200",
    viewModel: TableManagementViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val rupiahFormat = remember { NumberFormat.getIntegerInstance(Locale("id", "ID")) }

    var showTableDialog by remember { mutableStateOf(false) }
    var editingTable by remember { mutableStateOf<TableBoardEntry?>(null) }
    var posterTable by remember { mutableStateOf<TableBoardEntry?>(null) }
    var rotateConfirmTable by remember { mutableStateOf<TableBoardEntry?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TableManagementUiEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                TableManagementUiEffect.TableSaved -> {
                    showTableDialog = false
                    editingTable = null
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.table_mgmt_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_navigate_back)
                        )
                    }
                },
                actions = {
                    if (state.canManageTables) {
                        Button(
                            onClick = {
                                editingTable = null
                                showTableDialog = true
                            },
                            modifier = Modifier.sizeIn(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.table_add))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = { viewModel.refreshTables() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.table_refresh)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isSubmitting || state.isPrintingThermal) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val selected = state.selectedSection == "all"
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectSection("all") },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else null,
                        label = { Text(stringResource(R.string.table_section_all)) },
                        modifier = Modifier.sizeIn(minHeight = 48.dp)
                    )
                }
                items(state.sections, key = { it }) { section ->
                    val selected = state.selectedSection.equals(section, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectSection(section) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else null,
                        label = { Text(section) },
                        modifier = Modifier.sizeIn(minHeight = 48.dp)
                    )
                }
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.displayedTables.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.table_empty),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.displayedTables, key = { it.id }) { table ->
                            TableBoardCard(
                                table = table,
                                canManage = state.canManageTables,
                                isSubmitting = state.isSubmitting,
                                rupiahFormat = rupiahFormat,
                                onOpenSession = { viewModel.openSession(table.id) },
                                onCloseSession = {
                                    table.session?.id?.let { viewModel.closeSession(it) }
                                },
                                onPoster = { posterTable = table },
                                onRotateQr = { rotateConfirmTable = table },
                                onEdit = {
                                    editingTable = table
                                    showTableDialog = true
                                },
                                onArchive = { viewModel.archiveTable(table.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTableDialog) {
        TableFormDialog(
            initial = editingTable,
            isSubmitting = state.isSubmitting,
            onDismiss = { showTableDialog = false },
            onSave = { input -> viewModel.saveTable(editingTable?.id, input) }
        )
    }

    val currentPosterTable = posterTable
    if (currentPosterTable != null) {
        TablePosterDialog(
            table = currentPosterTable,
            storeName = state.storeName,
            qrUrl = viewModel.buildGuestQrUrl(currentPosterTable.qrToken),
            isPrintingThermal = state.isPrintingThermal,
            onPrintThermal = { viewModel.printThermalPoster(currentPosterTable, cashierPrinterIp) },
            onPrintPdf = {
                TablePdfPosterGenerator.printTablePosterPdf(
                    context = context,
                    storeName = state.storeName,
                    brandHex = null,
                    tableLabel = currentPosterTable.displayTitle,
                    section = currentPosterTable.section,
                    qrPayloadUrl = viewModel.buildGuestQrUrl(currentPosterTable.qrToken)
                )
            },
            onDismiss = { posterTable = null }
        )
    }

    val targetRotate = rotateConfirmTable
    if (targetRotate != null) {
        AlertDialog(
            onDismissRequest = { rotateConfirmTable = null },
            title = { Text(stringResource(R.string.table_rotate_qr_title)) },
            text = { Text(stringResource(R.string.table_rotate_qr_confirm, targetRotate.displayTitle)) },
            confirmButton = {
                Button(
                    enabled = !state.isSubmitting,
                    onClick = {
                        viewModel.rotateQr(targetRotate)
                        rotateConfirmTable = null
                    }
                ) {
                    Text(stringResource(R.string.table_rotate_qr_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { rotateConfirmTable = null }) {
                    Text(stringResource(R.string.catalog_btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun TableBoardCard(
    table: TableBoardEntry,
    canManage: Boolean,
    isSubmitting: Boolean,
    rupiahFormat: NumberFormat,
    onOpenSession: () -> Unit,
    onCloseSession: () -> Unit,
    onPoster: () -> Unit,
    onRotateQr: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    val (statusLabel, bgColor, fgColor) = when (table.status) {
        "occupied" -> Triple(stringResource(R.string.table_status_occupied), StatusWarnBgLight, StatusWarnFgLight)
        "settled" -> Triple(stringResource(R.string.table_status_settled), StatusErrBgLight, StatusErrFgLight)
        else -> Triple(stringResource(R.string.table_status_available), StatusOkBgLight, StatusOkFgLight)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = table.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            if (table.section.isNotBlank()) append("${table.section} | ")
                            append("${table.capacity} kursi")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(shape = RoundedCornerShape(6.dp), color = bgColor) {
                    Text(
                        text = statusLabel,
                        color = fgColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (table.unpaidOrders > 0) {
                Text(
                    text = "${table.unpaidOrders} pesanan belum dibayar (Rp ${rupiahFormat.format(table.unpaidAmount)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusErrFgLight,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (table.session == null) {
                    OutlinedButton(
                        onClick = onOpenSession,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .sizeIn(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.table_open_session))
                    }
                } else {
                    OutlinedButton(
                        onClick = onCloseSession,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .sizeIn(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.table_close_session))
                    }
                }

                OutlinedButton(
                    onClick = onPoster,
                    enabled = table.qrToken.isNotBlank(),
                    modifier = Modifier.sizeIn(minHeight = 48.dp)
                ) {
                    Icon(Icons.Rounded.QrCode2, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.table_poster_btn))
                }
            }

            if (canManage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onRotateQr,
                        enabled = !isSubmitting && table.status == "available",
                        modifier = Modifier.sizeIn(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.table_rotate_qr_btn))
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.table_edit))
                    }
                    IconButton(
                        onClick = onArchive,
                        enabled = !isSubmitting && table.status == "available"
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.catalog_btn_archive))
                    }
                }
            }
        }
    }
}

@Composable
private fun TableFormDialog(
    initial: TableBoardEntry?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (TableInput) -> Unit
) {
    var tableNumber by remember { mutableStateOf(initial?.tableNumber.orEmpty()) }
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var section by remember { mutableStateOf(initial?.section ?: "Indoor") }
    var capacityText by remember { mutableStateOf((initial?.capacity ?: 4).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.table_add)
                else stringResource(R.string.table_edit)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tableNumber,
                    onValueChange = { tableNumber = it },
                    label = { Text("Nomor Meja (mis. 01)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label Meja (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Area / Lantai (mis. Indoor)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Kapasitas Kursi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    onSave(
                        TableInput(
                            tableNumber = tableNumber.trim(),
                            label = label.trim().ifBlank { null },
                            section = section.trim().ifBlank { null },
                            capacity = capacityText.toIntOrNull() ?: 2,
                            isActive = initial?.isActive ?: true
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.catalog_btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.catalog_btn_cancel))
            }
        }
    )
}

@Composable
private fun TablePosterDialog(
    table: TableBoardEntry,
    storeName: String,
    qrUrl: String,
    isPrintingThermal: Boolean,
    onPrintThermal: () -> Unit,
    onPrintPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(qrUrl) { QrBitmapGenerator.createQrBitmap(qrUrl, 360, 360) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stringResource(R.string.table_poster_btn)}: ${table.displayTitle}") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (table.section.isNotBlank()) {
                    Text(table.section, style = MaterialTheme.typography.bodySmall)
                }
                if (qrBitmap != null) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR ${table.displayTitle}",
                            modifier = Modifier
                                .size(220.dp)
                                .padding(12.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.table_poster_hint),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPrintThermal,
                    enabled = !isPrintingThermal,
                    modifier = Modifier.sizeIn(minHeight = 48.dp)
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.table_print_thermal))
                }
                Button(
                    onClick = onPrintPdf,
                    modifier = Modifier.sizeIn(minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.table_print_pdf))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.catalog_btn_cancel))
            }
        }
    )
}
