package com.mediakasir.apotekpos.ui.main.pos

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.ShoppingCart
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
    val discounts by viewModel.discounts.collectAsState()
    val promotions by viewModel.promotions.collectAsState()
    val selectedDiscountLabel by viewModel.selectedDiscountLabel.collectAsState()
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
    val alertCount by viewModel.alertCount.collectAsState()
    val netOk by viewModel.isNetworkConnected.collectAsState()
    val posKasirCatalogBlocked by viewModel.posKasirCatalogBlocked.collectAsState()
    val posKasirAccessDialogText by viewModel.posKasirAccessDialogText.collectAsState()

    var search by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf("Semua") }
    var showCart by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, activeCategory) {
        if (activeCategory == "Semua") products
        else products.filter { it.category.equals(activeCategory, ignoreCase = true) }
    }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    val branchId = remember(license, user) { effectiveBranchId(license, user) }
    val branchName = remember(license, user) {
        user?.let { effectiveBranchName(license, it) }.orEmpty()
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
                    /* ══════════════════════════════════════════════════════════ */
                    /*  TOP BAR — App name + search + user info                 */
                    /* ══════════════════════════════════════════════════════════ */
                    Surface(
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // App branding
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(com.mediakasir.apotekpos.R.drawable.logo_apoapps_panjang),
                                contentDescription = "ApotekPOS Logo",
                                modifier = Modifier
                                    .height(32.dp)
                                    .align(Alignment.CenterVertically)
                            )

                            Spacer(Modifier.width(16.dp))

                            // Search bar
                            OutlinedTextField(
                                value = search,
                                onValueChange = {
                                    search = it
                                    if (user != null && shiftGateResolved && !shiftBlocking && !posKasirCatalogBlocked) {
                                        viewModel.loadProducts(branchId, it)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "Cari produk, obat...",
                                        color = TextMuted,
                                        fontSize = 14.sp,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF3F4F6),
                                    unfocusedContainerColor = Color(0xFFF3F4F6),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = ApoPrimary,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            )

                            Spacer(Modifier.width(12.dp))

                            // Scan + Cart (phone only) + Status
                            Row(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Status badges
                                if (!netOk) {
                                    Surface(
                                        color = Warning.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(20.dp),
                                    ) {
                                        Text(
                                            "Offline",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = Warning,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                if (pendingOut > 0) {
                                    Surface(
                                        color = Info.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(20.dp),
                                    ) {
                                        Text(
                                            "$pendingOut antre",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = Info,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }

                                BadgedBox(
                                    badge = {
                                        if (alertCount > 0) {
                                            Badge(containerColor = Error, contentColor = Color.White) {
                                                Text(alertCount.toString(), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    },
                                ) {
                                    IconButton(onClick = { toastMsg = "Ada $alertCount item alat/obat yang perlu diperhatikan (kadaluarsa/stok tipis)." }) {
                                        Icon(Icons.Filled.Notifications, contentDescription = "Notifikasi", tint = TextPrimary)
                                    }
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

                            // User info
                            if (user != null) {
                                Spacer(Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = ApoPrimary,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                user.name.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }
                                    if (wide) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            user.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    /* ══════════════════════════════════════════════════════════ */
                    /*  MAIN CONTENT                                            */
                    /* ══════════════════════════════════════════════════════════ */

                    if (productsLoadError != null && products.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
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
                                CircularProgressIndicator(color = ApoPrimary)
                                Spacer(Modifier.height(12.dp))
                                Text("Memeriksa shift…", color = TextMuted, fontSize = 14.sp)
                            }
                        }
                        posKasirCatalogBlocked -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("POS hanya untuk akun kasir.", color = TextMuted, fontSize = 15.sp)
                        }
                        isLoading && products.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ApoPrimary)
                        }
                        products.isEmpty() && productsLoadError != null -> Column(
                            Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("Gagal memuat produk", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text(productsLoadError!!, color = TextSecondary, fontSize = 14.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadProducts(branchId, search) }) { Text("Coba lagi") }
                        }
                        products.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada produk ditemukan", color = TextMuted)
                        }

                        /* ─── WIDE LAYOUT (tablet) ─── */
                        wide -> Row(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            // Left: Cart items sidebar (wide, scrollable)
                            Surface(
                                modifier = Modifier
                                    .weight(0.22f)
                                    .fillMaxHeight(),
                                color = Color.White,
                                shadowElevation = 0.dp,
                                shape = RoundedCornerShape(0.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .padding(12.dp),
                                ) {
                                    Text(
                                        "Keranjang",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${viewModel.getCartCount()} Item",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                    )
                                    
                                    if (cart.isEmpty()) {
                                        Spacer(Modifier.height(16.dp))
                                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Outlined.ShoppingCart, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextMuted)
                                                Spacer(Modifier.height(8.dp))
                                                Text("Keranjang kosong", color = TextMuted, fontSize = 13.sp)
                                            }
                                        }
                                    } else {
                                        Spacer(Modifier.height(12.dp))
                                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                                        
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Spacer(Modifier.height(4.dp))
                                            cart.forEach { item ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(item.product.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2)
                                                        Text(
                                                            formatIDR(item.product.sellPrice) + " ×" + item.qty,
                                                            fontSize = 10.sp,
                                                            color = TextSecondary,
                                                        )
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    ) {
                                                        IconButton(
                                                            onClick = { viewModel.updateQty(item.product.id, -1) },
                                                            modifier = Modifier.size(24.dp),
                                                        ) {
                                                            Icon(Icons.Filled.Remove, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                                                        }
                                                        Text(item.qty.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        IconButton(
                                                            onClick = { viewModel.updateQty(item.product.id, 1) },
                                                            modifier = Modifier.size(24.dp),
                                                        ) {
                                                            Icon(Icons.Filled.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (cart.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                                        Spacer(Modifier.height(6.dp))
                                        TextButton(onClick = { viewModel.clearCart() }, modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Filled.Delete, contentDescription = null, tint = Error, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Kosongkan", color = Error, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // Center: product catalog
                            Column(
                                modifier = Modifier
                                    .weight(0.53f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    "Dispense Obat",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Pilih item untuk transaksi",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                )
                                Spacer(Modifier.height(12.dp))

                                // Filter tabs
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Semua", "Resep", "OTC").forEach { label ->
                                        if (activeCategory == label) {
                                            Button(
                                                onClick = { activeCategory = label },
                                                colors = ButtonDefaults.buttonColors(containerColor = ApoPrimary),
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                            ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                                        } else {
                                            OutlinedButton(
                                                onClick = { activeCategory = label },
                                                shape = RoundedCornerShape(20.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                            ) { Text(label, fontSize = 13.sp, color = TextPrimary) }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Product grid
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(gridCols),
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(filteredProducts) { product ->
                                        ProductCard(
                                            product = product,
                                            onClick = {
                                                val err = viewModel.addToCart(product)
                                                if (err != null) toastMsg = err
                                            },
                                        )
                                    }
                                }
                            }

                            // Right: Payment panel with quick buttons
                            Surface(
                                modifier = Modifier
                                    .weight(0.25f)
                                    .fillMaxHeight(),
                                color = Color(0xFFF9FAFB),
                                shadowElevation = 0.dp,
                                shape = RoundedCornerShape(0.dp),
                            ) {
                                PaymentPanel(
                                    cart = cart,
                                    payments = payments,
                                    discountLabel = selectedDiscountLabel,
                                    viewModel = viewModel,
                                    onCheckout = { doCheckout() },
                                    isProcessing = isProcessing,
                                )
                            }
                        }

                        /* ─── PHONE LAYOUT ─── */
                        else -> Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                "Dispense Obat",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Pilih item untuk transaksi",
                                fontSize = 12.sp,
                                color = TextSecondary,
                            )
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Semua", "Resep", "OTC").forEach { label ->
                                    if (activeCategory == label) {
                                        Button(
                                            onClick = { activeCategory = label },
                                            colors = ButtonDefaults.buttonColors(containerColor = ApoPrimary),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                    } else {
                                        OutlinedButton(
                                            onClick = { activeCategory = label },
                                            shape = RoundedCornerShape(20.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        ) { Text(label, fontSize = 12.sp, color = TextPrimary) }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridCols),
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(filteredProducts) { product ->
                                    ProductCard(
                                        product = product,
                                        onClick = {
                                            val err = viewModel.addToCart(product)
                                            if (err != null) toastMsg = err
                                        },
                                    )
                                }
                            }
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
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
) {
    val catColor = getCategoryColor(product.category)
    val isOutOfStock = product.currentStock <= 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock) { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOutOfStock) 0.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = if (isOutOfStock) Color(0xFFF1F5F9) else Color.White),
        border = if (isOutOfStock) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            // Top row: icon + category badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Product icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ApoPrimary.copy(alpha = 0.10f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Science,
                            contentDescription = null,
                            tint = ApoPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isOutOfStock) Error.copy(alpha = 0.12f) else catColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        if (isOutOfStock) "HABIS" else product.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOutOfStock) Error else catColor,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Offer chips (API-driven, outlined for cleaner UI)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                product.discountLabel?.takeIf { it.isNotBlank() }?.let { label ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.7f)),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Success,
                            maxLines = 1,
                        )
                    }
                }
                product.promoLabel?.takeIf { it.isNotBlank() }?.let { label ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApoPrimary.copy(alpha = 0.7f)),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApoPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (!product.discountLabel.isNullOrBlank() || !product.promoLabel.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
            }

            // Product name
            Text(
                product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOutOfStock) TextMuted else TextPrimary,
                maxLines = 1,
            )
            // Description
            Text(
                "${product.category} • ${product.unit}",
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
            )

            Spacer(Modifier.height(12.dp))

            // Bottom row: price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatIDR(product.sellPrice),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOutOfStock) TextMuted else Color.Black,
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

