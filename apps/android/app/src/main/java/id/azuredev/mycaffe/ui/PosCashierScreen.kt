package id.azuredev.mycaffe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

private val EspressoPrimary = Color(0xFF2C1A12)
private val CremaCanvas = Color(0xFFFAF6F1)
private val MatchaPaidBg = Color(0xFFDCFCE7)
private val MatchaPaidFg = Color(0xFF166534)

data class PosOrderSummaryUiState(
    val storeName: String,
    val queueCode: String,
    val totalIdr: Long,
    val printerStatusLabel: String,
    val isPrinterOnline: Boolean
)

@Composable
fun PosCashierHeaderBar(
    state: PosOrderSummaryUiState,
    onTestPrint: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rupiahFormatter = NumberFormat.getIntegerInstance(Locale("id", "ID"))
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(CremaCanvas),
        color = CremaCanvas
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = state.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = EspressoPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Antrean ${state.queueCode} • Rp ${rupiahFormatter.format(state.totalIdr)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EspressoPrimary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onTestPrint,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isPrinterOnline) MatchaPaidBg else EspressoPrimary,
                        contentColor = if (state.isPrinterOnline) MatchaPaidFg else Color.White
                    )
                ) {
                    Text(text = state.printerStatusLabel)
                }
                Button(
                    onClick = onOpenDrawer,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EspressoPrimary)
                ) {
                    Text(text = "Buka Laci Kasir")
                }
            }
        }
    }
}
