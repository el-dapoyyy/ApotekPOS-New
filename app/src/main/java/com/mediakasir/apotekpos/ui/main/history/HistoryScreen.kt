package com.mediakasir.apotekpos.ui.main.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.Transaction
import com.mediakasir.apotekpos.ui.theme.*
import com.mediakasir.apotekpos.utils.formatDateTime
import com.mediakasir.apotekpos.utils.formatIDR

@Composable
fun HistoryScreen(
    license: LicenseInfo?,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTrx by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(license?.branchId) {
        license?.branchId?.let { viewModel.load(it, refresh = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .padding(16.dp)
        ) {
            Text("Riwayat Transaksi", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (isLoading && transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextMuted)
                    Text("Belum ada transaksi", color = TextMuted, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { trx ->
                    TransactionCard(trx) { selectedTrx = trx }
                }
                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Primary)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    selectedTrx?.let { trx ->
        TransactionDetailDialog(trx) { selectedTrx = null }
    }
}

@Composable
fun TransactionCard(transaction: Transaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Secondary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Receipt, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.transactionNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Kasir: ${transaction.cashierName}", fontSize = 12.sp, color = TextSecondary)
                Text(formatDateTime(transaction.createdAt), fontSize = 12.sp, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatIDR(transaction.totalAmount), fontWeight = FontWeight.Bold, color = Primary)
                Text("${transaction.items.size} item", fontSize = 12.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(transaction: Transaction, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(transaction.transactionNumber) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(formatDateTime(transaction.createdAt), fontSize = 12.sp, color = TextMuted)
                Text("Kasir: ${transaction.cashierName}", fontSize = 13.sp)
                Divider()
                transaction.items.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.productName} (${item.qty})", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text(formatIDR(item.subtotal), fontSize = 13.sp)
                    }
                }
                Divider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal"); Text(formatIDR(transaction.subtotal))
                }
                if (transaction.discount > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diskon"); Text(formatIDR(transaction.discount))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", fontWeight = FontWeight.Bold); Text(formatIDR(transaction.totalAmount), fontWeight = FontWeight.Bold, color = Primary)
                }
                transaction.paymentDetails.forEach { p ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.method.uppercase()); Text(formatIDR(p.amount))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kembalian"); Text(formatIDR(transaction.change))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
