package com.mediakasir.apotekpos.ui.main.pos

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.Product
import com.mediakasir.apotekpos.data.model.Transaction
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.ui.effectiveBranchId
import com.mediakasir.apotekpos.ui.effectiveBranchName
import com.mediakasir.apotekpos.ui.theme.*
import com.mediakasir.apotekpos.utils.formatDateTime
import com.mediakasir.apotekpos.utils.formatDigitsAsIndonesianNumber
import com.mediakasir.apotekpos.utils.formatIDR

@OptIn(ExperimentalMaterial3Api::class)
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
    val productsLoadError by viewModel.productsLoadError.collectAsState()
    val shiftGateResolved by viewModel.shiftGateResolved.collectAsState()
    val shiftBlocking by viewModel.shiftBlocking.collectAsState()
    val startingShift by viewModel.startingShift.collectAsState()
    val shiftDialogError by viewModel.shiftDialogError.collectAsState()
    val pendingOut by viewModel.pendingSyncCount.collectAsState()
    val netOk by viewModel.isNetworkConnected.collectAsState()
    val posKasirCatalogBlocked by viewModel.posKasirCatalogBlocked.collectAsState()
    val posKasirAccessDialogText by viewModel.posKasirAccessDialogText.collectAsState()

    var search by remember { mutableStateOf("") }
    var showCart by remember { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    val branchId = remember(license, user) { effectiveBranchId(license, user) }
    val branchName = remember(license, user) {
        user?.let { effectiveBranchName(license, it) }.orEmpty()
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result?.contents ?: return@rememberLauncherForActivityResult
        search = text
        if (user != null && shiftGateResolved && !shiftBlocking && !posKasirCatalogBlocked) {
            viewModel.loadProducts(branchId, text)
        }
    }

    LaunchedEffect(user?.userId, branchId) {
        if (user != null) {
            viewModel.checkActiveShift(userRole = user.role)
        }
    }

    LaunchedEffect(user?.userId, branchId, shiftGateResolved, shiftBlocking, posKasirCatalogBlocked, search) {
        if (user == null || !shiftGateResolved || shiftBlocking || posKasirCatalogBlocked) return@LaunchedEffect
        viewModel.loadProducts(branchId, search)
    }

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

    fun doCheckout() {
        user?.let { u ->
            viewModel.checkout(
                branchId,
                effectiveBranchName(license, u),
                u.userId,
                u.name,
            )
            showCart = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background),
        ) {
            val wide = maxWidth >= 900.dp
            val gridCols = when {
                maxWidth >= 1200.dp -> 4
                maxWidth >= 650.dp -> 3
                else -> 2
            }
            Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Surface(
                    color = SurfaceColor,
                    tonalElevation = 1.dp,
                    shadowElevation = 2.dp,
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = search,
                                onValueChange = {
                                    search = it
                                    if (user != null && shiftGateResolved && !shiftBlocking && !posKasirCatalogBlocked) {
                                        viewModel.loadProducts(branchId, it)
                                    }
                                },
                                placeholder = { Text("Cari produk, SKU, atau barcode...", color = TextMuted) },
                                trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = PosWebPrimary,
                                    unfocusedBorderColor = InputBorder,
                                    cursorColor = PosWebPrimaryDark,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            if (branchName.isNotBlank()) {
                                Column(horizontalAlignment = Alignment.End) {
                                    val parts = branchName.split("(", limit = 2)
                                    Text(
                                        parts[0].trim(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                    )
                                    if (parts.size > 1) {
                                        Text(
                                            "(${parts[1]}",
                                            fontSize = 13.sp,
                                            color = TextPrimary,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { scanLauncher.launch(ScanOptions()) }) {
                                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Pindai", tint = TextPrimary)
                                }
                                if (!wide) {
                                    BadgedBox(
                                        badge = {
                                            if (viewModel.getCartCount() > 0) {
                                                Badge(containerColor = Error, contentColor = Color.White) {
                                                    Text(viewModel.getCartCount().toString(), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                    ) {
                                        IconButton(onClick = { showCart = true }) {
                                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Keranjang", tint = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Status offline / pending indicator dipinggirkan ke bawah search jika butuh
                        if (!netOk || pendingOut > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!netOk) {
                                    Surface(color = Warning.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                                        Text("Offline", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Warning, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (pendingOut > 0) {
                                    Surface(color = Info.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                                        Text("$pendingOut antre kirim", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Info, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(listOf("Promo" to true, "Generic" to false, "Prescription" to false, "Supplements" to false, "Baby Care" to false)) { (btn, isSelected) ->
                                if (isSelected) {
                                    Button(
                                        onClick = { },
                                        colors = ButtonDefaults.buttonColors(containerColor = PosWebPrimary),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) { Text(btn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                } else {
                                    OutlinedButton(
                                        onClick = { },
                                        shape = RoundedCornerShape(20.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) { Text(btn, fontSize = 12.sp, color = TextPrimary) }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (productsLoadError != null && products.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Warning.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                productsLoadError!!,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = TextPrimary,
                            )
                            TextButton(onClick = { viewModel.loadProducts(branchId, search) }) {
                                Text("Muat ulang", color = PosWebPrimaryDark)
                            }
                        }
                    }
                }

                when {
                    user != null && !shiftGateResolved -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PosWebPrimary)
                            Spacer(Modifier.height(12.dp))
                            Text("Memeriksa shift…", color = TextMuted, fontSize = 14.sp)
                        }
                    }
                    posKasirCatalogBlocked -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "POS hanya untuk akun kasir.",
                            color = TextMuted,
                            fontSize = 15.sp,
                        )
                    }
                    isLoading && products.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PosWebPrimary)
                    }
                    products.isEmpty() && productsLoadError != null -> Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Gagal memuat produk", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(productsLoadError!!, color = TextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProducts(branchId, search) }) {
                            Text("Coba lagi")
                        }
                    }
                    products.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada produk ditemukan", color = TextMuted)
                    }
                    wide -> Row(Modifier.weight(1f).fillMaxWidth()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridCols),
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(products) { product ->
                                ProductCard(product) {
                                    val err = viewModel.addToCart(product)
                                    if (err != null) toastMsg = err
                                }
                            }
                        }
                        VerticalDivider(Modifier.fillMaxHeight())
                        Surface(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight(),
                            tonalElevation = 1.dp,
                            color = SurfaceColor,
                        ) {
                            CartPanelContent(
                                cart = cart,
                                payments = payments,
                                discount = discount,
                                viewModel = viewModel,
                                onCheckout = { doCheckout() },
                            )
                        }
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(gridCols),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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

            if (!wide && showCart) {
                CartBottomSheet(
                    cart = cart,
                    payments = payments,
                    discount = discount,
                    viewModel = viewModel,
                    onDismiss = { showCart = false },
                    onCheckout = { doCheckout() },
                )
            }
            if (user != null && shiftGateResolved && shiftBlocking) {
                OpenShiftGateDialog(
                    isLoading = startingShift,
                    errorMessage = shiftDialogError,
                    onClearError = { viewModel.clearShiftDialogError() },
                    onSubmit = { viewModel.submitStartingShift(it) },
                )
            }
            }
        }
    }

    posKasirAccessDialogText?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPosKasirAccessDialog() },
            title = { Text("Akses ditolak", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissPosKasirAccessDialog() }) {
                    Text("Mengerti")
                }
            },
        )
    }

    // Receipt Dialog
    receipt?.let { trx ->
        ReceiptDialog(
            transaction = trx,
            pharmacyName = license?.pharmacyName?.takeIf { it.isNotBlank() } ?: user?.partnerName.orEmpty(),
            address = license?.address ?: "",
            phone = license?.phone ?: "",
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}

@Composable
private fun OpenShiftGateDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var digits by remember { mutableStateOf("") }
    val display = formatDigitsAsIndonesianNumber(digits)
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 440.dp)
                    .fillMaxWidth(0.92f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Buka shift", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Masukkan modal awal kas sebelum bertransaksi.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(16.dp))
                    if (!errorMessage.isNullOrBlank()) {
                        Text(errorMessage, color = Error, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = display,
                        onValueChange = { raw ->
                            onClearError()
                            digits = raw.filter { it.isDigit() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Modal awal (Rp)") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !isLoading,
                        isError = !errorMessage.isNullOrBlank(),
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { onSubmit(digits) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isLoading && digits.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PosWebPrimary),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Mulai shift", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    val catColor = getCategoryColor(product.category)
    val isOutOfStock = product.currentStock <= 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock) { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOutOfStock) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = if (isOutOfStock) Color(0xFFF1F5F9) else Color.White),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PosWebPrimary.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Science,
                        contentDescription = null,
                        tint = PosWebPrimaryDark,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            
            Text(
                "${product.name}\n${product.unit}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOutOfStock) TextMuted else TextPrimary,
                maxLines = 2,
                lineHeight = 16.sp,
                modifier = Modifier.height(34.dp) // Maintain consistent height for 2 lines
            )
            
            Spacer(Modifier.height(8.dp))
            Text(
                formatIDR(product.sellPrice),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isOutOfStock) TextMuted else Color.Black,
            )
            
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isOutOfStock) Error.copy(alpha = 0.12f) else catColor,
            ) {
                Text(
                    if (isOutOfStock) "HABIS" else product.category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOutOfStock) Error else Color.White,
                )
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
    onCheckout: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.9f),
    ) {
        CartPanelContent(
            cart = cart,
            payments = payments,
            discount = discount,
            viewModel = viewModel,
            onCheckout = onCheckout,
            showTopClose = true,
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun ReceiptDialog(
    transaction: Transaction,
    pharmacyName: String,
    address: String,
    phone: String,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
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
                    if (transaction.isPendingSync) {
                        Text(
                            "*** Menunggu sinkron ***",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Warning,
                        )
                    }
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val lines = buildString {
                                appendLine(pharmacyName)
                                appendLine(transaction.transactionNumber)
                                appendLine(formatDateTime(transaction.createdAt))
                                transaction.items.forEach { i ->
                                    appendLine("${i.productName} ${i.qty} × ${formatIDR(i.sellPrice)}")
                                }
                                appendLine("TOTAL ${formatIDR(transaction.totalAmount)}")
                            }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, lines)
                            }
                            ctx.startActivity(Intent.createChooser(send, "Bagikan struk"))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Bagikan")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}
