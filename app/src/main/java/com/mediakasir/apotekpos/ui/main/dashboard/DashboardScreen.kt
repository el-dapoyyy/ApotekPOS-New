package com.mediakasir.apotekpos.ui.main.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediakasir.apotekpos.data.model.DashboardData
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.utils.formatIDR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    license: LicenseInfo?,
    user: UserInfo?,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val dashboard by viewModel.dashboard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(license?.branchId) {
        license?.branchId?.let { viewModel.load(it) }
    }

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
                .background(Color(0xFFFFFFFF))
                .verticalScroll(rememberScrollState())
        ) {
            HeaderSection(user, license)
            MenuSection()
            BannerSection()
            SalesDashboardSection(dashboard)
            SalesGraphSection()
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun HeaderSection(user: UserInfo?, license: LicenseInfo?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Color(0xFF00897B))
            .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                         Icon(Icons.Default.MedicalServices, contentDescription = "Logo", tint = Color(0xFF00897B), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = license?.branchName?.split(" - ")?.firstOrNull() ?: "apoapps Trial",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "ID: ${license?.branchId ?: "jawara"}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Box {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(16.dp)
                                .background(Color(0xFFEF4444), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("6", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Hi, ${user?.name ?: "Firmann"}!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Welcome to apoapps!",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Kode Sesi:",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF22C55E), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "220806-1",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

data class MenuItem(val label: String, val icon: ImageVector)

@Composable
fun MenuSection() {
    val menus = listOf(
        MenuItem("Persediaan", Icons.Default.Inventory2),
        MenuItem("Konsinyasi", Icons.Default.LocalShipping),
        MenuItem("Kontak", Icons.Default.ContactPage),
        MenuItem("Analisis", Icons.Default.RocketLaunch),
        MenuItem("Pelayanan", Icons.Default.MedicalServices),
        MenuItem("Pembelian", Icons.Default.ShoppingCartCheckout),
        MenuItem("Keuangan", Icons.Default.AccountBalanceWallet),
        MenuItem("Laporan", Icons.Default.Article),
        MenuItem("Pengguna", Icons.Default.Group),
        MenuItem("Program\npromo", Icons.Default.Percent)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(180.dp),
            userScrollEnabled = false
        ) {
            items(menus) { menu ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFE0F2F1), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            menu.icon,
                            contentDescription = menu.label,
                            tint = Color(0xFF00897B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = menu.label,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 11.sp,
                        color = Color.Black
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(4.dp)
                    .background(Color(0xFF00897B), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun BannerSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = Color(0xFF00897B),
                    modifier = Modifier.size(48.dp)
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        "Ajak teman apoapps!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Text(
                        "Dapatkan bonus referal jutaan rupiah",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFEF4444), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(6.dp).background(Color(0xFF4DB6AC), CircleShape))
            Box(modifier = Modifier.size(6.dp).background(Color(0xFF00897B), CircleShape))
        }
    }
}

@Composable
fun SalesDashboardSection(dashboard: DashboardData?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Dashboard Penjualan",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    "Tahun ini",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SalesStatItem(
                    label = "Penjualan",
                    value = dashboard?.todayRevenue?.let { formatIDR(it) } ?: "Rp. 94.826.055",
                    color = Color(0xFF00897B)
                )
                SalesStatItem(
                    label = "Retur Penjualan",
                    value = "Rp. -2.294.279",
                    color = Color(0xFF00897B)
                )
                SalesStatItem(
                    label = "Keuntungan",
                    value = "Rp. 29.063.064",
                    color = Color(0xFF00897B)
                )
            }
        }
    }
}

@Composable
fun SalesStatItem(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SalesGraphSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEFDF4)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Grafik Penjualan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF059669)
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(end = 16.dp, top = 24.dp, bottom = 24.dp, start = 50.dp)) {
                val width = size.width
                val height = size.height
                
                val stepY = height / 4
                for (i in 0..4) {
                    val y = height - (i * stepY)
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
                
                val stepX = width / 5
                for (i in 0..5) {
                    val x = (i * stepX)
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                val pointsPink = listOf(
                    Offset(0f, height * 0.4f),
                    Offset(width * 0.2f, height * 0.35f),
                    Offset(width * 0.4f, height * 0.8f),
                    Offset(width * 0.6f, height * 0.4f),
                    Offset(width * 0.8f, height * 0.75f),
                    Offset(width, height * 0.95f)
                )
                val pathPink = Path().apply {
                    moveTo(pointsPink.first().x, pointsPink.first().y)
                    for (i in 1 until pointsPink.size) {
                        lineTo(pointsPink[i].x, pointsPink[i].y)
                    }
                }
                drawPath(pathPink, color = Color(0xFFF472B6), style = Stroke(width = 4f))
                for (point in pointsPink) {
                    drawCircle(color = Color(0xFFF472B6), radius = 6f, center = point)
                }

                val pointsYellow = listOf(
                    Offset(0f, height * 0.85f),
                    Offset(width * 0.2f, height * 0.6f),
                    Offset(width * 0.4f, height * 0.9f),
                    Offset(width * 0.6f, height * 0.85f),
                    Offset(width * 0.8f, height * 0.98f),
                    Offset(width, height * 0.95f)
                )
                val pathYellow = Path().apply {
                    moveTo(pointsYellow.first().x, pointsYellow.first().y)
                    for (i in 1 until pointsYellow.size) {
                        lineTo(pointsYellow[i].x, pointsYellow[i].y)
                    }
                }
                drawPath(pathYellow, color = Color(0xFFFBBF24), style = Stroke(width = 4f))
                for (point in pointsYellow) {
                    drawCircle(color = Color(0xFFFBBF24), radius = 6f, center = point)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(start = 50.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFF472B6), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Total Penjualan", fontSize = 10.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFBBF24), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Keuntungan Penjualan", fontSize = 10.sp, color = Color.Gray)
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 24.dp, bottom = 24.dp)
                    .width(46.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf("Rp 21,7jt", "Rp 16,4jt", "Rp 11,1jt", "Rp 5,8jt", "Rp 0,4jt").forEach { label ->
                    Text(label, fontSize = 9.sp, color = Color.Gray, maxLines = 1)
                }
            }
        }
    }
}
