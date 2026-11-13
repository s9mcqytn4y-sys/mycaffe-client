package com.azuredevid.cassy.ui.catalog

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.data.model.CatalogCategory
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.CatalogVariant
import com.azuredevid.cassy.data.model.ProductUpsertInput
import com.azuredevid.cassy.data.model.VariantUpsertInput
import kotlinx.coroutines.flow.SharedFlow
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: CatalogManagementViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val rupiahFormat = remember { NumberFormat.getIntegerInstance(Locale("id", "ID")) }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CatalogCategory?>(null) }

    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<CatalogProduct?>(null) }

    var variantsTargetProductId by remember { mutableStateOf<String?>(null) }
    var pendingPhotoProductId by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val productId = pendingPhotoProductId
        pendingPhotoProductId = null
        if (uri != null && !productId.isNullOrBlank()) {
            viewModel.uploadProductImage(productId) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CatalogManagementUiEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                CatalogManagementUiEffect.ProductSaved -> {
                    showProductDialog = false
                    editingProduct = null
                }
                CatalogManagementUiEffect.VariantSaved -> Unit
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_mgmt_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_navigate_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCatalog() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.catalog_mgmt_title)
                        )
                    }
                    if (state.canManageCatalog) {
                        Button(
                            onClick = {
                                editingProduct = null
                                showProductDialog = true
                            },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .sizeIn(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.catalog_add_product))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Left panel: Categories
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.category_all),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.canManageCatalog) {
                        IconButton(onClick = {
                            editingCategory = null
                            showCategoryDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.catalog_add_category)
                            )
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        CategoryFilterItem(
                            name = stringResource(R.string.category_all),
                            selected = state.selectedCategoryId == "all",
                            canManage = false,
                            onSelect = { viewModel.selectCategory("all") },
                            onEdit = {},
                            onDelete = {}
                        )
                    }
                    items(state.categories, key = { it.id }) { category ->
                        CategoryFilterItem(
                            name = category.name,
                            selected = state.selectedCategoryId == category.id,
                            canManage = state.canManageCatalog,
                            onSelect = { viewModel.selectCategory(category.id) },
                            onEdit = {
                                editingCategory = category
                                showCategoryDialog = true
                            },
                            onDelete = { viewModel.deleteCategory(category.id) }
                        )
                    }
                }
            }

            VerticalDivider()

            // Right panel: Search + Products
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!state.canManageCatalog) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.catalog_readonly_hint),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text(stringResource(R.string.catalog_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.isSubmitting || state.isUploadingImage) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                    state.displayedProducts.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.catalog_empty),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.displayedProducts, key = { it.id }) { product ->
                                CatalogProductManagementRow(
                                    product = product,
                                    rupiahFormat = rupiahFormat,
                                    canManage = state.canManageCatalog,
                                    isUploading = state.isUploadingImage,
                                    onToggleAvailability = { viewModel.toggleProductAvailability(product) },
                                    onEdit = {
                                        editingProduct = product
                                        showProductDialog = true
                                    },
                                    onUploadPhoto = {
                                        pendingPhotoProductId = product.id
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onManageVariants = { variantsTargetProductId = product.id },
                                    onArchive = { viewModel.archiveProduct(product.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        CategoryFormDialog(
            initial = editingCategory,
            onDismiss = { showCategoryDialog = false },
            onSave = { name, order ->
                viewModel.saveCategory(editingCategory?.id, name, order, isActive = true)
                showCategoryDialog = false
            }
        )
    }

    if (showProductDialog) {
        ProductFormDialog(
            initial = editingProduct,
            categories = state.categories,
            isSubmitting = state.isSubmitting,
            onDismiss = { showProductDialog = false },
            onSave = { input ->
                viewModel.saveProduct(editingProduct?.id, input)
            }
        )
    }

    val variantProduct = state.products.find { it.id == variantsTargetProductId }
    if (variantProduct != null) {
        ProductVariantsDialog(
            product = variantProduct,
            rupiahFormat = rupiahFormat,
            isSubmitting = state.isSubmitting,
            effects = viewModel.effects,
            onDismiss = { variantsTargetProductId = null },
            onSaveVariant = { editingVarId, input ->
                viewModel.saveVariant(variantProduct.id, editingVarId, input)
            },
            onArchiveVariant = { variantId -> viewModel.archiveVariant(variantId) }
        )
    }
}

@Composable
private fun CategoryFilterItem(
    name: String,
    selected: Boolean,
    canManage: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (canManage) {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.catalog_edit_category)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.catalog_btn_archive)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogProductManagementRow(
    product: CatalogProduct,
    rupiahFormat: NumberFormat,
    canManage: Boolean,
    isUploading: Boolean,
    onToggleAvailability: () -> Unit,
    onEdit: () -> Unit,
    onUploadPhoto: () -> Unit,
    onManageVariants: () -> Unit,
    onArchive: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val hasPhoto = product.imageUrl.isNotBlank()
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (hasPhoto) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (hasPhoto) Icons.Rounded.Image else Icons.Rounded.HideImage,
                        contentDescription = stringResource(
                            if (hasPhoto) R.string.catalog_photo_present else R.string.catalog_photo_missing
                        ),
                        tint = if (hasPhoto) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Rp ${rupiahFormat.format(product.basePriceIdr)} | ${product.prepStation.uppercase()} | ${product.variants.size} varian",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (product.description.isNotBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = product.isAvailable,
                        onCheckedChange = { onToggleAvailability() }
                    )
                    Text(
                        text = if (product.isAvailable) {
                            stringResource(R.string.catalog_status_available)
                        } else {
                            stringResource(R.string.catalog_status_sold_out)
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (canManage) {
                    OutlinedButton(
                        onClick = onUploadPhoto,
                        enabled = !isUploading,
                        modifier = Modifier.sizeIn(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.catalog_btn_upload_photo))
                    }
                    OutlinedButton(
                        onClick = onManageVariants,
                        modifier = Modifier.sizeIn(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.catalog_btn_variants))
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.catalog_edit_product)
                        )
                    }
                    IconButton(onClick = onArchive) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.catalog_btn_archive)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFormDialog(
    initial: CatalogCategory?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var orderText by remember { mutableStateOf((initial?.displayOrder ?: 0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.catalog_add_category)
                else stringResource(R.string.catalog_edit_category)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = orderText,
                    onValueChange = { orderText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Urutan Tampil") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, orderText.toIntOrNull() ?: 0) }) {
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
private fun ProductFormDialog(
    initial: CatalogProduct?,
    categories: List<CatalogCategory>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (ProductUpsertInput) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var selectedCategoryId by remember { mutableStateOf(initial?.categoryId.orEmpty()) }
    var productType by remember { mutableStateOf(initial?.productType ?: "prepared") }
    var prepStation by remember { mutableStateOf(initial?.prepStation ?: "bar") }
    var priceText by remember { mutableStateOf(initial?.basePriceIdr?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.catalog_add_product)
                else stringResource(R.string.catalog_edit_product)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Menu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (initial == null) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Dasar (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("Stasiun Dapur", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("bar" to "Barista", "kitchen" to "Dapur", "none" to "Tanpa Stasiun").forEach { (key, label) ->
                        FilterChip(
                            selected = prepStation == key,
                            onClick = { prepStation = key },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Tipe Menu", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("prepared" to "Diolah", "ready_to_serve" to "Siap Saji").forEach { (key, label) ->
                        FilterChip(
                            selected = productType == key,
                            onClick = { productType = key },
                            label = { Text(label) }
                        )
                    }
                }
                if (categories.isNotEmpty()) {
                    Text("Kategori", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryId = if (isSelected) "" else cat.id
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null) }
                                } else null,
                                label = { Text(cat.name) },
                                modifier = Modifier.sizeIn(minHeight = 48.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    onSave(
                        ProductUpsertInput(
                            categoryId = selectedCategoryId.ifBlank { null },
                            clearCategory = initial != null && selectedCategoryId.isBlank(),
                            name = name,
                            description = description,
                            productType = productType,
                            prepStation = prepStation,
                            isAvailable = initial?.isAvailable ?: true,
                            basePriceIdr = if (initial == null) priceText.toLongOrNull() else null
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
private fun ProductVariantsDialog(
    product: CatalogProduct,
    rupiahFormat: NumberFormat,
    isSubmitting: Boolean,
    effects: SharedFlow<CatalogManagementUiEffect>,
    onDismiss: () -> Unit,
    onSaveVariant: (String?, VariantUpsertInput) -> Unit,
    onArchiveVariant: (String) -> Unit
) {
    var editingVariant by remember { mutableStateOf<CatalogVariant?>(null) }
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }

    LaunchedEffect(effects) {
        effects.collect { effect ->
            if (effect is CatalogManagementUiEffect.VariantSaved) {
                editingVariant = null
                name = ""
                priceText = ""
                isDefault = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stringResource(R.string.catalog_btn_variants)}: ${product.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                product.variants.forEach { variant ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (variant.isDefault) "${variant.name} (Default)" else variant.name,
                                fontWeight = FontWeight.Medium
                            )
                            Text("Rp ${rupiahFormat.format(variant.price)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = {
                                editingVariant = variant
                                name = variant.name
                                priceText = variant.price.toString()
                                isDefault = variant.isDefault
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.catalog_btn_variants))
                            }
                            if (!variant.isDefault) {
                                IconButton(onClick = { onArchiveVariant(variant.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.catalog_btn_archive))
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Varian (mis. Large)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Harga Varian (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        val price = priceText.toLongOrNull() ?: return@Button
                        onSaveVariant(
                            editingVariant?.id,
                            VariantUpsertInput(name = name, price = price, isDefault = isDefault)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                ) {
                    Text(if (editingVariant == null) "Tambah Varian" else "Perbarui Varian")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.catalog_btn_cancel))
            }
        }
    )
}
