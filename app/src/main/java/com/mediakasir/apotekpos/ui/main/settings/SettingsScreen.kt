package com.mediakasir.apotekpos.ui.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediakasir.apotekpos.R
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    license: LicenseInfo?,
    user: UserInfo?,
    onLogout: () -> Unit,
    onLogoutAllDevices: () -> Unit,
    onResetApp: () -> Unit,
    onRefresh: () -> Unit = {},
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showTutupKasirDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Stop refreshing when data changes (license or user updated)
    LaunchedEffect(license, user) {
        isRefreshing = false
    }

    var selectedTab by remember { mutableStateOf("Informasi Apotek") }
    val tabs = listOf("Informasi Apotek", "Informasi Pengguna", "Akun & Keamanan", "Tentang Aplikasi")

    val coroutineScope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
            // Paksa berhenti setelah 500ms karena jika data tidak ada yang berubah,
            // LaunchedEffect(license, user) tidak akan terpanggil lagi oleh Compose.
            coroutineScope.launch {
                kotlinx.coroutines.delay(500)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top App Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Setelan", 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold, 
                color = Color.White
            )
        }
        
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Sidebar
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .background(Color.White)
            ) {
                // Profile Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(shape = CircleShape, color = Primary, modifier = Modifier.size(72.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(user?.email ?: "email@pengguna.com", fontSize = 14.sp, color = TextPrimary)
                    Text("Role: ${user?.role?.uppercase()}", fontSize = 11.sp, color = TextSecondary)
                }
                
                // Menu Items
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val bgColor = if (isSelected) Primary else Color.White
                    val contentColor = if (isSelected) Color.White else TextPrimary
                    val iconColor = if (isSelected) Color.White else Primary
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor)
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when(tab) {
                            "Informasi Apotek" -> Icons.Outlined.Home
                            "Informasi Pengguna" -> Icons.Outlined.Group
                            "Akun & Keamanan" -> Icons.Outlined.Lock
                            else -> Icons.Outlined.Info
                        }
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(tab, color = contentColor, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        
                        if (!isSelected) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Right Content Area
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .background(Background)
                    .padding(horizontal = 40.dp, vertical = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(selectedTab, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(24.dp))
                
                if (selectedTab != "Tentang Aplikasi") {
                    Text(
                        when(selectedTab) {
                            "Informasi Apotek" -> "Data Apotek"
                            "Informasi Pengguna" -> "Detail Akun"
                            "Akun & Keamanan" -> "Security Options"
                            else -> ""
                        }, 
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                    )
                    Text("Terakhir diperbarui hari ini", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Border)
                    Spacer(Modifier.height(16.dp))
                }
                
                if (selectedTab != "Tentang Aplikasi") {
                    Text("Options", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Spacer(Modifier.height(12.dp))
                
                when(selectedTab) {
                    "Informasi Apotek" -> {
                        DetailRow("Nama Apotek", license?.pharmacyName ?: "-")
                        DetailRow("Cabang", license?.branchName ?: "-")
                        DetailRow("Alamat Lengkap", license?.address ?: "-")
                        DetailRow("Nomor Telepon", license?.phone ?: "-")
                    }
                    "Informasi Pengguna" -> {
                        DetailRow("Alamat Email", user?.email ?: "-")
                        DetailRow("Nama Pengguna", user?.name ?: "-")
                        DetailRow("Role Sistem", user?.role?.uppercase() ?: "-")
                    }
                    "Tentang Aplikasi" -> {
                        DetailRow("Nama Aplikasi", "ApoApps POS")
                        DetailRow("Versi", "v1.0")
                        DetailRow("Kontak", "support@apoapps.sekawanputrapratama.com")
                        DetailRow("Deskripsi", "Aplikasi Point of Sale untuk manajemen apotek secara digital dan terintegrasi.")
                    }
                    "Akun & Keamanan" -> {
                        DetailRow("Ganti Password", "Ganti password dilakukan lewat ApoApps web (admin).")
                        DetailRow("Keamanan", "Aplikasi POS hanya sinkron dengan API.")

                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = { showTutupKasirDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Warning),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tutup Kasir", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }

                        Spacer(Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Error),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Error
                            )
                        ) {
                            Text("Logout", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Error)
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = onLogoutAllDevices,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)), // Merah Tua
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Logout Semua Perangkat", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)), // Merah Solid (paling tegas)
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⚠️ Reset Data Aplikasi ⚠️", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                    }
                }
                
                Spacer(Modifier.height(120.dp))
            }
        }
    }
    } // PullToRefreshBox

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Anda yakin ingin keluar?") },
            confirmButton = {
                Button(onClick = { showLogoutDialog = false; onLogout() }) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showTutupKasirDialog) {
        AlertDialog(
            onDismissRequest = { showTutupKasirDialog = false },
            title = { Text("Tutup Kasir") },
            text = { Text("Anda akan melakukan Tutup Kasir (End Shift) dan Logout. Lanjutkan?") },
            confirmButton = {
                Button(onClick = { showTutupKasirDialog = false; onLogout() }) { Text("Ya, Tutup") }
            },
            dismissButton = {
                TextButton(onClick = { showTutupKasirDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_reset_app)) },
            text = { Text(stringResource(R.string.settings_reset_app_message)) },
            confirmButton = {
                Button(
                    onClick = { showClearDialog = false; onResetApp() },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) { Text(stringResource(R.string.settings_reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun DetailRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 13.sp, color = TextSecondary)
        }
    }
}
