package com.mediakasir.apotekpos.ui.main.pos

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.mediakasir.apotekpos.data.local.LocalCashExpenseEntity
import com.mediakasir.apotekpos.data.model.Product
import com.mediakasir.apotekpos.data.model.Transaction
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.ui.components.NetworkStatusDot
import com.mediakasir.apotekpos.ui.effectiveBranchId
import com.mediakasir.apotekpos.ui.effectiveBranchName
import com.mediakasir.apotekpos.ui.theme.*
import com.mediakasir.apotekpos.util.ThermalPrinterManager
import com.mediakasir.apotekpos.utils.formatDateTime
import com.mediakasir.apotekpos.utils.formatDigitsAsIndonesianNumber
import com.mediakasir.apotekpos.utils.formatIDR
import com.mediakasir.apotekpos.utils.parseMoneyInputToDouble
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class CategoryTab(val label: String, val match: (String) -> Boolean)

private val defaultCategoryTabs = listOf(
    CategoryTab("Semua") { true },
    CategoryTab("Resep") { c ->
        val l = c.lowercase()
        l.contains("keras") || l.contains("psiko") || l.contains("narkotika")
    },
    CategoryTab("OTC") { c ->
        val l = c.lowercase()
        (l.contains("bebas") && !l.contains("terbatas")) || l.contains("otc")
    },
    CategoryTab("OBT") { c -> c.lowercase().contains("terbatas") },
    CategoryTab("Alkes") { c ->
        val l = c.lowercase()
        l.contains("alat") || l.contains("alkes")
    },
    CategoryTab("Suplemen") { c ->
        val l = c.lowercase()
        l.contains("suplemen") || l.contains("vitamin")
    },
)

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
    val selectedDiscountId by viewModel.selectedDiscountId.collectAsState()
    val isLoading by viewModel.isLoadingProducts.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val receipt by viewModel.receipt.collectAsState()
    val error by viewModel.error.collectAsState()
    val productsLoadError by viewModel.productsLoadError.collectAsState()
    val shiftGateResolved by viewModel.shiftGateResolved.collectAsState()
    val shiftBlocking by viewModel.shiftBlocking.collectAsState()
    val startingShift by viewModel.startingShift.collectAsState()
    val shiftDialogError by viewModel.shiftDialogError.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val showCloseShiftDialog by viewModel.showCloseShiftDialog.collectAsState()
    val closingShift by viewModel.closingShift.collectAsState()
    val shiftSummary by viewModel.shiftSummary.collectAsState()
    val shiftExpiredWarning by viewModel.shiftExpiredWarning.collectAsState()
    val shiftSoftAlert by viewModel.shiftSoftAlert.collectAsState()
    val shiftHardLock by viewModel.shiftHardLock.collectAsState()
    val pendingOut by viewModel.pendingSyncCount.collectAsState()
    val syncErrors by viewModel.syncErrors.collectAsState()
    val showSyncErrorDialog by viewModel.showSyncErrorDialog.collectAsState()
    val isRetryingSync by viewModel.isRetryingSync.collectAsState()
    val syncRetryMessage by viewModel.syncRetryMessage.collectAsState()
    val alertCount by viewModel.alertCount.collectAsState()
    val netOk by viewModel.isNetworkConnected.collectAsState()
    val posKasirCatalogBlocked by viewModel.posKasirCatalogBlocked.collectAsState()
    val posKasirAccessDialogText by viewModel.posKasirAccessDialogText.collectAsState()
    val compoundRecipes by viewModel.compoundRecipes.collectAsState()
    val isLoadingRacikan by viewModel.isLoadingRacikan.collectAsState()
    val racikanError by viewModel.racikanError.collectAsState()
    val showRacikanDialog by viewModel.showRacikanDialog.collectAsState()
    val cashOutHistory by viewModel.cashOutHistory.collectAsState()
    val cashOutTotal by viewModel.cashOutTotal.collectAsState()
    val cashOutLoading by viewModel.cashOutLoading.collectAsState()

    var search by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf("Semua") }
    var showCart by remember { mutableStateOf(false) }
    var showCashOutDialog by remember { mutableStateOf(false) }
    var showCashOutHistoryDialog by remember { mutableStateOf(false) }
    var cashOutPhotoUri by remember { mutableStateOf<String?>(null) }
    var editingCashOut by remember { mutableStateOf<LocalCashExpenseEntity?>(null) }
    var deleteCashOutTarget by remember { mutableStateOf<LocalCashExpenseEntity?>(null) }
    var showNetworkStatusDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    val categoryTabs = defaultCategoryTabs
    val filteredProducts = remember(products, activeCategory) {
        val tab = categoryTabs.find { it.label == activeCategory }
        if (tab == null || activeCategory == "Semua") products
        else products.filter { tab.match(it.category) }
    }
    val categoryCountMap = remember(products) {
        categoryTabs.associate { tab ->
            tab.label to if (tab.label == "Semua") products.size else products.count { tab.match(it.category) }
        }
    }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    val branchId = remember(license, user) { effectiveBranchId(license, user) }
    val branchName = remember(license, user) {
        user?.let { effectiveBranchName(license, it) }.orEmpty()
    }

    // Barcode scanner launcher — scan result goes directly to cart, no search field involved
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.takeIf { it.isNotBlank() }?.let { barcode ->
            viewModel.addByBarcode(barcode, branchId)
        }
    }
    val pickCashOutPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        cashOutPhotoUri = uri?.toString()
    }



    LaunchedEffect(user?.userId, branchId) {
        if (user != null) {
            viewModel.checkActiveShift(userRole = user.role, branchId = branchId, cashierId = user.userId)
        }
    }

    LaunchedEffect(user?.userId, branchId, shiftGateResolved, shiftBlocking, posKasirCatalogBlocked, search) {
        if (user == null || !shiftGateResolved || shiftBlocking || posKasirCatalogBlocked) return@LaunchedEffect
        if (search.isNotBlank()) delay(300L)
        viewModel.loadProducts(branchId, search)
    }

    LaunchedEffect(error) {
        if (error != null) {
            toastMsg = error
            viewModel.clearError()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
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
        contentWindowInsets = WindowInsets(0), // Outer Scaffold di MainActivity sudah urus inset
    ) { _ -> // abaikan innerPadding agar konten meluas penuh tanpa gap ganda
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 108.dp + androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
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
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HeaderHeight)
                                .padding(horizontal = SpacingLg, vertical = SpacingSm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // App branding
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(com.mediakasir.apotekpos.R.drawable.logo_apoapps_panjang),
                                contentDescription = "ApotekPOS Logo",
                                modifier = Modifier
                                    .height(30.dp)
                                    .align(Alignment.CenterVertically)
                            )

                            Spacer(Modifier.width(SpacingLg))

                            // Search bar — modern filled style
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
                                        fontSize = 13.sp,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(IconSizeMd),
                                    )
                                },
                                trailingIcon = if (search.isNotEmpty()) {
                                    {
                                        IconButton(onClick = {
                                            search = ""
                                            if (user != null && shiftGateResolved && !shiftBlocking && !posKasirCatalogBlocked) {
                                                viewModel.loadProducts(branchId, "")
                                            }
                                        }) {
                                            Icon(Icons.Outlined.Close, contentDescription = "Hapus", tint = TextMuted, modifier = Modifier.size(IconSizeSm))
                                        }
                                    }
                                } else {
                                    {
                                        IconButton(onClick = {
                                            scanLauncher.launch(
                                                ScanOptions().apply {
                                                    setPrompt("Arahkan ke barcode produk")
                                                    setBeepEnabled(true)
                                                    setOrientationLocked(false)
                                                }
                                            )
                                        }) {
                                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan", tint = TextMuted, modifier = Modifier.size(IconSizeMd))
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceFill,
                                    unfocusedContainerColor = SurfaceFill,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = ApoPrimary,
                                ),
                                shape = ShapeMd,
                            )

                            Spacer(Modifier.width(SpacingMd))

                            // Action icons
                            Row(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                horizontalArrangement = Arrangement.spacedBy(SpacingXs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Shift status badge — always visible
                                val shiftActive = activeShift != null
                                Surface(
                                    color = if (shiftActive) Success.copy(alpha = 0.10f) else Error.copy(alpha = 0.10f),
                                    shape = ShapeFull,
                                    onClick = {
                                        if (shiftActive) viewModel.requestCloseShift()
                                        else toastMsg = "Belum ada shift aktif. Hubungi admin untuk membuka shift."
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(SpacingXs),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (shiftActive) Success else Error, CircleShape)
                                        )
                                        Text(
                                            if (shiftActive) "Shift ${activeShift?.shiftType?.replaceFirstChar { it.uppercase() }}" else "No Shift",
                                            color = if (shiftActive) Success else Error,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }

                                // CP (Kas Keluar) button
                                IconButton(onClick = {
                                    if (activeShift == null) {
                                        android.widget.Toast.makeText(context, "Shift belum aktif. Tidak bisa mencatat Kas Keluar.", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        editingCashOut = null
                                        cashOutPhotoUri = null
                                        showCashOutDialog = true
                                    }
                                }) {
                                    Icon(Icons.Outlined.Payments, contentDescription = "Kas Keluar", tint = if (activeShift != null) TextSecondary else TextMuted, modifier = Modifier.size(IconSizeLg))
                                }

                                // CP History button
                                IconButton(onClick = {
                                    val shift = activeShift
                                    if (shift == null) {
                                        android.widget.Toast.makeText(context, "Shift belum aktif. Tidak ada riwayat kas keluar.", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.loadCashOutHistory(shift.id)
                                        showCashOutHistoryDialog = true
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = "Riwayat Kas Keluar", tint = if (activeShift != null) TextSecondary else TextMuted, modifier = Modifier.size(IconSizeLg))
                                }

                                // Network status — dropdown anchor
                                Box {
                                    Surface(
                                        onClick = { showNetworkStatusDialog = !showNetworkStatusDialog },
                                        shape = CircleShape,
                                        color = Color.Transparent,
                                    ) {
                                        Box(Modifier.padding(SpacingXs + 2.dp)) {
                                            NetworkStatusDot(
                                                isOnline = netOk,
                                                pendingCount = pendingOut,
                                            )
                                        }
                                    }
                                    // Network dropdown
                                    DropdownMenu(
                                        expanded = showNetworkStatusDialog,
                                        onDismissRequest = { showNetworkStatusDialog = false },
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .width(280.dp)
                                                .padding(SpacingMd),
                                            verticalArrangement = Arrangement.spacedBy(SpacingSm),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(SpacingSm),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                            if (netOk) Success else Error,
                                                            CircleShape,
                                                        )
                                                )
                                                Text(
                                                    if (netOk) "Online" else "Offline",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = TextPrimary,
                                                )
                                            }
                                            Text(
                                                if (netOk) "Perangkat terhubung ke internet."
                                                else "Data tetap disimpan lokal.",
                                                fontSize = 12.sp,
                                                color = TextSecondary,
                                            )
                                            HorizontalDivider(color = Border)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text("Antrian sync", fontSize = 12.sp, color = TextSecondary)
                                                Text(
                                                    "$pendingOut transaksi",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (pendingOut > 0) Warning else TextPrimary,
                                                )
                                            }
                                            if (pendingOut > 0 && netOk) {
                                                Button(
                                                    onClick = {
                                                        showNetworkStatusDialog = false
                                                        viewModel.retrySync()
                                                        toastMsg = "Sinkronisasi dimulai..."
                                                    },
                                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = ApoPrimary),
                                                    shape = ShapeSm,
                                                ) {
                                                    Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(IconSizeXs))
                                                    Spacer(Modifier.width(SpacingXs))
                                                    Text("Sinkronkan Sekarang", fontSize = 12.sp)
                                                }
                                            }
                                            TextButton(
                                                onClick = {
                                                    showNetworkStatusDialog = false
                                                    viewModel.openSyncErrorDialog()
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text("Lihat Detail", fontSize = 12.sp, color = TextSecondary)
                                            }
                                        }
                                    }
                                }

                                // Notification bell — dropdown anchor
                                Box {
                                    BadgedBox(
                                        badge = {
                                            if (alertCount > 0) {
                                                Badge(containerColor = Error, contentColor = Color.White) {
                                                    Text(alertCount.toString(), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                    ) {
                                        IconButton(onClick = { showNotificationDialog = !showNotificationDialog }) {
                                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifikasi", tint = TextSecondary, modifier = Modifier.size(IconSizeLg))
                                        }
                                    }
                                    // Notification dropdown
                                    DropdownMenu(
                                        expanded = showNotificationDialog,
                                        onDismissRequest = { showNotificationDialog = false },
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .width(300.dp)
                                                .padding(SpacingMd),
                                            verticalArrangement = Arrangement.spacedBy(SpacingSm),
                                        ) {
                                            Text(
                                                "Notifikasi",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = TextPrimary,
                                            )
                                            HorizontalDivider(color = Border)
                                            if (alertCount > 0) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Warning.copy(alpha = 0.08f), ShapeSm)
                                                        .padding(SpacingSm),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(SpacingSm),
                                                ) {
                                                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = Warning, modifier = Modifier.size(IconSizeMd))
                                                    Column {
                                                        Text("$alertCount peringatan aktif", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                                        Text("Kadaluarsa / stok tipis", fontSize = 11.sp, color = TextSecondary)
                                                    }
                                                }
                                            }
                                            if (pendingOut > 0) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Info.copy(alpha = 0.08f), ShapeSm)
                                                        .padding(SpacingSm),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(SpacingSm),
                                                ) {
                                                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = Info, modifier = Modifier.size(IconSizeMd))
                                                    Column {
                                                        Text("$pendingOut pending sync", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                                        Text(if (netOk) "Akan dikirim otomatis" else "Menunggu koneksi internet", fontSize = 11.sp, color = TextSecondary)
                                                    }
                                                }
                                            }
                                            if (shiftHardLock) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Error.copy(alpha = 0.08f), ShapeSm)
                                                        .padding(SpacingSm),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(SpacingSm),
                                                ) {
                                                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Error, modifier = Modifier.size(IconSizeMd))
                                                    Column {
                                                        Text("Shift melewati batas", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Error)
                                                        Text("Wajib tutup shift sekarang", fontSize = 11.sp, color = TextSecondary)
                                                    }
                                                }
                                                Button(
                                                    onClick = {
                                                        showNotificationDialog = false
                                                        viewModel.requestCloseShift()
                                                    },
                                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                                                    shape = ShapeSm,
                                                ) {
                                                    Text("Tutup Shift", fontSize = 12.sp)
                                                }
                                            }
                                            if (alertCount == 0 && pendingOut == 0 && !shiftHardLock) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = SpacingMd),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(IconSizeXl))
                                                        Spacer(Modifier.height(SpacingXs))
                                                        Text("Semua baik!", fontSize = 12.sp, color = TextSecondary)
                                                    }
                                                }
                                            }
                                        }
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
                                            Icon(Icons.Outlined.ShoppingCart, contentDescription = "Keranjang", tint = TextSecondary, modifier = Modifier.size(IconSizeLg))
                                        }
                                    }
                                }
                            }

                            // User info
                            if (user != null) {
                                Spacer(Modifier.width(SpacingSm))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = ApoPrimary,
                                        modifier = Modifier.size(AvatarSizeSm),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                user.name.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                            )
                                        }
                                    }
                                    if (wide) {
                                        Spacer(Modifier.width(SpacingSm))
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
                        // Subtle bottom border instead of shadow
                        HorizontalDivider(color = Border, thickness = 0.5.dp)
                        }
                    }

                    /* ══════════════════════════════════════════════════════════ */
                    /*  MAIN CONTENT                                            */
                    /* ══════════════════════════════════════════════════════════ */

                    // Shift expiry warning banner
                    if ((shiftExpiredWarning || shiftSoftAlert) && activeShift != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Error.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    if (shiftHardLock) {
                                        "⚠️ Batas waktu shift berakhir. Tutup shift sekarang untuk membuka transaksi kembali."
                                    } else {
                                        "⚠️ Shift sudah berjalan 8 jam. Segera lakukan tutup shift saat pergantian petugas."
                                    },
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = Error,
                                )
                                TextButton(onClick = { viewModel.requestCloseShift() }) {
                                    Text("Tutup Shift", color = Error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

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
                        // Shift not opened — show blocking CTA
                        user != null && shiftGateResolved && shiftBlocking -> Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(SpacingMd),
                            ) {
                                Icon(
                                    Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = TextMuted,
                                )
                                Text(
                                    "Belum Ada Shift Aktif",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                                Text(
                                    "Buka shift terlebih dahulu untuk mulai transaksi.",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
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
                        /* ─── WIDE LAYOUT (tablet) ─── */
                        wide -> Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            color = Color.White,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                            // Left: Cart items sidebar (docked)
                            Surface(
                                modifier = Modifier
                                    .weight(0.22f)
                                    .fillMaxHeight(),
                                color = SurfaceElevated,
                                shadowElevation = ElevationNone,
                                shape = RoundedCornerShape(0.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .padding(SpacingMd),
                                ) {
                                    Text(
                                        "Keranjang",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                    )
                                    Spacer(Modifier.height(SpacingXxs))
                                    Text(
                                        "${viewModel.getCartCount()} Item",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                    )
                                    
                                    if (cart.isEmpty()) {
                                        Spacer(Modifier.height(16.dp))
                                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Outlined.ShoppingCart, contentDescription = null, modifier = Modifier.size(IconSizeXxl), tint = TextMuted)
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
                                                        .background(SurfaceSubtle, ShapeSm)
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
                                                            onClick = {
                                                                if (item.qty <= 1) viewModel.removeFromCart(item.product.id)
                                                                else viewModel.updateQty(item.product.id, -1)
                                                            },
                                                            modifier = Modifier.size(24.dp),
                                                        ) {
                                                            Icon(
                                                                if (item.qty <= 1) Icons.Outlined.Delete else Icons.Outlined.Remove,
                                                                contentDescription = null,
                                                                tint = if (item.qty <= 1) Error else Primary,
                                                                modifier = Modifier.size(IconSizeXs),
                                                            )
                                                        }
                                                        Text(item.qty.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        IconButton(
                                                            onClick = { viewModel.updateQty(item.product.id, 1) },
                                                            modifier = Modifier.size(24.dp),
                                                        ) {
                                                            Icon(Icons.Outlined.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(IconSizeXs))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Subtotal", fontSize = 12.sp, color = TextSecondary)
                                            Text(formatIDR(viewModel.getSubtotal()), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        }
                                        TextButton(
                                            onClick = { viewModel.clearCart() },
                                            enabled = cart.isNotEmpty(),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = Error, modifier = Modifier.size(IconSizeXs))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Kosongkan", color = Error, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            VerticalDivider(color = DividerColor, modifier = Modifier.fillMaxHeight())

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
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    categoryTabs.forEach { tab ->
                                        val count = categoryCountMap[tab.label] ?: 0
                                        if (activeCategory == tab.label) {
                                            Button(
                                                onClick = { activeCategory = tab.label },
                                                colors = ButtonDefaults.buttonColors(containerColor = ApoPrimary),
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            ) {
                                                Text(tab.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                if (tab.label != "Semua") {
                                                    Spacer(Modifier.width(4.dp))
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color.White.copy(alpha = 0.25f),
                                                    ) {
                                                        Text(
                                                            count.toString(),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = { activeCategory = tab.label },
                                                shape = RoundedCornerShape(20.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            ) {
                                                Text(tab.label, fontSize = 13.sp, color = TextPrimary)
                                                if (tab.label != "Semua" && count > 0) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = InputBorder,
                                                    ) {
                                                        Text(
                                                            count.toString(),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextSecondary,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Button(
                                        onClick = { viewModel.showRacikanDialog() },
                                        colors = ButtonDefaults.buttonColors(containerColor = RacikanPurple),
                                        shape = ShapeFull,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    ) {
                                        Text("\uD83C\uDF3F  +Racikan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Product grid
                                if (filteredProducts.isEmpty() && !isLoading) {
                                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("Tidak ada produk ditemukan", color = TextMuted, fontSize = 15.sp)
                                    }
                                } else {
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
                            }

                            VerticalDivider(color = DividerColor, modifier = Modifier.fillMaxHeight())

                            // Right: Payment panel with quick buttons
                            Surface(
                                modifier = Modifier
                                    .weight(0.25f)
                                    .fillMaxHeight(),
                                color = SurfaceElevated,
                                shadowElevation = ElevationNone,
                                shape = RoundedCornerShape(0.dp),
                            ) {
                                PaymentPanel(
                                    cart = cart,
                                    payments = payments,
                                    discountLabel = selectedDiscountLabel,
                                    discounts = discounts,
                                    promotions = promotions,
                                    selectedDiscountId = selectedDiscountId,
                                    viewModel = viewModel,
                                    onCheckout = { doCheckout() },
                                    isProcessing = isProcessing,
                                    checkoutLocked = shiftHardLock,
                                    checkoutLockMessage = "Shift melewati batas waktu. Tutup shift untuk lanjut checkout.",
                                )
                            }
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
                                categoryTabs.forEach { tab ->
                                    if (activeCategory == tab.label) {
                                        Button(
                                            onClick = { activeCategory = tab.label },
                                            colors = ButtonDefaults.buttonColors(containerColor = ApoPrimary),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        ) { Text(tab.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                    } else {
                                        OutlinedButton(
                                            onClick = { activeCategory = tab.label },
                                            shape = RoundedCornerShape(20.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        ) { Text(tab.label, fontSize = 12.sp, color = TextPrimary) }
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                Button(
                                    onClick = { viewModel.showRacikanDialog() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RacikanPurple),
                                    shape = ShapeFull,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                ) {
                                    Text("\uD83C\uDF3F  +Racikan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(Modifier.height(12.dp))

                            if (filteredProducts.isEmpty() && !isLoading) {
                                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Tidak ada produk ditemukan", color = TextMuted)
                                }
                            } else {
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
                if (showRacikanDialog) {
                    RacikanDialog(
                        recipes = compoundRecipes,
                        isLoading = isLoadingRacikan,
                        errorMessage = racikanError,
                        onRetry = { viewModel.loadCompoundRecipes() },
                        onAdd = { viewModel.addRacikanToCart(it) },
                        onDismiss = { viewModel.hideRacikanDialog() },
                    )
                }
                if (user != null && shiftGateResolved && shiftBlocking) {
                    OpenShiftGateDialog(
                        isLoading = startingShift,
                        errorMessage = shiftDialogError,
                        onClearError = { viewModel.clearShiftDialogError() },
                        onSubmit = { viewModel.submitStartingShift(it, branchId = branchId, cashierId = user.userId, cashierName = user.name) },
                    )
                }
                if (showCloseShiftDialog) {
                    CloseShiftDialog(
                        isLoading = closingShift,
                        errorMessage = shiftDialogError,
                        onClearError = { viewModel.clearShiftDialogError() },
                        onDismiss = { viewModel.dismissCloseShiftDialog() },
                        onSubmit = { cash, notes -> viewModel.closeShift(cash, notes, branchName) },
                    )
                }
                val currentShift = activeShift
                if (showCashOutDialog && user != null && currentShift != null) {
                    val editing = editingCashOut
                    CashOutDialog(
                        initialAmountDigits = editing?.amount?.toLong()?.toString() ?: "",
                        initialCategory = editing?.category ?: "Operasional",
                        initialNote = editing?.note.orEmpty(),
                        isEditMode = editing != null,
                        photoUri = cashOutPhotoUri,
                        onDismiss = {
                            showCashOutDialog = false
                            editingCashOut = null
                            cashOutPhotoUri = null
                        },
                        onPickPhoto = { pickCashOutPhotoLauncher.launch("image/*") },
                        onSubmit = { amountDigits, category, note ->
                            val amount = parseMoneyInputToDouble(amountDigits)
                            if (amount == null || amount <= 0.0) {
                                toastMsg = "Nominal Kas Keluar tidak valid."
                            } else {
                                if (editing != null) {
                                    viewModel.updateCashOut(
                                        id = editing.id,
                                        shiftId = currentShift.id,
                                        actorCashierId = user.userId,
                                        amount = amount,
                                        category = category,
                                        note = note,
                                        receiptPhotoPath = cashOutPhotoUri ?: editing.receiptPhotoPath,
                                    )
                                    toastMsg = "Kas Keluar diperbarui: ${formatIDR(amount)}"
                                } else {
                                    viewModel.recordCashOut(
                                        shiftId = currentShift.id,
                                        branchId = branchId,
                                        cashierId = user.userId,
                                        amount = amount,
                                        category = category,
                                        note = note,
                                        receiptPhotoPath = cashOutPhotoUri,
                                    )
                                    toastMsg = "Kas Keluar tersimpan: ${formatIDR(amount)}"
                                }
                                showCashOutDialog = false
                                editingCashOut = null
                                cashOutPhotoUri = null
                            }
                        },
                    )
                }
                if (showCashOutHistoryDialog) {
                    val canManageCashOut = activeShift?.endedAt == null && !showCloseShiftDialog && !closingShift
                    CashOutHistoryDialog(
                        isLoading = cashOutLoading,
                        total = cashOutTotal,
                        items = cashOutHistory,
                        canManageActions = canManageCashOut,
                        onEdit = { entry ->
                            editingCashOut = entry
                            cashOutPhotoUri = entry.receiptPhotoPath
                            showCashOutDialog = true
                        },
                        onDelete = { entry ->
                            deleteCashOutTarget = entry
                        },
                        onDismiss = { showCashOutHistoryDialog = false },
                    )
                }
                deleteCashOutTarget?.let { target ->
                    AlertDialog(
                        onDismissRequest = { deleteCashOutTarget = null },
                        title = { Text("Hapus Kas Keluar?") },
                        text = { Text("Nominal ${formatIDR(target.amount)} (${target.category}) akan dihapus dari shift ini.") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteCashOut(
                                    id = target.id,
                                    shiftId = target.shiftId,
                                    actorCashierId = user?.userId.orEmpty(),
                                )
                                deleteCashOutTarget = null
                                toastMsg = "Kas Keluar dihapus"
                            }) {
                                Text("Hapus", color = Error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { deleteCashOutTarget = null }) {
                                Text("Batal")
                            }
                        },
                    )
                }
                shiftSummary?.let { summary ->
                    ShiftSummaryDialog(
                        summary = summary,
                        onDismiss = { viewModel.dismissShiftSummary() },
                        onPrint = { device ->
                            viewModel.rememberPreferredPrinter(device.address)
                            val report = ThermalPrinterManager.ShiftReportData(
                                pharmacyName = license?.pharmacyName ?: "Apotek",
                                shiftType = summary.shiftType,
                                cashierName = summary.cashierName,
                                branchName = summary.branchName,
                                startedAt = summary.startedAt,
                                endedAt = summary.endedAt,
                                startingCash = summary.startingCash,
                                endingCash = summary.endingCash,
                                expectedCash = summary.expectedCash,
                                difference = summary.difference,
                                totalSales = summary.totalSales,
                                totalCashSales = summary.totalCashSales,
                                totalNonCashSales = summary.totalNonCashSales,
                                totalQrisSales = summary.totalQrisSales,
                                totalCashOut = summary.totalCashOut,
                                totalTransactions = summary.totalTransactions,
                                topSellingProducts = summary.topSellingProducts.map {
                                    ThermalPrinterManager.TopSellingLine(name = it.name, qty = it.qty)
                                },
                            )
                            coroutineScope.launch {
                                ThermalPrinterManager.printShiftReport(context, device, report)
                            }
                        },
                    )
                }

                if (shiftHardLock && activeShift != null) {
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
                                .background(Color.Black.copy(alpha = 0.60f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .widthIn(max = 460.dp)
                                    .fillMaxWidth(0.94f),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                            ) {
                                Column(Modifier.padding(24.dp)) {
                                    Text("Batas waktu shift berakhir", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Error)
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "Anda wajib melakukan Tutup Shift untuk melanjutkan transaksi kembali.",
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    Button(
                                        onClick = { viewModel.requestCloseShift() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Error),
                                    ) {
                                        Text("Tutup Shift Sekarang", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSyncErrorDialog) {
        SyncErrorDialog(
            pendingCount = pendingOut,
            errors = syncErrors,
            isRetrying = isRetryingSync,
            netOk = netOk,
            retryMessage = syncRetryMessage,
            onRetry = { viewModel.retrySync() },
            onDismiss = { viewModel.closeSyncErrorDialog() },
        )
    }

    // Network and Notification dropdowns are now inline in the header — no separate dialogs needed.

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
private fun CashOutHistoryDialog(
    isLoading: Boolean,
    total: Double,
    items: List<LocalCashExpenseEntity>,
    canManageActions: Boolean,
    onEdit: (LocalCashExpenseEntity) -> Unit,
    onDelete: (LocalCashExpenseEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
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
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(0.95f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Riwayat Kas Keluar", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Total CP Shift: ${formatIDR(total)}", fontSize = 13.sp, color = Error, fontWeight = FontWeight.SemiBold)
                    if (!canManageActions) {
                        Spacer(Modifier.height(4.dp))
                        Text("Shift sudah ditutup. Data Kas Keluar hanya bisa dilihat.", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (isLoading) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ApoPrimary)
                        }
                    } else if (items.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada transaksi Kas Keluar di shift ini.", color = TextMuted)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items) { entry ->
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(entry.category, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                Spacer(Modifier.width(6.dp))
                                                if (!entry.receiptPhotoPath.isNullOrBlank()) {
                                                    Text("Nota", fontSize = 10.sp, color = ApoPrimary)
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("-${formatIDR(entry.amount)}", color = Error, fontWeight = FontWeight.Bold)
                                                IconButton(
                                                    onClick = { onEdit(entry) },
                                                    enabled = canManageActions,
                                                    modifier = Modifier.size(22.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Edit,
                                                        contentDescription = "Edit",
                                                        tint = if (canManageActions) ApoPrimary else TextMuted,
                                                        modifier = Modifier.size(IconSizeXs),
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onDelete(entry) },
                                                    enabled = canManageActions,
                                                    modifier = Modifier.size(22.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "Hapus",
                                                        tint = if (canManageActions) Error else TextMuted,
                                                        modifier = Modifier.size(IconSizeXs),
                                                    )
                                                }
                                            }
                                        }
                                        if (!entry.note.isNullOrBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(entry.note, fontSize = 12.sp, color = TextSecondary)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(formatDateTime(entry.createdAt), fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CashOutDialog(
    initialAmountDigits: String,
    initialCategory: String,
    initialNote: String,
    isEditMode: Boolean,
    photoUri: String?,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onSubmit: (amountDigits: String, category: String, note: String) -> Unit,
) {
    var digits by remember(initialAmountDigits) { mutableStateOf(initialAmountDigits) }
    val display = formatDigitsAsIndonesianNumber(digits)
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }
    val categories = remember {
        listOf("Operasional", "Transport", "Kebersihan", "Konsumsi", "Lainnya")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
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
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(0.94f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                ) {
                    Text("Kas Keluar", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Catat pengeluaran kas agar expected cash shift tetap akurat.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )

                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = display,
                        onValueChange = { raw -> digits = raw.filter { it.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nominal (Rp)") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("Kategori", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { cat ->
                            val active = selectedCategory == cat
                            if (active) {
                                Button(
                                    onClick = { selectedCategory = cat },
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ApoPrimary),
                                ) { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedCategory = cat },
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) { Text(cat, fontSize = 12.sp, color = TextPrimary) }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Catatan") },
                        placeholder = { Text("Contoh: Bayar sampah harian") },
                        minLines = 2,
                        maxLines = 3,
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onPickPhoto,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp), tint = ApoPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(if (photoUri == null) "Pilih Foto Nota" else "Ganti Foto Nota", color = ApoPrimary)
                    }
                    if (photoUri != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Nota: ${photoUri.substringAfterLast('/')}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text("Batal") }
                        Button(
                            onClick = { onSubmit(digits, selectedCategory, note) },
                            enabled = digits.isNotBlank(),
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ApoPrimary),
                        ) { Text(if (isEditMode) "Perbarui" else "Simpan", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
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
private fun CloseShiftDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var digits by remember { mutableStateOf("") }
    val display = formatDigitsAsIndonesianNumber(digits)
    var notes by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
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
                    Text("Tutup Shift", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Masukkan jumlah kas aktual di laci untuk menutup shift.",
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
                        label = { Text("Kas akhir (Rp)") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !isLoading,
                        isError = !errorMessage.isNullOrBlank(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Catatan (opsional)") },
                        singleLine = true,
                        enabled = !isLoading,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = !isLoading,
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = { onSubmit(digits, notes) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = !isLoading && digits.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Error),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Tutup Shift", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftSummaryDialog(
    summary: ShiftSummaryData,
    onDismiss: () -> Unit,
    onPrint: (android.bluetooth.BluetoothDevice) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPrinterPicker by remember { mutableStateOf(false) }

    // Duration
    val durationText = remember(summary.startedAt, summary.endedAt) {
        runCatching {
            val start = java.time.Instant.parse(summary.startedAt)
            val end   = java.time.Instant.parse(summary.endedAt)
            val mins  = java.time.Duration.between(start, end).toMinutes()
            "${mins / 60}j ${mins % 60}m"
        }.getOrDefault("-")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
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
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(0.94f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                ) {
                    // Title
                    Text(
                        "Laporan Tutup Shift ${summary.shiftType.replaceFirstChar { it.uppercase() }}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(summary.cashierName, fontSize = 13.sp, color = TextSecondary)
                    if (summary.branchName.isNotBlank()) {
                        Text(summary.branchName, fontSize = 12.sp, color = TextMuted)
                    }
                    Spacer(Modifier.height(4.dp))

                    // Time range
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Info.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                "Mulai: ${com.mediakasir.apotekpos.utils.formatDateTime(summary.startedAt)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = Info,
                            )
                        }
                        Surface(
                            color = Info.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                "Selesai: ${com.mediakasir.apotekpos.utils.formatDateTime(summary.endedAt)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = Info,
                            )
                        }
                    }
                    Text("Durasi: $durationText", fontSize = 12.sp, color = TextMuted)
                    Spacer(Modifier.height(16.dp))

                    @Composable
                    fun SummaryRow(label: String, value: String, valueColor: Color = TextPrimary, bold: Boolean = false) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, fontSize = 14.sp, color = TextSecondary)
                            Text(value, fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = valueColor)
                        }
                    }

                    // Sales section
                    Text("Penjualan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(Modifier.height(4.dp))
                    SummaryRow("Total Transaksi", "${summary.totalTransactions}")
                    SummaryRow("Total Penjualan", formatIDR(summary.totalSales), bold = true)
                    SummaryRow("  Tunai", formatIDR(summary.totalCashSales))
                    SummaryRow("  Non-Tunai", formatIDR(summary.totalNonCashSales))

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Cash reconciliation section
                    Text("Rekap Kas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(Modifier.height(4.dp))
                    SummaryRow("Kas Awal", formatIDR(summary.startingCash))
                    SummaryRow("Kas Keluar (CP)", formatIDR(summary.totalCashOut), valueColor = Error)
                    if (summary.cashOutByCategory.isNotEmpty()) {
                        summary.cashOutByCategory.forEach { row ->
                            SummaryRow("  • ${row.category}", formatIDR(row.total), valueColor = TextSecondary)
                        }
                    }
                    SummaryRow("Kas Akhir (Aktual)", formatIDR(summary.endingCash))
                    SummaryRow("Kas Akhir (Expected)", formatIDR(summary.expectedCash))
                    val diffColor = when {
                        summary.difference > 0 -> Success
                        summary.difference < 0 -> Error
                        else -> TextPrimary
                    }
                    val diffPrefix = if (summary.difference > 0) "+" else ""
                    val diffLabel = when {
                        summary.difference > 0 -> "Selisih (Lebih)"
                        summary.difference < 0 -> "Selisih (Kurang)"
                        else -> "Selisih"
                    }
                    SummaryRow(diffLabel, "$diffPrefix${formatIDR(summary.difference)}", diffColor, bold = true)

                    Spacer(Modifier.height(20.dp))

                    // Action buttons
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showPrinterPicker = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PosWebPrimary),
                        ) {
                            Icon(Icons.Outlined.Print, contentDescription = null, tint = PosWebPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cetak", color = PosWebPrimary)
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PosWebPrimary),
                        ) {
                            Text("Selesai", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Bluetooth printer picker for shift report
    if (showPrinterPicker) {
        val pairedDevices = remember { ThermalPrinterManager.getPairedPrinters(context) }
        Dialog(
            onDismissRequest = { showPrinterPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.90f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Pilih Printer Bluetooth", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(12.dp))
                        if (pairedDevices.isEmpty()) {
                            Text("Tidak ada printer Bluetooth yang dipasangkan.", color = TextMuted, fontSize = 14.sp)
                        } else {
                            pairedDevices.forEach { device ->
                                val name = runCatching { device.name }.getOrDefault("Unknown")
                                TextButton(
                                    onClick = {
                                        showPrinterPicker = false
                                        onPrint(device)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.Print, contentDescription = null, tint = PosWebPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(name, color = TextPrimary)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { showPrinterPicker = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("Batal")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncErrorDialog(
    pendingCount: Int,
    errors: List<com.mediakasir.apotekpos.data.local.FailedSyncInfo>,
    isRetrying: Boolean,
    netOk: Boolean,
    retryMessage: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
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
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = if (errors.isNotEmpty()) Error else Info,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            "Status Sinkronisasi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    if (!netOk) {
                        Surface(color = Warning.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "⚠ Tidak ada koneksi internet. Transaksi akan dikirim otomatis saat online kembali.",
                                modifier = Modifier.padding(10.dp),
                                fontSize = 13.sp,
                                color = Warning,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    Text(
                        "$pendingCount transaksi belum terkirim ke server.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Data tersimpan aman di perangkat dan akan dikirim otomatis setiap 15 menit, atau saat kembali online.",
                        fontSize = 12.sp,
                        color = TextMuted,
                    )

                    if (errors.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "${errors.size} transaksi gagal dikirim:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Error,
                        )
                        Spacer(Modifier.height(8.dp))
                        errors.forEach { e ->
                            Surface(
                                color = Error.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            formatIDR(e.grandTotal),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                        )
                                        Text(
                                            "Percobaan: ${e.syncAttempts}x",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                        )
                                    }
                                    if (!e.lastSyncError.isNullOrBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            e.lastSyncError,
                                            fontSize = 11.sp,
                                            color = Error,
                                            maxLines = 2,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tekan 'Coba Lagi' untuk memaksa pengiriman ulang sekarang.",
                            fontSize = 12.sp,
                            color = TextMuted,
                        )
                    }

                    // Retry result message
                    if (retryMessage != null) {
                        Spacer(Modifier.height(12.dp))
                        val isSuccess = retryMessage.startsWith("✅")
                        val msgColor = when {
                            isSuccess -> Success
                            retryMessage.startsWith("⚠") -> Warning
                            else -> Error
                        }
                        Surface(
                            color = msgColor.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                retryMessage,
                                modifier = Modifier.padding(10.dp),
                                fontSize = 13.sp,
                                color = msgColor,
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(46.dp),
                            enabled = !isRetrying,
                        ) {
                            Text("Tutup")
                        }
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f).height(46.dp),
                            enabled = !isRetrying && netOk,
                            colors = ButtonDefaults.buttonColors(containerColor = PosWebPrimary),
                        ) {
                            if (isRetrying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Coba Lagi", fontWeight = FontWeight.SemiBold)
                            }
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
            .height(186.dp)
            .clickable(enabled = !isOutOfStock) { onClick() },
        shape = ShapeLg,
        elevation = CardDefaults.cardElevation(defaultElevation = ElevationNone),
        colors = CardDefaults.cardColors(containerColor = if (isOutOfStock) Subtle else Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isOutOfStock) Color.Transparent else Border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
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
                    shape = RoundedCornerShape(999.dp),
                    color = if (isOutOfStock) Error.copy(alpha = 0.13f) else catColor.copy(alpha = 0.13f),
                ) {
                    Text(
                        if (isOutOfStock) "Habis" else categoryShortLabel(product.category),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOutOfStock) Error else catColor,
                        maxLines = 1,
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

@SuppressLint("MissingPermission")
@Composable
fun ReceiptDialog(
    transaction: Transaction,
    pharmacyName: String,
    address: String,
    phone: String,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPrinterPicker by remember { mutableStateOf(false) }
    var isPrinting by remember { mutableStateOf(false) }
    var printStatus by remember { mutableStateOf<String?>(null) }
    var printerTab by remember { mutableStateOf("bluetooth") } // "bluetooth" | "usb"
    var pendingUsbDevice by remember { mutableStateOf<android.hardware.usb.UsbDevice?>(null) }
    var usbPermissionGranted by remember { mutableStateOf(false) }

    // Bluetooth permission launcher (Android 12+)
    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            showPrinterPicker = true
        } else {
            printStatus = "Izin Bluetooth diperlukan"
        }
    }

    // Register BroadcastReceiver for USB permission result
    val usbPermissionReceiver = remember {
        object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == ThermalPrinterManager.ACTION_USB_PERMISSION) {
                    val granted = intent.getBooleanExtra(android.hardware.usb.UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    usbPermissionGranted = granted
                    if (!granted) printStatus = "Izin USB ditolak"
                }
            }
        }
    }
    DisposableEffect(Unit) {
        val filter = android.content.IntentFilter(ThermalPrinterManager.ACTION_USB_PERMISSION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(usbPermissionReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            ctx.registerReceiver(usbPermissionReceiver, filter)
        }
        onDispose { ctx.unregisterReceiver(usbPermissionReceiver) }
    }

    // Auto-print via USB once permission is granted
    LaunchedEffect(usbPermissionGranted) {
        if (usbPermissionGranted) {
            val device = pendingUsbDevice ?: return@LaunchedEffect
            showPrinterPicker = false
            isPrinting = true
            printStatus = null
            val result = ThermalPrinterManager.printReceiptUsb(ctx, device, transaction, pharmacyName, address, phone)
            isPrinting = false
            printStatus = if (result.isSuccess) "Print berhasil!" else "Gagal print: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            usbPermissionGranted = false
            pendingUsbDevice = null
        }
    }

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

                // Print status message
                printStatus?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 11.sp,
                        color = if (msg.contains("berhasil", ignoreCase = true)) Success else Warning,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Print button
                    OutlinedButton(
                        onClick = {
                            printStatus = null
                            showPrinterPicker = true
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isPrinting,
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPrinting) "Printing..." else "Print")
                    }
                    // Share button
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
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Bagikan")
                    }
                    // Close button
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

    // Printer selection dialog (Bluetooth + USB tabs)
    if (showPrinterPicker) {
        AlertDialog(
            onDismissRequest = { showPrinterPicker = false },
            title = { Text("Pilih Printer", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // Tab switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("bluetooth" to "Bluetooth", "usb" to "USB").forEach { (key, label) ->
                            val selected = printerTab == key
                            Surface(
                                onClick = { printerTab = key },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) ApoPrimary else Color(0xFFEFF2F5),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else TextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    if (printerTab == "bluetooth") {
                        // ── Bluetooth tab ──────────────────────────────────
                        if (!ThermalPrinterManager.isBluetoothEnabled(ctx)) {
                            Text("Bluetooth tidak aktif. Aktifkan Bluetooth di pengaturan perangkat.", fontSize = 13.sp, color = Warning)
                        } else {
                            val pairedDevices = remember { ThermalPrinterManager.getPairedPrinters(ctx) }
                            if (pairedDevices.isEmpty()) {
                                Text("Tidak ada printer Bluetooth yang dipasangkan.\nPasangkan printer di pengaturan Bluetooth terlebih dahulu.", fontSize = 13.sp)
                            } else {
                                Text("Perangkat yang dipasangkan:", fontSize = 12.sp, color = TextSecondary)
                                Spacer(Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    pairedDevices.forEach { device ->
                                        Surface(
                                            onClick = {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                    // Check permission first, then print
                                                    val hasPerm = ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                                                        android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    if (hasPerm) {
                                                        showPrinterPicker = false
                                                        isPrinting = true
                                                        printStatus = null
                                                        scope.launch {
                                                            val result = ThermalPrinterManager.printReceipt(ctx, device, transaction, pharmacyName, address, phone)
                                                            isPrinting = false
                                                            printStatus = if (result.isSuccess) "Print berhasil!" else "Gagal print: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                                        }
                                                    } else {
                                                        btPermissionLauncher.launch(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN))
                                                    }
                                                } else {
                                                    showPrinterPicker = false
                                                    isPrinting = true
                                                    printStatus = null
                                                    scope.launch {
                                                        val result = ThermalPrinterManager.printReceipt(ctx, device, transaction, pharmacyName, address, phone)
                                                        isPrinting = false
                                                        printStatus = if (result.isSuccess) "Print berhasil!" else "Gagal print: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF8FAFC),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(20.dp), tint = ApoPrimary)
                                                Spacer(Modifier.width(10.dp))
                                                Column {
                                                    Text(device.name ?: "Unknown Device", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                    Text(device.address, fontSize = 10.sp, color = TextMuted)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ── USB tab ────────────────────────────────────────
                        val usbDevices = remember { ThermalPrinterManager.getUsbPrinters(ctx) }
                        if (usbDevices.isEmpty()) {
                            Text("Tidak ada perangkat USB yang terhubung.\nHubungkan printer USB via kabel OTG.", fontSize = 13.sp)
                        } else {
                            Text("Perangkat USB yang terhubung:", fontSize = 12.sp, color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                usbDevices.forEach { device ->
                                    Surface(
                                        onClick = {
                                            if (ThermalPrinterManager.hasUsbPermission(ctx, device)) {
                                                showPrinterPicker = false
                                                isPrinting = true
                                                printStatus = null
                                                scope.launch {
                                                    val result = ThermalPrinterManager.printReceiptUsb(ctx, device, transaction, pharmacyName, address, phone)
                                                    isPrinting = false
                                                    printStatus = if (result.isSuccess) "Print berhasil!" else "Gagal print: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                                }
                                            } else {
                                                pendingUsbDevice = device
                                                ThermalPrinterManager.requestUsbPermission(ctx, device)
                                                printStatus = "Berikan izin akses USB pada dialog yang muncul"
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(20.dp), tint = ApoPrimary)
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(device.productName ?: "USB Printer [${device.vendorId}:${device.productId}]", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Text("VID:${device.vendorId} PID:${device.productId}", fontSize = 10.sp, color = TextMuted)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrinterPicker = false }) {
                    Text("Batal")
                }
            },
        )
    }
}

