package com.azuredevid.cassy.ui.shift.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.azuredevid.cassy.R
import com.azuredevid.cassy.domain.model.XReport
import com.azuredevid.cassy.ui.theme.statusWarnBg
import com.azuredevid.cassy.ui.theme.statusWarnFg
import java.text.NumberFormat
import java.util.Locale

@Composable
fun XReportModal(
    report: XReport?,
    isLoading: Boolean,
    isPrinting: Boolean,
    onPrint: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rupiahFormatter = NumberFormat.getIntegerInstance(Locale("id", "ID"))
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.shift_xreport_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                    }

                    report != null -> {
                        val s = report.summary
                        XReportRow(stringResource(R.string.shift_xreport_opening_float), "Rp ${rupiahFormatter.format(s.openingFloat)}")
                        XReportRow(stringResource(R.string.shift_xreport_cash_sales), "Rp ${rupiahFormatter.format(s.cashSales)}")
                        if (s.cashIns > 0L) {
                            XReportRow(stringResource(R.string.shift_xreport_cash_ins), "Rp ${rupiahFormatter.format(s.cashIns)}")
                        }
                        if (s.paidOuts > 0L) {
                            XReportRow(stringResource(R.string.shift_xreport_paid_outs), "-Rp ${rupiahFormatter.format(s.paidOuts)}")
                        }
                        if (s.cashRefunds > 0L) {
                            XReportRow(stringResource(R.string.shift_xreport_refunds), "-Rp ${rupiahFormatter.format(s.cashRefunds)}")
                        }
                        HorizontalDivider(color = colors.outline)

                        val expectedValue = if (report.expectedRedacted || report.expectedCash == null) {
                            stringResource(R.string.shift_xreport_expected_redacted)
                        } else {
                            "Rp ${rupiahFormatter.format(report.expectedCash)}"
                        }
                        XReportRow(
                            label = stringResource(R.string.shift_xreport_expected_cash),
                            value = expectedValue,
                            emphasize = true
                        )

                        if (report.unsettledOpenTabsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(statusWarnBg(), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.shift_xreport_unsettled_tabs,
                                        report.unsettledOpenTabsCount,
                                        rupiahFormatter.format(report.unsettledOpenTabsTotalAmount)
                                    ),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = statusWarnFg()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.shift_xreport_close_btn), color = colors.onSurface)
                    }

                    Button(
                        onClick = onPrint,
                        enabled = report != null && !isLoading && !isPrinting,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.shift_xreport_print_btn))
                    }
                }
            }
        }
    }
}

@Composable
private fun XReportRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasize) colors.onSurface else colors.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = if (emphasize) 15.sp else 13.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = colors.onSurface
        )
    }
}
