package online.abbdullaziz.mycaffe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

data class PosOrderSummaryUiState(
    val storeName: String,
    val queueCode: String,
    val totalIdr: Long,
    val printerStatusLabel: String,
    val isPrinterOnline: Boolean
)

@Composable
fun SemanticBadge(
    label: String,
    tone: String,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (tone) {
        "ok" -> if (darkTheme) StatusOkBgDark to StatusOkFgDark else StatusOkBgLight to StatusOkFgLight
        "warn" -> if (darkTheme) StatusWarnBgDark to StatusWarnFgDark else StatusWarnBgLight to StatusWarnFgLight
        else -> if (darkTheme) StatusErrBgDark to StatusErrFgDark else StatusErrBgLight to StatusErrFgLight
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun PosCashierHeaderBar(
    state: PosOrderSummaryUiState,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onQuickPrintNav: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rupiahFormatter = NumberFormat.getIntegerInstance(Locale("id", "ID"))
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Antrean ${state.queueCode} | Rp ${rupiahFormatter.format(state.totalIdr)} | online.abbdullaziz.mycaffe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_toggle_theme")
                        .semantics {
                            contentDescription = if (darkTheme) "Ganti ke Tema Terang" else "Ganti ke Tema Gelap"
                        }
                ) {
                    Text(text = if (darkTheme) "Mode Terang" else "Mode Gelap")
                }
                Button(
                    onClick = onQuickPrintNav,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_header_printer"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = state.printerStatusLabel)
                }
            }
        }
    }
}
