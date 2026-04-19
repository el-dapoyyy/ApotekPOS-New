package com.mediakasir.apotekpos.ui.main.laporan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.ui.theme.Background
import com.mediakasir.apotekpos.ui.theme.Primary
import com.mediakasir.apotekpos.ui.theme.TextMuted
import com.mediakasir.apotekpos.ui.theme.TextPrimary
import com.mediakasir.apotekpos.ui.theme.TextSecondary
import com.mediakasir.apotekpos.utils.formatIDR
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateDisplay = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val DateDisplay2 = DateTimeFormatter.ofPattern("dd/MM/yy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(
    license: LicenseInfo?,
    user: UserInfo?,
    viewModel: LaporanViewModel = hiltViewModel(),
) {
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var startDate by remember { mutableStateOf(viewModel.defaultStart) }
    var endDate by remember { mutableStateOf(viewModel.defaultEnd) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isLoading) {
        if (!isLoading) isRefreshing = false
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            initialSelectedEndDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedStartDateMillis?.let { ms ->
                        startDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    pickerState.selectedEndDateMillis?.let { ms ->
                        endDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            },
        ) {
            DateRangePicker(
                state = pickerState,
                modifier = Modifier.weight(1f),
                title = { Text("Pilih Rentang Tanggal", modifier = Modifier.padding(start = 16.dp)) },
                headline = {
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        val s = pickerState.selectedStartDateMillis
                        val e = pickerState.selectedEndDateMillis
                        Text(
                            text = if (s != null) Instant.ofEpochMilli(s).atZone(ZoneId.systemDefault()).toLocalDate().format(DateDisplay) else "Mulai",
                            fontWeight = FontWeight.Medium,
                        )
                        Text("  \u2013  ", color = TextMuted)
                        Text(
                            text = if (e != null) Instant.ofEpochMilli(e).atZone(ZoneId.systemDefault()).toLocalDate().format(DateDisplay) else "Selesai",
                            fontWeight = FontWeight.Medium,
                        )
                    }
                },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = "Laporan",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Background,
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.loadReport(startDate, endDate)
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Periode Laporan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showDatePicker = true },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Background,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Outlined.CalendarMonth,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "${startDate.format(DateDisplay2)}  \u2013  ${endDate.format(DateDisplay2)}",
                                                fontSize = 13.sp,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { viewModel.loadReport(startDate, endDate) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = !isLoading,
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Text("Muat", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    summary?.let { s ->
                        item {
                            Text(
                                text = "Ringkasan",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                SummaryCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.Receipt,
                                    label = "Transaksi",
                                    value = "${s.totalTransactions}",
                                    color = Primary,
                                )
                                SummaryCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                                    label = "Total Pendapatan",
                                    value = formatIDR(s.totalRevenue),
                                    color = Color(0xFF388E3C),
                                )
                                SummaryCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.ShoppingCart,
                                    label = "Rata-rata",
                                    value = formatIDR(s.avgTransaction),
                                    color = Color(0xFF1565C0),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    if (transactions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Daftar Transaksi (${transactions.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp),
                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Background)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                ) {
                                    Text("Invoice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(2f))
                                    Text("Pelanggan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(2f))
                                    Text("Total", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(2f), textAlign = TextAlign.End)
                                    Text("Tanggal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(2f), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }

                    itemsIndexed(transactions, key = { _, row -> row.id }) { idx, row ->
                        val bg = if (idx % 2 == 0) Color.White else Color(0xFFF9F9F9)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg)
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = row.invoiceNumber?.take(12) ?: "#${row.id}",
                                fontSize = 11.sp,
                                color = Primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(2f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = row.customerName ?: "Umum",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(2f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = formatIDR(row.grandTotal ?: 0.0),
                                fontSize = 11.sp,
                                color = Color(0xFF388E3C),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(2f),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = (row.completedAt ?: row.syncedAt)?.take(10)
                                    ?.let { runCatching { LocalDate.parse(it).format(DateDisplay2) }.getOrDefault(it) }
                                    ?: "-",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.weight(2f),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                            )
                        }
                        if (idx < transactions.lastIndex) {
                            HorizontalDivider(color = Background, thickness = 0.5.dp)
                        }
                    }

                    if (!isLoading && transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Outlined.Receipt,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(text = "Belum ada laporan", color = TextMuted, fontSize = 14.sp)
                                    Text(text = "Pilih periode dan tekan Muat", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(snackbarHostState)
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(text = label, fontSize = 10.sp, color = TextMuted, maxLines = 1)
        }
    }
}
