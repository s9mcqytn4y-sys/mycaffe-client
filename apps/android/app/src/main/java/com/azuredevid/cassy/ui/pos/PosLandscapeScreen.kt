package com.azuredevid.cassy.ui.pos

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.azuredevid.cassy.R
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.OrderQuoteItem
import com.azuredevid.cassy.data.model.OrderQuoteRequest
import com.azuredevid.cassy.ui.pos.components.*
import com.azuredevid.cassy.ui.shift.ShiftUiEffect
import com.azuredevid.cassy.ui.shift.ShiftViewModel
import com.azuredevid.cassy.ui.shift.components.CashMovementModal
import com.azuredevid.cassy.ui.shift.components.CloseShiftModal
import com.azuredevid.cassy.ui.shift.components.OpenShiftModal
import com.azuredevid.cassy.ui.shift.components.ShiftStatusBanner
import com.azuredevid.cassy.ui.shift.components.XReportModal
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale

data class CartItem(
    val product: CatalogProduct,
    val quantity: Int = 1,
    val notes: String = "",
    val isSentToKitchen: Boolean = false
)

/**
 * Clean, modular 10%-55%-35% Landscape POS Screen for tablets.
 */
@Composable
fun PosLandscapeScreen(
    posViewModel: PosViewModel = koinViewModel(),
    shiftViewModel: ShiftViewModel = koinViewModel(),
    onOpenDrawerKick: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToCatalog: () -> Unit = {},
    onNavigateToPairing: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rupiahFormat = remember { NumberFormat.getIntegerInstance(Locale("id", "ID")) }

    val posState by posViewModel.uiState.collectAsState()
    val shiftState by shiftViewModel.uiState.collectAsState()

    LaunchedEffect(shiftViewModel) {
        shiftViewModel.effects.collect { effect ->
            when (effect) {
                is ShiftUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ShiftUiEffect.PromptOpenShift -> {
                    shiftViewModel.openShiftModal()
                }
                is ShiftUiEffect.ShiftOpenedSuccessfully -> Unit
                is ShiftUiEffect.ShiftClosedSuccessfully -> Unit
            }
        }
    }

    LaunchedEffect(posViewModel) {
        posViewModel.effects.collect { effect ->
            when (effect) {
                is PosUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is PosUiEffect.CashPaymentCompleted -> {
                    shiftViewModel.loadCurrentShift()
                }
                is PosUiEffect.QrisPaymentCompleted -> {
                    shiftViewModel.loadCurrentShift()
                }
            }
        }
    }

    val allCategoryLabel = stringResource(R.string.category_all)
    val categories = remember(posState.categories, allCategoryLabel) {
        listOf("all" to allCategoryLabel) + posState.categories.map { it.id to it.name }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val totalWidth = maxWidth

        Row(modifier = Modifier.fillMaxSize()) {
            PosNavRail(
                railWidth = totalWidth * 0.10f,
                categories = categories,
                selectedCategoryId = posState.selectedCategoryId,
                onCategorySelected = { posViewModel.selectCategory(it) },
                allProducts = posState.allProducts,
                isPrinterOnline = posState.isPrinterConfigured,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToCatalog = onNavigateToCatalog,
                onNavigateToPairing = onNavigateToPairing
            )

            VerticalDivider()

            Column(
                modifier = Modifier
                    .width(totalWidth * 0.55f)
                    .fillMaxHeight()
            ) {
                ShiftStatusBanner(
                    isShiftOpen = shiftState.isShiftOpen,
                    currentShift = shiftState.currentShift,
                    onOpenShiftClick = { shiftViewModel.openShiftModal() },
                    onXReportClick = { shiftViewModel.openXReportModal() },
                    onCashMovementClick = { shiftViewModel.openCashMovementModal() },
                    onCloseShiftClick = { shiftViewModel.openCloseShiftModal() }
                )

                PosCatalogGrid(
                    gridWidth = totalWidth * 0.55f,
                    displayedProducts = posState.displayedProducts,
                    searchQuery = posState.searchQuery,
                    onSearchQueryChange = { posViewModel.updateSearchQuery(it) },
                    isLoadingCatalog = posState.isLoadingCatalog,
                    catalogError = posState.catalogError,
                    onRetryCatalog = { posViewModel.loadInitialData() },
                    onProductSelected = { product -> posViewModel.addProductToCart(product) },
                    rupiahFormat = rupiahFormat,
                    modifier = Modifier.weight(1f)
                )
            }

            VerticalDivider()

            PosOrderSummaryPanel(
                panelWidth = totalWidth * 0.35f,
                tableName = posState.tableName,
                orderType = posState.orderType,
                onTableNameChanged = { posViewModel.updateTableName(it) },
                onToggleOrderType = { posViewModel.toggleOrderType() },
                cart = posState.cart,
                isCartLocked = posState.isCartLocked,
                isShiftOpen = shiftState.isShiftOpen,
                isSubmittingOrder = posState.isSubmittingOrder,
                onIncreaseQuantity = { item -> posViewModel.increaseItemQuantity(item.product.id) },
                onDecreaseQuantity = { item -> posViewModel.decreaseItemQuantity(item.product.id) },
                quote = posState.quote,
                quoteError = posState.quoteError,
                rupiahFormat = rupiahFormat,
                onOpenDrawerKick = {
                    if (posState.isPrinterConfigured) {
                        posViewModel.kickDrawerManual()
                    } else {
                        onOpenDrawerKick()
                    }
                },
                onSendToKitchen = {
                    if (!shiftState.isShiftOpen) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.shift_required_for_kitchen),
                            Toast.LENGTH_SHORT
                        ).show()
                        shiftViewModel.openShiftModal()
                    } else {
                        posViewModel.sendToKitchen()
                    }
                },
                onOpenCashModal = {
                    if (!shiftState.isShiftOpen) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.shift_required_for_payment),
                            Toast.LENGTH_SHORT
                        ).show()
                        shiftViewModel.openShiftModal()
                    } else {
                        posViewModel.openCashModal()
                    }
                },
                onOpenQrisModal = {
                    if (!shiftState.isShiftOpen) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.shift_required_for_payment),
                            Toast.LENGTH_SHORT
                        ).show()
                        shiftViewModel.openShiftModal()
                    } else {
                        posViewModel.startQrisPayment(shiftState.currentShift?.drawerId)
                    }
                },
                failoverWarning = posState.failoverAlert,
                onReprintStations = { posViewModel.reprintStations() }
            )
        }

        val pricedQuote = posState.quote
        if (posState.showCashModal && pricedQuote != null) {
            PosCashModal(
                grandTotal = pricedQuote.totalAmount,
                rupiahFormat = rupiahFormat,
                isSubmitting = posState.isSubmittingOrder,
                errorMessage = posState.settlementError,
                onDismiss = { posViewModel.closeCashModal() },
                onConfirmCashPayment = { tenderedLong, _ ->
                    posViewModel.submitCashPayment(
                        tenderedAmount = tenderedLong,
                        drawerId = shiftState.currentShift?.drawerId
                    )
                }
            )
        }

        val activeQris = posState.activeQrisPayment
        if (activeQris != null) {
            PosQrisModal(
                orderNumber = posState.pendingOrder?.orderNumber.orEmpty(),
                payment = activeQris,
                rupiahFormat = rupiahFormat,
                isCheckingStatus = posState.isCheckingQrisStatus,
                onCheckStatus = { posViewModel.refreshQrisStatus() },
                onDismiss = { posViewModel.dismissQrisModal() }
            )
        }

        if (shiftState.showOpenShiftModal) {
            OpenShiftModal(
                drawers = shiftState.drawers,
                selectedDrawerId = shiftState.selectedDrawerId,
                onDrawerSelected = { shiftViewModel.onDrawerSelected(it) },
                openingFloatInput = shiftState.openingFloatInput,
                notes = shiftState.openShiftNotes,
                isOpening = shiftState.isOpeningShift,
                errorMessage = shiftState.errorMessage,
                onOpeningFloatChanged = { shiftViewModel.onOpeningFloatChanged(it) },
                onNotesChanged = { shiftViewModel.onOpenShiftNotesChanged(it) },
                onSubmit = { shiftViewModel.submitOpenShift() },
                onDismiss = { shiftViewModel.closeOpenShiftModal() }
            )
        }

        if (shiftState.showCashMovementModal) {
            CashMovementModal(
                movementType = shiftState.movementType,
                amountInput = shiftState.movementAmountInput,
                reason = shiftState.movementReason,
                isRecording = shiftState.isRecordingMovement,
                errorMessage = shiftState.errorMessage,
                onMovementTypeChanged = { shiftViewModel.onMovementTypeChanged(it) },
                onAmountChanged = { shiftViewModel.onMovementAmountChanged(it) },
                onReasonChanged = { shiftViewModel.onMovementReasonChanged(it) },
                onSubmit = { shiftViewModel.submitCashMovement() },
                onDismiss = { shiftViewModel.closeCashMovementModal() }
            )
        }

        if (shiftState.showXReportModal) {
            XReportModal(
                report = shiftState.currentXReport,
                isLoading = shiftState.isLoadingXReport,
                isPrinting = shiftState.isPrintingReport,
                onPrint = { shiftViewModel.printCurrentXReport() },
                onDismiss = { shiftViewModel.closeXReportModal() }
            )
        }

        if (shiftState.showCloseShiftModal) {
            CloseShiftModal(
                currentShift = shiftState.currentShift,
                countedCashInput = shiftState.countedCashInput,
                varianceReason = shiftState.varianceReason,
                notes = shiftState.closeShiftNotes,
                supervisorPinInput = shiftState.supervisorPinInput,
                calculatedVariance = shiftState.calculatedVariance,
                isSupervisorAuthRequired = shiftState.isSupervisorAuthRequired,
                printReceiptOnClose = shiftState.printReceiptOnClose,
                isClosing = shiftState.isClosingShift,
                errorMessage = shiftState.errorMessage,
                onCountedCashChanged = { shiftViewModel.onCountedCashChanged(it) },
                onVarianceReasonChanged = { shiftViewModel.onVarianceReasonChanged(it) },
                onNotesChanged = { shiftViewModel.onCloseShiftNotesChanged(it) },
                onSupervisorPinChanged = { shiftViewModel.onSupervisorPinChanged(it) },
                onPrintReceiptToggled = { shiftViewModel.onPrintReceiptOnCloseToggled(it) },
                onSubmit = { shiftViewModel.submitCloseShift() },
                onDismiss = { shiftViewModel.closeCloseShiftModal() }
            )
        }
    }
}

/** Request body for the server quote; order types follow the API (dine_in, takeaway). */
internal fun quoteRequestOf(cart: List<CartItem>, orderType: String) = OrderQuoteRequest(
    orderType = if (orderType == "DINE-IN") "dine_in" else "takeaway",
    items = cart.map { OrderQuoteItem(productId = it.product.id, quantity = it.quantity) }
)
