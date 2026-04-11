package com.mediakasir.apotekpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.animation.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mediakasir.apotekpos.ui.MainViewModel
import com.mediakasir.apotekpos.ui.Screen
import com.mediakasir.apotekpos.ui.auth.LoginScreen
import com.mediakasir.apotekpos.ui.auth.SplashScreen
import com.mediakasir.apotekpos.ui.main.dashboard.DashboardScreen
import com.mediakasir.apotekpos.ui.main.history.HistoryScreen
import com.mediakasir.apotekpos.ui.main.laporan.LaporanScreen
import com.mediakasir.apotekpos.ui.main.pos.POSScreen
import com.mediakasir.apotekpos.ui.main.prescriptions.PrescriptionsPlaceholderScreen
import com.mediakasir.apotekpos.ui.main.settings.SettingsScreen
import com.mediakasir.apotekpos.ui.main.stok.StokScreen
import com.mediakasir.apotekpos.ui.theme.ApotekTheme
import dagger.hilt.android.AndroidEntryPoint

data class NavItem(
    val labelRes: Int,
    val icon: ImageVector,
    val route: String,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApotekTheme {
                ApotekNavHost()
            }
        }
    }
}

@Composable
fun ApotekNavHost() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = hiltViewModel()

    val license by viewModel.license.collectAsState()
    val user by viewModel.user.collectAsState()

    val navItems = remember(user?.role) {
        val all = listOf(
            NavItem(R.string.nav_pos, Icons.Filled.ShoppingCart, Screen.POS.route),
            NavItem(R.string.nav_stock, Icons.Filled.Inventory, Screen.Stok.route),
            NavItem(R.string.nav_laporan, Icons.Filled.Analytics, Screen.Laporan.route),
            NavItem(R.string.nav_history, Icons.Filled.ReceiptLong, Screen.History.route),
            NavItem(R.string.nav_settings, Icons.Filled.Settings, Screen.Settings.route),
        )
        if (user?.role?.trim()?.equals("kasir", ignoreCase = true) == true) {
            val allow = setOf(Screen.POS.route, Screen.Stok.route, Screen.Laporan.route, Screen.History.route, Screen.Settings.route)
            all.filter { it.route in allow }
        } else {
            all
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showMainNav = currentRoute in navItems.map { it.route }


    DisposableEffect(Unit) {
        val owner = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.refreshSessionAfterDayCheck()
            }
        }
        owner.addObserver(observer)
        onDispose { owner.removeObserver(observer) }
    }

    LaunchedEffect(user, currentRoute) {
        if (user == null &&
            currentRoute != null &&
            currentRoute != Screen.Splash.route &&
            currentRoute != Screen.Login.route
        ) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = false
        Row(Modifier.fillMaxSize()) {
            if (useRail && showMainNav) {
                NavigationRail {
                    navItems.forEach { item ->
                        val label = stringResource(item.labelRes)
                        NavigationRailItem(
                            icon = { Icon(item.icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (!useRail && showMainNav) {
                        NavigationBar(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                .clip(RoundedCornerShape(32.dp)),
                            containerColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            navItems.forEach { item ->
                                val label = stringResource(item.labelRes)
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = label) },
                                    label = { Text(label, maxLines = 1) },
                                    selected = currentRoute == item.route,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = com.mediakasir.apotekpos.ui.theme.Primary,
                                        selectedTextColor = com.mediakasir.apotekpos.ui.theme.Primary,
                                        indicatorColor = com.mediakasir.apotekpos.ui.theme.PrimaryLight,
                                        unselectedIconColor = com.mediakasir.apotekpos.ui.theme.TextSecondary,
                                        unselectedTextColor = com.mediakasir.apotekpos.ui.theme.TextSecondary
                                    ),
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable(Screen.Splash.route) {
                        SplashScreen(
                            viewModel = viewModel,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(Screen.Login.route) {
                        LoginScreen(
                            viewModel = viewModel,
                            onSuccess = {
                                navController.navigate(Screen.POS.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(Screen.Dashboard.route) {
                        DashboardScreen(license = license, user = user)
                    }

                    composable(Screen.POS.route) {
                        POSScreen(license = license, user = user)
                    }

                    composable(Screen.Stok.route) {
                        StokScreen(license = license, user = user)
                    }

                    composable(Screen.History.route) {
                        HistoryScreen(license = license, user = user)
                    }

                    composable(Screen.Laporan.route) {
                        LaporanScreen(license = license, user = user)
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            license = license,
                            user = user,
                            onLogout = {
                                viewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onResetApp = {
                                viewModel.resetAppData()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(Screen.Prescriptions.route) {
                        PrescriptionsPlaceholderScreen()
                    }
                }
            }
        }
    }
}
