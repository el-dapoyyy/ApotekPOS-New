package com.mediakasir.apotekpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mediakasir.apotekpos.data.network.ApiService
import com.mediakasir.apotekpos.ui.MainViewModel
import com.mediakasir.apotekpos.ui.Screen
import com.mediakasir.apotekpos.ui.auth.LicenseScreen
import com.mediakasir.apotekpos.ui.auth.LoginScreen
import com.mediakasir.apotekpos.ui.main.dashboard.DashboardScreen
import com.mediakasir.apotekpos.ui.main.history.HistoryScreen
import com.mediakasir.apotekpos.ui.main.pos.POSScreen
import com.mediakasir.apotekpos.ui.main.settings.SettingsScreen
import com.mediakasir.apotekpos.ui.main.stok.StokScreen
import com.mediakasir.apotekpos.ui.theme.ApotekTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var api: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApotekTheme {
                ApotekApp(api = api)
            }
        }
    }
}

@Composable
fun ApotekApp(api: ApiService) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = hiltViewModel()

    val license by viewModel.license.collectAsState()
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Determine start destination
    val startDestination = when {
        license == null -> Screen.License.route
        user == null -> Screen.Login.route
        else -> Screen.Dashboard.route
    }

    val bottomNavItems = listOf(
        BottomNavItem("Beranda", Icons.Filled.Home, Screen.Dashboard.route),
        BottomNavItem("Kasir", Icons.Filled.ShoppingCart, Screen.POS.route),
        BottomNavItem("Stok", Icons.Filled.Inventory, Screen.Stok.route),
        BottomNavItem("Riwayat", Icons.Filled.ReceiptLong, Screen.History.route),
        BottomNavItem("Setelan", Icons.Filled.Settings, Screen.Settings.route),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    if (isLoading) {
        // Splash screen
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.License.route) {
                LicenseScreen(
                    viewModel = viewModel,
                    onSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.License.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = viewModel,
                    onSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(license = license, user = user)
            }

            composable(Screen.POS.route) {
                POSScreen(license = license, user = user)
            }

            composable(Screen.Stok.route) {
                StokScreen(license = license)
            }

            composable(Screen.History.route) {
                HistoryScreen(license = license)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    license = license,
                    user = user,
                    viewModel = viewModel,
                    api = api,
                    onLogout = {
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onClearLicense = {
                        viewModel.clearLicense()
                        navController.navigate(Screen.License.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

