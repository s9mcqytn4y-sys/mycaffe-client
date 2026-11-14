package com.azuredevid.cassy.ui.shift.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.ui.theme.statusErrFg
import com.azuredevid.cassy.ui.theme.statusOkBg
import com.azuredevid.cassy.ui.theme.statusOkFg
import com.azuredevid.cassy.ui.theme.statusWarnBg
import com.azuredevid.cassy.ui.theme.statusWarnFg
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ShiftStatusBanner(
    isShiftOpen: Boolean,
    currentShift: Shift?,
    onOpenShiftClick: () -> Unit,
    onXReportClick: () -> Unit = {},
    onCashMovementClick: () -> Unit,
    onCloseShiftClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rupiahFormatter = NumberFormat.getIntegerInstance(Locale("id", "ID"))
    val colors = MaterialTheme.colorScheme
    val okBg = statusOkBg()
    val okFg = statusOkFg()
    val warnBg = statusWarnBg()
    val warnFg = statusWarnFg()
    val errFg = statusErrFg()

    if (isShiftOpen && currentShift != null) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            color = okBg,
            border = BorderStroke(1.dp, okFg.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(okFg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val shortId = if (currentShift.id.length > 6) currentShift.id.take(6).uppercase() else currentShift.id.uppercase()
                        Text(
                            text = "SHIFT #$shortId",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.surface
                        )
                    }

                    Column {
                        val cashierName = currentShift.openedByName.ifBlank {
                            stringResource(R.string.shift_banner_cashier_default)
                        }
                        Text(
                            text = stringResource(R.string.shift_banner_cashier_label, cashierName),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = okFg
                        )
                        if (currentShift.expectedCash != null) {
                            Text(
                                text = stringResource(
                                    R.string.shift_banner_expected_cash,
                                    rupiahFormatter.format(currentShift.expectedCash)
                                ),
                                fontSize = 11.sp,
                                color = okFg.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onXReportClick,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                        border = BorderStroke(1.dp, colors.outline)
                    ) {
                        Text(
                            text = stringResource(R.string.shift_banner_xreport_btn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick = onCashMovementClick,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                        border = BorderStroke(1.dp, colors.outline)
                    ) {
                        Text(
                            text = stringResource(R.string.shift_banner_movement_btn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick = onCloseShiftClick,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = errFg),
                        border = BorderStroke(1.dp, errFg.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = stringResource(R.string.shift_banner_close_btn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            color = warnBg,
            border = BorderStroke(1.dp, warnFg.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(warnFg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.shift_banner_closed_badge),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.surface
                        )
                    }

                    Text(
                        text = stringResource(R.string.shift_banner_closed_hint),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = warnFg
                    )
                }

                Button(
                    onClick = onOpenShiftClick,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.shift_banner_open_btn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
