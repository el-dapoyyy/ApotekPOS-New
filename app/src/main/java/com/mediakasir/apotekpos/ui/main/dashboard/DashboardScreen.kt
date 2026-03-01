package com.mediakasir.apotekpos.ui.main.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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

    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(license?.branchId) {
        license?.branchId?.let { viewModel.load(it) }
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
            license?.branchId?.let { viewModel.load(it) }
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
                                icon = Icons.Filled.TrendingUp,
                                iconColor = Success,
                                bgColor = Color(0xFFECFDF5)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "Transaksi",
                                value = data.todayTransactions.toString(),
                                icon = Icons.Filled.ShoppingCart,
                                iconColor = Primary,
                                bgColor = Color(0xFFE0F2F1)
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
                                icon = Icons.Filled.Inventory,
                                iconColor = Info,
                                bgColor = Color(0xFFEFF6FF)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "Stok Hampir Habis",
                                value = data.lowStockCount.toString(),
                                icon = Icons.Filled.Warning,
                                iconColor = Warning,
                                bgColor = Color(0xFFFFFBEB)
                            )
                        }
                    }

                    // Alerts
                    alerts?.let { alertData ->
                        if (alertData.expiringBatches.isNotEmpty() || alertData.expiredBatches.isNotEmpty() || alertData.lowStockProducts.isNotEmpty()) {
                            AlertsSection(alertData)
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

@Composable
fun AlertsSection(alerts: AlertData) {
    Text("⚠️ Peringatan", fontWeight = FontWeight.Bold, fontSize = 16.sp)

    if (alerts.expiredBatches.isNotEmpty()) {
        AlertCard(
            title = "Batch Kadaluarsa (${alerts.expiredBatches.size})",
            color = Error,
            items = alerts.expiredBatches.map { "${it.productName} - ${it.batchNumber}" }
        )
    }

    if (alerts.expiringBatches.isNotEmpty()) {
        AlertCard(
            title = "Mendekati Kadaluarsa (${alerts.expiringBatches.size})",
            color = Warning,
            items = alerts.expiringBatches.map { "${it.productName} - Exp: ${formatDate(it.expiryDate)}" }
        )
    }

    if (alerts.lowStockProducts.isNotEmpty()) {
        AlertCard(
            title = "Stok Hampir Habis (${alerts.lowStockProducts.size})",
            color = Info,
            items = alerts.lowStockProducts.map { "${it.name} - Stok: ${it.currentStock}" }
        )
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


