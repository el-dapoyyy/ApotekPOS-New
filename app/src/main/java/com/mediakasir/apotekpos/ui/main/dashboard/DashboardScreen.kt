package com.mediakasir.apotekpos.ui.main.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediakasir.apotekpos.data.model.AlertData
import com.mediakasir.apotekpos.data.model.DashboardData
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.ui.components.SwipeableItem
import com.mediakasir.apotekpos.ui.effectiveBranchId
import com.mediakasir.apotekpos.ui.theme.*
import com.mediakasir.apotekpos.utils.formatDate
import com.mediakasir.apotekpos.utils.formatIDR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    license: LicenseInfo?,
    user: UserInfo?,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val dashboard by viewModel.dashboard.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.error.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }

    val branchId = remember(license, user) { effectiveBranchId(license, user) }
    LaunchedEffect(user?.userId, branchId) {
        if (user != null) {
            viewModel.load(branchId)
        }
    }

    // Sync isRefreshing with isLoading state from ViewModel
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            if (user != null) {
                viewModel.load(branchId)
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            // Curvy Header
            Surface(
                color = Primary,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Selamat Datang,",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${user?.name ?: "Kasir"} 👋",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            license?.branchName?.split(" - ")?.lastOrNull() ?: "Cab. Utama",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            color = Color.White,

                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            loadError?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        err,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (isLoading && dashboard == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // License expiry warning banner
                    val daysLeft = license?.daysRemaining?.toInt() ?: Int.MAX_VALUE
                    if (daysLeft in 0..14) {
                        val isTrial = license?.isTrial == true
                        val bannerColor = if (daysLeft <= 3) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                        val textColor = if (daysLeft <= 3) Error else Warning
                        val icon = if (daysLeft <= 3) Icons.Outlined.Error else Icons.Outlined.Warning
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = bannerColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
                                Column {
                                    Text(
                                        if (isTrial) "Masa Trial Segera Berakhir" else "Lisensi Segera Berakhir",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = textColor,
                                    )
                                    Text(
                                        if (daysLeft <= 0) "Lisensi berakhir hari ini! Hubungi admin untuk perpanjangan."
                                        else "Sisa $daysLeft hari. Segera perpanjang untuk menghindari gangguan layanan.",
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.85f),
                                    )
                                }
                            }
                        }
                    }

                    // Stats Grid
                    dashboard?.let { data ->
                        Text("Ringkasan Hari Ini", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "Pendapatan",
                                value = formatIDR(data.todayRevenue),
                                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                                iconColor = Success,
                                bgColor = StatBgGreen
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "Transaksi",
                                value = data.todayTransactions.toString(),
                                icon = Icons.Outlined.ShoppingCart,
                                iconColor = Primary,
                                bgColor = StatBgTeal
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "Total Produk",
                                value = data.totalProducts.toString(),
                                icon = Icons.Outlined.Inventory,
                                iconColor = Info,
                                bgColor = StatBgBlue
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "Stok Hampir Habis",
                                value = data.lowStockCount.toString(),
                                icon = Icons.Outlined.Warning,
                                iconColor = Warning,
                                bgColor = StatBgAmber
                            )
                        }
                    }

                    // Alerts
                    alerts?.let { alertData ->
                        if (alertData.expiringBatches.isNotEmpty() || alertData.expiredBatches.isNotEmpty() || alertData.lowStockProducts.isNotEmpty()) {
                            AlertsSection(
                                alerts = alertData,
                                onAcknowledgeExpiry = { viewModel.acknowledgeAllExpiry() },
                                onAcknowledgeStock = { viewModel.acknowledgeAllStock() },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
            }
            Column {
                Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsSection(
    alerts: AlertData,
    onAcknowledgeExpiry: () -> Unit,
    onAcknowledgeStock: () -> Unit,
) {
    Text("⚠️ Peringatan", fontWeight = FontWeight.Bold, fontSize = 16.sp)

    if (alerts.expiredBatches.isNotEmpty()) {
        val swipeState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onAcknowledgeExpiry()
                    true
                } else false
            },
        )
        SwipeableItem(
            state = swipeState,
            enableEndToStart = true,
            enableStartToEnd = false,
            endToStartIcon = Icons.Outlined.Check,
            endToStartLabel = "Selesai",
            endToStartColor = Error,
        ) {
            AlertCard(
                title = "Batch Kadaluarsa (${alerts.expiredBatches.size})",
                color = Error,
                items = alerts.expiredBatches.map { "${it.productName} - ${it.batchNumber}" }
            )
        }
    }

    if (alerts.expiringBatches.isNotEmpty()) {
        val swipeState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onAcknowledgeExpiry()
                    true
                } else false
            },
        )
        SwipeableItem(
            state = swipeState,
            enableEndToStart = true,
            enableStartToEnd = false,
            endToStartIcon = Icons.Outlined.Check,
            endToStartLabel = "Selesai",
            endToStartColor = Warning,
        ) {
            AlertCard(
                title = "Mendekati Kadaluarsa (${alerts.expiringBatches.size})",
                color = Warning,
                items = alerts.expiringBatches.map { "${it.productName} - Exp: ${formatDate(it.expiryDate)}" }
            )
        }
    }

    if (alerts.expiredBatches.isNotEmpty() || alerts.expiringBatches.isNotEmpty()) {
        TextButton(onClick = onAcknowledgeExpiry) {
            Text("Tandai alert kadaluarsa selesai")
        }
    }

    if (alerts.lowStockProducts.isNotEmpty()) {
        val swipeState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onAcknowledgeStock()
                    true
                } else false
            },
        )
        SwipeableItem(
            state = swipeState,
            enableEndToStart = true,
            enableStartToEnd = false,
            endToStartIcon = Icons.Outlined.Check,
            endToStartLabel = "Selesai",
            endToStartColor = Info,
        ) {
            AlertCard(
                title = "Stok Hampir Habis (${alerts.lowStockProducts.size})",
                color = Info,
                items = alerts.lowStockProducts.map { "${it.name} - Stok: ${it.currentStock}" }
            )
        }
        TextButton(onClick = onAcknowledgeStock) {
            Text("Tandai alert stok selesai")
        }
    }
}

@Composable
fun AlertCard(title: String, color: Color, items: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                items.take(3).forEach { item ->
                    Text("• $item", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(vertical = 3.dp), fontWeight = FontWeight.Medium)
                }
                if (items.size > 3) {
                    Spacer(Modifier.height(4.dp))
                    Text("Lihat ${items.size - 3} lainnya...", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


