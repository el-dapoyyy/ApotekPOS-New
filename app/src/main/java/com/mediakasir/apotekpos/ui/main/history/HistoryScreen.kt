package com.mediakasir.apotekpos.ui.main.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.Transaction
import com.mediakasir.apotekpos.data.model.TransactionItem
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.ui.effectiveBranchId
import com.mediakasir.apotekpos.ui.theme.*
import com.mediakasir.apotekpos.utils.formatDateTime
import com.mediakasir.apotekpos.utils.formatIDR
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun HistoryScreen(
    license: LicenseInfo?,
    user: UserInfo?,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var dialogTrx by remember { mutableStateOf<Transaction?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var returnTrx by remember { mutableStateOf<Transaction?>(null) }
    var returnError by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val branchId = remember(license, user) { effectiveBranchId(license, user) }

    LaunchedEffect(user?.userId, branchId) {
        if (user != null) {
            viewModel.load(branchId, refresh = true)
        }
    }

    LaunchedEffect(listState, transactions.size, isLoading, user?.userId, branchId) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            if (last == null || transactions.isEmpty()) false
            else last >= transactions.lastIndex
        }.distinctUntilChanged().collect { atEnd ->
            if (atEnd && !isLoading && transactions.isNotEmpty() && user != null) {
                viewModel.load(branchId, refresh = false)
            }
        }
    }

    fun openTransaction(t: Transaction) {
        dialogTrx = t
        if (t.items.isEmpty() && t.id.isNotBlank()) {
            detailLoading = true
            viewModel.fetchTransactionDetail(t.id) { full ->
                detailLoading = false
                if (full != null && dialogTrx?.id == t.id) dialogTrx = full
            }
        } else {
            detailLoading = false
        }
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
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { trx ->
                    TransactionCard(trx) { openTransaction(trx) }
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

    dialogTrx?.let { trx ->
        TransactionDetailDialog(
            transaction = trx,
            isLoadingDetail = detailLoading && trx.items.isEmpty(),
            onDismiss = { dialogTrx = null; detailLoading = false },
            onRequestReturn = if (detailLoading) null else {
                {
                    returnTrx = trx
                    returnError = null
                }
            },
        )
    }

    returnTrx?.let { trx ->
        ReturnFormDialog(
            transaction = trx,
            errorMessage = returnError,
            onDismiss = { returnTrx = null; returnError = null },
            onSubmit = { reason, lines ->
                val tid = trx.id.toIntOrNull() ?: return@ReturnFormDialog
                viewModel.submitReturn(
                    transactionId = tid,
                    reason = reason,
                    lines = lines,
                ) { err ->
                    if (err == null) {
                        returnTrx = null
                        dialogTrx = null
                        if (user != null) {
                            viewModel.load(branchId, refresh = true)
                        }
                    } else {
                        returnError = err
                    }
                }
            },
        )
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
                Text("${transaction.displayItemCount()} item", fontSize = 12.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    isLoadingDetail: Boolean = false,
    onDismiss: () -> Unit,
    onRequestReturn: (() -> Unit)? = null,
) {
    val canReturn = onRequestReturn != null &&
        transaction.items.isNotEmpty() &&
        transaction.id.toIntOrNull() != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(transaction.transactionNumber) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isLoadingDetail) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(32.dp))
                    }
                } else {
                    Text(formatDateTime(transaction.createdAt), fontSize = 12.sp, color = TextMuted)
                    Text("Kasir: ${transaction.cashierName}", fontSize = 13.sp)
                    HorizontalDivider()
                    if (transaction.items.isEmpty()) {
                        Text("Detail item tidak tersedia.", fontSize = 13.sp, color = TextMuted)
                    } else {
                        transaction.items.forEach { item ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.productName} (${item.qty})", modifier = Modifier.weight(1f), fontSize = 13.sp)
                                Text(formatIDR(item.subtotal), fontSize = 13.sp)
                            }
                        }
                    }
                    HorizontalDivider()
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
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canReturn) {
                    TextButton(onClick = onRequestReturn!!) { Text("Retur") }
                }
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        },
    )
}

@Composable
private fun ReturnFormDialog(
    transaction: Transaction,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, lines: List<Pair<TransactionItem, Int>>) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var qtys by remember(transaction.id) {
        mutableStateOf(List(transaction.items.size) { 0 })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retur ${transaction.transactionNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2,
                )
                transaction.items.forEachIndexed { index, item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${item.productName} (max ${item.qty})",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                        )
                        OutlinedTextField(
                            value = if (qtys[index] == 0) "" else qtys[index].toString(),
                            onValueChange = { v ->
                                val n = v.toIntOrNull() ?: 0
                                qtys = qtys.mapIndexed { i, q ->
                                    if (i == index) n.coerceIn(0, item.qty) else q
                                }
                            },
                            modifier = Modifier.width(72.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Qty") },
                        )
                    }
                }
                errorMessage?.let { Text(it, color = Error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lines = transaction.items.mapIndexedNotNull { i, item ->
                        val q = qtys[i]
                        if (q > 0) item to q else null
                    }
                    if (reason.isBlank() || lines.isEmpty()) return@Button
                    onSubmit(reason, lines)
                },
                enabled = reason.isNotBlank() && qtys.any { it > 0 },
            ) { Text("Kirim retur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
