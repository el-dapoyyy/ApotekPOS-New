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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background),
        ) {
            Surface(
                color = Primary,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
                ) {
                    Text(
                        "Cek stok",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Hanya lihat — ubah data lewat ApoApps web",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = search,
                        onValueChange = {
                            search = it
                            if (user != null) {
                                viewModel.loadProducts(branchId, it)
                            }
                        },
                        placeholder = { Text("Cari nama / barcode...", color = Color.White.copy(0.7f)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color.White,
                        ),
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
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
    val catColor = getCategoryColor(product.category)
    val isLowStock = product.currentStock <= product.minStock

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = catColor.copy(alpha = 0.15f)) {
                        Text(
                            product.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            color = catColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (isLowStock) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Warning.copy(alpha = 0.15f)) {
                            Text(
                                "Stok rendah",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                color = Warning,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text(
                    "SKU ${product.sku} • ${product.unit} • ${formatIDR(product.sellPrice)}",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
                Text(
                    "Stok ${product.currentStock} PCS • Min ${product.minStock} PCS",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
            }
            IconButton(onClick = onBatches) {
                Icon(Icons.Filled.Inventory2, contentDescription = "Lihat batch", tint = Info)
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
