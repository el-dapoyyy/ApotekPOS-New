package com.mediakasir.apotekpos.ui.main.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.Product
import com.mediakasir.apotekpos.data.model.Transaction
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.ui.theme.*
import com.mediakasir.apotekpos.utils.formatDateTime
import com.mediakasir.apotekpos.utils.formatIDR

val PAYMENT_METHODS = listOf("Tunai", "Transfer", "QRIS", "Debit", "Kredit")

@Composable
fun POSScreen(
    license: LicenseInfo?,
    user: UserInfo?,
    viewModel: POSViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val discount by viewModel.discount.collectAsState()
    val isLoading by viewModel.isLoadingProducts.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val receipt by viewModel.receipt.collectAsState()
    val error by viewModel.error.collectAsState()

    var search by remember { mutableStateOf("") }
    var showCart by remember { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(license?.branchId) {
        license?.branchId?.let { viewModel.loadProducts(it) }
    }

    // Toast-like snackbar
    LaunchedEffect(error) {
        if (error != null) {
            toastMsg = error
            viewModel.clearError()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            snackbarHostState.showSnackbar(it)
            toastMsg = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kasir", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    BadgedBox(
                        badge = {
                            if (viewModel.getCartCount() > 0) {
                                Badge { Text(viewModel.getCartCount().toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = { showCart = true }) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Keranjang", tint = Color.White)
                        }
                    }
                }
            }

            // Search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                        license?.branchId?.let { bid -> viewModel.loadProducts(bid, it) }
                    },
                    placeholder = { Text("Cari produk atau barcode...", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Product Grid
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada produk ditemukan", color = TextMuted)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products) { product ->
                        ProductCard(product) {
                            val err = viewModel.addToCart(product)
                            if (err != null) toastMsg = err
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Cart Sheet
    if (showCart) {
        CartBottomSheet(
            cart = cart,
            payments = payments,
            discount = discount,
            viewModel = viewModel,
            onDismiss = { showCart = false },
            onCheckout = {
                license?.let { lic ->
                    user?.let { u ->
                        viewModel.checkout(lic.branchId, lic.branchName, u.userId, u.fullName)
                        showCart = false
                    }
                }
            }
        )
    }

    // Receipt Dialog
    receipt?.let { trx ->
        ReceiptDialog(
            transaction = trx,
            pharmacyName = license?.pharmacyName ?: "",
            address = license?.address ?: "",
            phone = license?.phone ?: "",
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    val catColor = getCategoryColor(product.category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = product.currentStock > 0) { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = if (product.currentStock <= 0) Color(0xFFF8F8F8) else Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = catColor.copy(alpha = 0.15f)
            ) {
                Text(
                    product.category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = catColor
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                product.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (product.currentStock <= 0) TextMuted else TextPrimary,
                maxLines = 2
            )
            Text(product.unit, fontSize = 11.sp, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatIDR(product.sellPrice),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (product.currentStock <= 0) Color(0xFFFEF2F2) else Secondary
                ) {
                    Text(
                        if (product.currentStock <= 0) "Habis" else "Stok: ${product.currentStock}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = if (product.currentStock <= 0) Error else PrimaryDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cart: List<CartItem>,
    payments: List<PaymentEntry>,
    discount: String,
    viewModel: POSViewModel,
    onDismiss: () -> Unit,
    onCheckout: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val subtotal = viewModel.getSubtotal()
    val discountAmt = viewModel.getDiscountAmt()
    val total = viewModel.getTotal()
    val totalPaid = viewModel.getTotalPaid()
    val change = viewModel.getChange()
    val isProcessing by viewModel.isProcessing.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keranjang (${viewModel.getCartCount()})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup")
                }
            }
            Divider()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextMuted)
                            Text("Keranjang kosong", color = TextMuted, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                } else {
                    // Cart Items
                    cart.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "${formatIDR(item.product.sellPrice)} × ${item.qty} = ${formatIDR(item.product.sellPrice * item.qty)}",
                                    fontSize = 12.sp, color = TextSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.updateQty(item.product.id, -1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                }
                                Text(item.qty.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)
                                IconButton(
                                    onClick = { viewModel.updateQty(item.product.id, 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.removeFromCart(item.product.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Divider()
                    }

                    // Summary
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", color = TextSecondary)
                        Text(formatIDR(subtotal), fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Diskon", color = TextSecondary)
                        OutlinedTextField(
                            value = discount,
                            onValueChange = { viewModel.setDiscount(it) },
                            modifier = Modifier.width(120.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.End)
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Secondary
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(formatIDR(total), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryDark)
                        }
                    }

                    // Payment Methods
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Metode Pembayaran", fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.addPayment() }) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Tambah")
                        }
                    }

                    payments.forEach { payment ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Subtle)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Method chips
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PAYMENT_METHODS.forEach { method ->
                                        Surface(
                                            modifier = Modifier.clickable { viewModel.updatePayment(payment.id, method = method) },
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (payment.method == method) Primary else Surface,
                                            border = if (payment.method != method) androidx.compose.foundation.BorderStroke(1.dp, Border) else null
                                        ) {
                                            Text(
                                                method,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                fontSize = 12.sp,
                                                color = if (payment.method == method) Color.White else TextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = payment.amount,
                                        onValueChange = { viewModel.updatePayment(payment.id, amount = it) },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Jumlah") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    if (payments.size > 1) {
                                        IconButton(onClick = { viewModel.removePayment(payment.id) }) {
                                            Icon(Icons.Filled.Close, contentDescription = null, tint = Error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Change
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Dibayar", color = TextSecondary)
                        Text(
                            formatIDR(totalPaid),
                            color = if (totalPaid >= total) Success else Error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kembalian", color = TextSecondary)
                        Text(
                            formatIDR(maxOf(0.0, change)),
                            color = if (change >= 0) Success else Error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Checkout button
            if (cart.isNotEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onCheckout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isProcessing && totalPaid >= total,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            disabledContainerColor = TextMuted
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Proses Transaksi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    transaction: Transaction,
    pharmacyName: String,
    address: String,
    phone: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text("========== STRUK ==========", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text(pharmacyName, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text(address, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("Telp: $phone", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("----------------------------", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("No: ${transaction.transactionNumber}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("Tgl: ${formatDateTime(transaction.createdAt)}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("Kasir: ${transaction.cashierName}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("----------------------------", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    transaction.items.forEach { item ->
                        Text(item.productName, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                        Text("  ${item.qty} ${item.unit} × ${formatIDR(item.sellPrice)} = ${formatIDR(item.subtotal)}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    }
                    Text("----------------------------", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("Subtotal: ${formatIDR(transaction.subtotal)}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    if (transaction.discount > 0) Text("Diskon: ${formatIDR(transaction.discount)}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("TOTAL: ${formatIDR(transaction.totalAmount)}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("----------------------------", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    transaction.paymentDetails.forEach { p ->
                        Text("${p.method.uppercase()}: ${formatIDR(p.amount)}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    }
                    Text("Kembalian: ${formatIDR(transaction.change)}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("============================", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text("Terima Kasih!", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Bluetooth print */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cetak Struk")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}
