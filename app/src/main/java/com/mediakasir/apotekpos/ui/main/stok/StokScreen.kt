package com.mediakasir.apotekpos.ui.main.stok

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediakasir.apotekpos.data.model.Batch
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.Product
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.ui.effectiveBranchId
import com.mediakasir.apotekpos.ui.theme.Background
import com.mediakasir.apotekpos.ui.theme.Error
import com.mediakasir.apotekpos.ui.theme.Info
import com.mediakasir.apotekpos.ui.theme.Primary
import com.mediakasir.apotekpos.ui.theme.PrimaryDark
import com.mediakasir.apotekpos.ui.theme.Secondary
import com.mediakasir.apotekpos.ui.theme.TextMuted
import com.mediakasir.apotekpos.ui.theme.TextPrimary
import com.mediakasir.apotekpos.ui.theme.TextSecondary
import com.mediakasir.apotekpos.ui.theme.Warning
import com.mediakasir.apotekpos.ui.theme.getCategoryColor
import com.mediakasir.apotekpos.utils.formatDate
import com.mediakasir.apotekpos.utils.formatIDR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokScreen(
    license: LicenseInfo?,
    user: UserInfo?,
    viewModel: StokViewModel = hiltViewModel(),
) {
    val products by viewModel.products.collectAsState()
    val batches by viewModel.batches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val productsLoadError by viewModel.productsLoadError.collectAsState()

    var search by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val snackbarHostState = remember { SnackbarHostState() }

    val branchId = remember(license, user) { effectiveBranchId(license, user) }

    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (!isLoading) isRefreshing = false
    }

    LaunchedEffect(user?.userId, branchId) {
        if (user != null) {
            viewModel.loadProducts(branchId)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadProducts(branchId)
            },
            modifier = Modifier.fillMaxSize(),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Primary),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Stok",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user?.name ?: "Kasir",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Kelola stok obat dengan mudah",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                        if (user != null) {
                            viewModel.loadProducts(branchId, it)
                        }
                    },
                    placeholder = { Text("Cari nama obat / barcode...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        Row(modifier = Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Mic, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color.Black,
                    ),
                    shape = CircleShape,
                )
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.FilterList, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Filter", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)) {
                            Text("Jenis Obat", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp)
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)) {
                            Text("Status Stok", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp)
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)) {
                            Text("Urutkan", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp)
                        }
                    }

                    if (productsLoadError != null && products.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            Text("Muat ulang", color = PrimaryDark)
                        }
                    }
                }
            }

            when {
                isLoading && products.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = Primary)
                }
                products.isEmpty() && productsLoadError != null -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Gagal memuat persediaan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(productsLoadError!!, color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadProducts(branchId, search) }) {
                        Text("Coba lagi")
                    }
                }
                products.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada produk di cache untuk cabang ini.", color = TextMuted)
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(products) { product ->
                        StokReadOnlyRow(
                            product = product,
                            onBatches = {
                                selectedProduct = product
                                if (user != null) {
                                    viewModel.loadBatches(branchId, product.id)
                                }
                            },
                        )
                    }
                }
            }
                }
            }
        }
        } // PullToRefreshBox
    }

    if (selectedProduct != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    selectedProduct!!.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text("Batch (read-only)", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                if (batches.isEmpty()) {
                    Text("Memuat atau tidak ada batch…", color = TextMuted, fontSize = 14.sp)
                } else {
                    batches.forEach { batch ->
                        BatchReadOnlyCard(batch)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StokReadOnlyRow(
    product: Product,
    onBatches: () -> Unit,
) {
    val isOutOfStock = product.currentStock <= 0
    val isLowStock = !isOutOfStock && product.currentStock <= product.minStock
    val progress = if (product.minStock > 0) {
        (product.currentStock.toFloat() / (product.minStock * 2).toFloat()).coerceIn(0f, 1f)
    } else {
        if (product.currentStock > 0) 1f else 0f
    }
    val progressPercent = (progress * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha=0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOutOfStock) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Error.copy(alpha = 0.13f)) {
                        Text(
                            "Habis",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            color = Error,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                } else if (isLowStock) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Warning.copy(alpha = 0.15f)) {
                        Text(
                            "Stok Menipis",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            color = Warning,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onBatches, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = "Lihat batch", tint = TextSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SKU/${product.sku} - ${product.currentStock}", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("${product.unit} • ${formatIDR(product.sellPrice)} || ${formatIDR(product.sellPrice)}", fontSize = 13.sp, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Stok: ${product.currentStock} pcs", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Primary.copy(alpha=0.2f),
                            modifier = Modifier.width(60.dp).height(6.dp)
                        ) {
                             Surface(
                                 shape = RoundedCornerShape(4.dp),
                                 color = when {
                                     isOutOfStock -> Error
                                     isLowStock -> Warning
                                     else -> Primary
                                 },
                                 modifier = Modifier.fillMaxWidth(progress).height(6.dp)
                             ) {}
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("$progressPercent%", fontSize=12.sp, fontWeight=FontWeight.Bold, color=TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Surface(shape=RoundedCornerShape(8.dp), color=when {
                            isOutOfStock -> Error
                            isLowStock -> Warning
                            else -> Primary
                        }) {
                           Text("$progressPercent%", modifier=Modifier.padding(horizontal=8.dp, vertical=4.dp), color=Color.White, fontSize=12.sp, fontWeight=FontWeight.Bold)
                        } 
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchReadOnlyCard(batch: Batch) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                batch.isExpired -> Error.copy(alpha = 0.08f)
                batch.isExpiringSoon -> Warning.copy(alpha = 0.08f)
                else -> Secondary
            },
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(batch.batchNumber, fontWeight = FontWeight.SemiBold)
            Text(
                "Exp: ${formatDate(batch.expiryDate)} | Qty: ${batch.currentQty}",
                fontSize = 13.sp,
                color = TextSecondary,
            )
            if (batch.isExpired) {
                Text("Kadaluarsa", fontSize = 12.sp, color = Error)
            } else if (batch.isExpiringSoon) {
                Text("Mendekati kadaluarsa", fontSize = 12.sp, color = Warning)
            }
        }
    }
}
