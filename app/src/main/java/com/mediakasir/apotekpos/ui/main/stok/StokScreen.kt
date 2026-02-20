package com.mediakasir.apotekpos.ui.main.stok

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediakasir.apotekpos.data.model.*
import com.mediakasir.apotekpos.ui.theme.*
import com.mediakasir.apotekpos.utils.formatDate
import com.mediakasir.apotekpos.utils.formatIDR

val PRODUCT_UNITS = listOf("tablet", "kapsul", "strip", "botol", "sachet", "tube", "buah", "vial")
val PRODUCT_CATEGORIES = listOf("Analgesik", "Antibiotik", "Vitamin", "Lambung", "Antihistamin", "Batuk & Flu", "Antiseptik", "Diabetes", "Jantung", "Umum")

@Composable
fun StokScreen(
    license: LicenseInfo?,
    viewModel: StokViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val batches by viewModel.batches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()

    var search by remember { mutableStateOf("") }
    var showProductDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var editingBatch by remember { mutableStateOf<Batch?>(null) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(license?.branchId) {
        license?.branchId?.let { viewModel.loadProducts(it) }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(success) {
        success?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showProductDialog = true
                },
                containerColor = Primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Produk", tint = Color.White)
            }
        }
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
                Column {
                    Text("Manajemen Stok", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = search,
                        onValueChange = {
                            search = it
                            license?.branchId?.let { bid -> viewModel.loadProducts(bid, it) }
                        },
                        placeholder = { Text("Cari produk...", color = Color.White.copy(0.7f)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(0.5f),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (isLoading && products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products) { product ->
                        ProductListItem(
                            product = product,
                            onEdit = {
                                editingProduct = product
                                showProductDialog = true
                            },
                            onDelete = {
                                license?.branchId?.let { bid ->
                                    viewModel.deleteProduct(product.id, bid, search)
                                }
                            },
                            onBatch = {
                                selectedProduct = product
                                editingBatch = null
                                license?.branchId?.let { bid ->
                                    viewModel.loadBatches(bid, product.id)
                                }
                                showBatchDialog = true
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Product Dialog
    if (showProductDialog) {
        ProductDialog(
            product = editingProduct,
            branchId = license?.branchId ?: "",
            onSave = { data ->
                if (editingProduct != null) {
                    viewModel.updateProduct(editingProduct!!.id, data) {
                        showProductDialog = false
                        license?.branchId?.let { bid -> viewModel.loadProducts(bid, search) }
                    }
                } else {
                    viewModel.createProduct(data) {
                        showProductDialog = false
                        license?.branchId?.let { bid -> viewModel.loadProducts(bid, search) }
                    }
                }
            },
            onDismiss = { showProductDialog = false }
        )
    }

    // Batch Dialog
    if (showBatchDialog && selectedProduct != null) {
        BatchDialog(
            product = selectedProduct!!,
            batches = batches,
            editingBatch = editingBatch,
            branchId = license?.branchId ?: "",
            onSaveBatch = { data ->
                if (editingBatch != null) {
                    viewModel.updateBatch(
                        editingBatch!!.id, data,
                        license?.branchId ?: "",
                        selectedProduct!!.id
                    ) { editingBatch = null }
                } else {
                    viewModel.createBatch(
                        data,
                        license?.branchId ?: "",
                        selectedProduct!!.id
                    ) { editingBatch = null }
                }
            },
            onEditBatch = { editingBatch = it },
            onDeleteBatch = { batchId ->
                viewModel.deleteBatch(batchId, license?.branchId ?: "", selectedProduct!!.id)
            },
            onDismiss = { showBatchDialog = false }
        )
    }
}

@Composable
fun ProductListItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBatch: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val catColor = getCategoryColor(product.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = catColor.copy(0.15f)) {
                        Text(
                            product.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = catColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (product.currentStock <= product.minStock) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Warning.copy(0.15f)) {
                            Text(
                                "Stok Rendah",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Warning,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${product.unit} | ${formatIDR(product.sellPrice)}", fontSize = 13.sp, color = TextSecondary)
                Text("Stok: ${product.currentStock} | Min: ${product.minStock}", fontSize = 13.sp, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onBatch) {
                    Icon(Icons.Filled.Inventory2, contentDescription = "Batch", tint = Info)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Primary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = Error)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Produk") },
            text = { Text("Hapus \"${product.name}\"?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Hapus", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    product: Product?,
    branchId: String,
    onSave: (ProductCreate) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Umum") }
    var unit by remember { mutableStateOf(product?.unit ?: "tablet") }
    var sellPrice by remember { mutableStateOf(product?.sellPrice?.toString() ?: "") }
    var buyPrice by remember { mutableStateOf(product?.buyPrice?.toString() ?: "") }
    var minStock by remember { mutableStateOf(product?.minStock?.toString() ?: "10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product != null) "Edit Produk" else "Tambah Produk") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Produk *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = sellPrice, onValueChange = { sellPrice = it }, label = { Text("Harga Jual *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = it }, label = { Text("Harga Beli") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = minStock, onValueChange = { minStock = it }, label = { Text("Stok Minimum") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                Text("Satuan", fontSize = 12.sp, color = TextSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PRODUCT_UNITS.take(4).forEach { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u, fontSize = 11.sp) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PRODUCT_UNITS.drop(4).forEach { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u, fontSize = 11.sp) })
                    }
                }

                Text("Kategori", fontSize = 12.sp, color = TextSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PRODUCT_CATEGORIES.take(5).forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 11.sp) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PRODUCT_CATEGORIES.drop(5).forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 11.sp) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && sellPrice.isNotBlank()) {
                    onSave(
                        ProductCreate(
                            name = name.trim(),
                            barcode = barcode.trim(),
                            category = category,
                            unit = unit,
                            sellPrice = sellPrice.toDoubleOrNull() ?: 0.0,
                            buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                            minStock = minStock.toIntOrNull() ?: 10,
                            branchId = branchId
                        )
                    )
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun BatchDialog(
    product: Product,
    batches: List<Batch>,
    editingBatch: Batch?,
    branchId: String,
    onSaveBatch: (BatchCreate) -> Unit,
    onEditBatch: (Batch) -> Unit,
    onDeleteBatch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var batchNumber by remember(editingBatch) { mutableStateOf(editingBatch?.batchNumber ?: "") }
    var expiryDate by remember(editingBatch) { mutableStateOf(editingBatch?.expiryDate ?: "") }
    var qty by remember(editingBatch) { mutableStateOf(editingBatch?.currentQty?.toString() ?: "") }
    var buyPrice by remember(editingBatch) { mutableStateOf(editingBatch?.buyPrice?.toString() ?: product.buyPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batch Stok: ${product.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Existing batches
                if (batches.isNotEmpty()) {
                    Text("Daftar Batch", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    batches.forEach { batch ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    batch.isExpired -> Error.copy(0.1f)
                                    batch.isExpiringSoon -> Warning.copy(0.1f)
                                    else -> Subtle
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(batch.batchNumber, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Exp: ${formatDate(batch.expiryDate)} | Qty: ${batch.currentQty}", fontSize = 12.sp, color = TextSecondary)
                                    if (batch.isExpired) Text("⚠ Kadaluarsa", fontSize = 11.sp, color = Error)
                                    else if (batch.isExpiringSoon) Text("⚠ Mendekati Kadaluarsa", fontSize = 11.sp, color = Warning)
                                }
                                Row {
                                    IconButton(onClick = { onEditBatch(batch) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { onDeleteBatch(batch.id) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    Divider()
                }

                // Add/Edit form
                Text(if (editingBatch != null) "Edit Batch" else "Tambah Batch Baru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(value = batchNumber, onValueChange = { batchNumber = it }, label = { Text("No. Batch *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = expiryDate, onValueChange = { expiryDate = it }, label = { Text("Tgl. Kadaluarsa (YYYY-MM-DD) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("2025-12-31") })
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Jumlah *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = it }, label = { Text("Harga Beli") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                Button(
                    onClick = {
                        if (batchNumber.isNotBlank() && expiryDate.isNotBlank() && qty.isNotBlank()) {
                            onSaveBatch(
                                BatchCreate(
                                    productId = product.id,
                                    batchNumber = batchNumber.trim(),
                                    expiryDate = expiryDate.trim(),
                                    currentQty = qty.toIntOrNull() ?: 0,
                                    buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                                    branchId = branchId
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingBatch != null) "Update Batch" else "Simpan Batch")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}
