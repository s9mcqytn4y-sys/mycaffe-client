package com.azuredevid.cassy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.azuredevid.cassy.core.security.SessionManager
import com.azuredevid.cassy.ui.catalog.CatalogManagementScreen
import com.azuredevid.cassy.ui.pairing.StorePairingScreen
import com.azuredevid.cassy.ui.pos.PosLandscapeScreen
import com.azuredevid.cassy.ui.settings.ServerConfigScreen
import com.azuredevid.cassy.ui.settings.StoreSettingsScreen

@Composable
fun CassyNavHost(
    navController: NavHostController,
    startDestination: CassyRoute,
    sessionManager: SessionManager,
    onOpenCashDrawer: () -> Unit,
    onShowPairingToast: (storeName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSession by sessionManager.sessionFlow.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Fail-closed: if session becomes invalid/cleared while inside POS or Settings, return to Pairing.
    LaunchedEffect(activeSession, currentRoute) {
        val isPublicAuthRoute = currentRoute?.contains("StorePairing") == true ||
            currentRoute?.contains("ServerConfig") == true
        if (activeSession == null && currentRoute != null && !isPublicAuthRoute) {
            navController.navigate(CassyRoute.StorePairing) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<CassyRoute.StorePairing> {
            StorePairingScreen(
                onNavigateToServerConfig = {
                    navController.navigate(CassyRoute.ServerConfig)
                },
                onPairingSuccess = { _, storeName ->
                    onShowPairingToast(storeName)
                    navController.navigate(CassyRoute.PosCashier) {
                        popUpTo(CassyRoute.StorePairing) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<CassyRoute.ServerConfig> {
            ServerConfigScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable<CassyRoute.PosCashier> {
            PosLandscapeScreen(
                onOpenDrawerKick = onOpenCashDrawer,
                onNavigateToSettings = {
                    navController.navigate(CassyRoute.StoreSettings)
                },
                onNavigateToCatalog = {
                    navController.navigate(CassyRoute.CatalogManagement)
                },
                onNavigateToPairing = {
                    navController.navigate(CassyRoute.StorePairing)
                }
            )
        }

        composable<CassyRoute.StoreSettings> {
            StoreSettingsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable<CassyRoute.CatalogManagement> {
            CatalogManagementScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
