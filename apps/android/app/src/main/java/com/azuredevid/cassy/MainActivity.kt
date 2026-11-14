package com.azuredevid.cassy

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.azuredevid.cassy.core.hardware.PrinterPort9100Manager
import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.domain.repository.PrinterSettingsRepository
import com.azuredevid.cassy.ui.navigation.CassyNavHost
import com.azuredevid.cassy.ui.navigation.CassyRoute
import com.azuredevid.cassy.ui.theme.CassyPandan
import com.azuredevid.cassy.ui.theme.CassyTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {

    private val printerManager: PrinterPort9100Manager by inject()
    private val printerSettings: PrinterSettingsRepository by inject()
    private val sessionManager: SessionManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CassyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var startRoute by remember { mutableStateOf<CassyRoute?>(null) }

                    LaunchedEffect(Unit) {
                        val existing = sessionManager.awaitLoaded()
                        startRoute = if (existing?.isAuthenticated == true) {
                            CassyRoute.PosCashier
                        } else {
                            CassyRoute.StorePairing
                        }
                    }

                    val initial = startRoute
                    if (initial == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CassyPandan)
                        }
                    } else {
                        val navController = rememberNavController()
                        CassyNavHost(
                            navController = navController,
                            startDestination = initial,
                            sessionManager = sessionManager,
                            onOpenCashDrawer = {
                                lifecycleScope.launch {
                                    val config = printerSettings.getConfig()
                                    if (!config.hasCashierPrinter) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.printer_error_not_configured),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@launch
                                    }
                                    printerManager.openCashDrawer(
                                        config.cashierPrinterIp,
                                        config.cashierPrinterPort
                                    ).onSuccess {
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.drawer_open_success),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure {
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.printer_lan_unreachable),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onShowPairingToast = { storeName ->
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.pairing_connected_toast, storeName),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}
