package com.azuredevid.cassy.ui.shift.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuredevid.cassy.R
import com.azuredevid.cassy.domain.model.Shift
import com.azuredevid.cassy.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ShiftStatusBanner(
    isShiftOpen: Boolean,
    currentShift: Shift?,
    onOpenShiftClick: () -> Unit,
    onCashMovementClick: () -> Unit,
    onCloseShiftClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rupiahFormatter = NumberFormat.getIntegerInstance(Locale("id", "ID"))

    if (isShiftOpen && currentShift != null) {
        // Active Open Shift Banner
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            color = StatusOkBgLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, StatusOkFgLight.copy(alpha = 0.3f))
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
                            .background(StatusOkFgLight, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val shortId = if (currentShift.id.length > 6) currentShift.id.take(6).uppercase() else currentShift.id.uppercase()
                        Text(
                            text = "SHIFT #$shortId",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column {
                        val cashierName = currentShift.openedByName.ifBlank { "Kasir" }
                        Text(
                            text = "Kasir: $cashierName",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusOkFgLight
                        )
                        if (currentShift.expectedCash != null) {
                            Text(
                                text = "Estimasi Kas: Rp " + rupiahFormatter.format(currentShift.expectedCash),
                                fontSize = 11.sp,
                                color = StatusOkFgLight.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCashMovementClick,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightInk),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrFgLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusErrFgLight.copy(alpha = 0.5f))
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
        // Shift Closed / Inactive Banner
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            color = StatusWarnBgLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarnFgLight.copy(alpha = 0.3f))
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
                            .background(StatusWarnFgLight, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.shift_banner_closed_badge),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = stringResource(R.string.shift_banner_closed_hint),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = StatusWarnFgLight
                    )
                }

                Button(
                    onClick = onOpenShiftClick,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CassyPandan)
                ) {
                    Text(
                        text = stringResource(R.string.shift_banner_open_btn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
