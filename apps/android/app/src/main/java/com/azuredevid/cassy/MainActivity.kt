package com.azuredevid.cassy

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.ui.pos.PosLandscapeScreen
import com.azuredevid.cassy.ui.settings.MidtransConfigScreen
import com.azuredevid.cassy.ui.theme.CassyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val printerManager = PrinterPort9100Manager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CassyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by rememberSaveable { mutableStateOf("pos") }

                    when (currentScreen) {
                        "pos" -> {
                            PosLandscapeScreen(
                                onOpenDrawerKick = {
                                    lifecycleScope.launch {
                                        // Pemicu Drawer Kick ESC/POS Pin 2 ke Printer Kasir LAN Port 9100
                                        printerManager.openCashDrawer("192.168.10.200", 9100)
                                            .onSuccess {
                                                Toast.makeText(this@MainActivity, "Perintah Buka Laci Kasir Terkirim (ESC/POS 1B700019FA)", Toast.LENGTH_SHORT).show()
                                            }
                                            .onFailure {
                                                Toast.makeText(this@MainActivity, "Simulasi Buka Laci (Printer LAN tidak terjangkau)", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                },
                                onNavigateToSettings = {
                                    currentScreen = "settings"
                                }
                            )
                        }
                        "settings" -> {
                            MidtransConfigScreen(
                                onSaveSuccess = { response ->
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Kredensial tersimpan: ${response.merchantId} [${response.environment}] (Status: ${response.validationStatus})",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    currentScreen = "pos"
                                },
                                onNavigateBack = {
                                    currentScreen = "pos"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

