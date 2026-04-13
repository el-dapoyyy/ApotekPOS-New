package com.mediakasir.apotekpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(
        bottomBar = {
            if (showMainNav) {
                ApotekBottomBar(
                    items = navItems,
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
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
                    onLogoutAllDevices = {
                        viewModel.logoutAllDevices()
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

/* ─────────────────────────────────────────────────────────────────────── */
/*  Custom Bottom Navigation Bar (matches reference design)              */
/* ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun ApotekBottomBar(
    items: List<NavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
) {
    val accent = com.mediakasir.apotekpos.ui.theme.Primary        // green
    val inactiveIcon = Color(0xFF6B7280)   // gray-500
    val inactiveText = Color(0xFF9CA3AF)   // gray-400

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding(),
    ) {
        // thin top divider
        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E7EB))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val label = stringResource(item.labelRes)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onItemClick(item.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Icon with pill-shaped highlight when selected
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) accent else Color.Transparent)
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = label,
                            modifier = Modifier.size(26.dp),
                            tint = if (selected) Color.White else inactiveIcon,
                        )
                    }

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color(0xFF1F2937) else inactiveText,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
