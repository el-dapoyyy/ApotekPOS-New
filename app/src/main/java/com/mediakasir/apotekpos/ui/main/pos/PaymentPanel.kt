package com.mediakasir.apotekpos.ui.main.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediakasir.apotekpos.data.model.PosCustomerDto
import com.mediakasir.apotekpos.data.model.PosDiscountDto
import com.mediakasir.apotekpos.data.model.PosPromotionDto
import com.mediakasir.apotekpos.ui.theme.ApoPrimary
import com.mediakasir.apotekpos.ui.theme.ApoPrimaryDark
import com.mediakasir.apotekpos.ui.theme.Error
import com.mediakasir.apotekpos.ui.theme.InputBorder
import com.mediakasir.apotekpos.ui.theme.Success
import com.mediakasir.apotekpos.ui.theme.TextMuted
import com.mediakasir.apotekpos.ui.theme.TextPrimary
import com.mediakasir.apotekpos.ui.theme.TextSecondary
import com.mediakasir.apotekpos.ui.theme.Warning
import com.mediakasir.apotekpos.utils.formatIDR
import com.mediakasir.apotekpos.utils.parseMoneyInputToDouble
import kotlinx.coroutines.delay

/**
 * Payment panel composable for the right sidebar of POS screen.
 * Compact design: Pelanggan / Voucher / Promo rows each open a dialog.
 */
@Composable
fun PaymentPanel(
    cart: List<CartItem>,
    payments: List<PaymentEntry>,
    discountLabel: String?,
    discounts: List<PosDiscountDto> = emptyList(),
    promotions: List<PosPromotionDto> = emptyList(),
    selectedDiscountId: Int? = null,
    viewModel: POSViewModel,
    onCheckout: () -> Unit,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val subtotal = viewModel.getSubtotal()
    val discountAmt = viewModel.getDiscountAmt()
    val total = viewModel.getTotal()
    val totalPaid = viewModel.getTotalPaid()
    val change = viewModel.getChange()
    val remaining = (total - totalPaid).coerceAtLeast(0.0)
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val cashPaid = payments.firstOrNull { it.method.equals("Tunai", ignoreCase = true) }
        ?.let { parseMoneyInputToDouble(it.amount) }
        ?: 0.0

    var manualCashInput by remember(cashPaid) {
        mutableStateOf(if (cashPaid > 0.0) cashPaid.toLong().toString() else "")
    }

    val quickAmounts = buildList {
        if (total > 0.0) add(total)
        val round10 = kotlin.math.ceil(total / 10_000.0) * 10_000.0
        val round50 = kotlin.math.ceil(total / 50_000.0) * 50_000.0
        val round100 = kotlin.math.ceil(total / 100_000.0) * 100_000.0
        addAll(listOf(round10, round50, round100, 200_000.0, 500_000.0, cashPaid).filter { it >= total && it > 0.0 })
    }.distinct().sorted()

    val cartProductIds = cart.map { it.product.id }.toSet()
    val relevantPromoCount = promotions.count { promo ->
        promo.promotionProducts?.any { pp -> pp.productId.toString() in cartProductIds } == true
    }

    var showCustomerDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showPromoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().fillMaxHeight().background(Color(0xFFF8FAFC)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text("Pembayaran", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                text = if (remaining > 0.0) "Masukkan nominal pembayaran" else "Pembayaran lengkap",
                fontSize = 10.sp,
                color = TextSecondary,
            )
        }

        HorizontalDivider(color = Color(0xFFE5E7EB))

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // RINGKASAN
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ringkasan", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    PaymentMetricRow("Subtotal", formatIDR(subtotal))
                    if (discountAmt > 0) {
                        PaymentMetricRow(
                            label = if (discountLabel != null) "Diskon • $discountLabel" else "Diskon",
                            value = "- ${formatIDR(discountAmt)}",
                            valueColor = Success,
                        )
                    }
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    PaymentMetricRow(
                        label = "Total", value = formatIDR(total),
                        labelWeight = FontWeight.Bold, valueWeight = FontWeight.ExtraBold, valueColor = ApoPrimary,
                    )
                }
            }

            // INFO ROWS
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Pelanggan
                    InfoActionRow(
                        icon = { Icon(Icons.Filled.AccountCircle, null, modifier = Modifier.size(16.dp), tint = if (selectedCustomer != null) ApoPrimary else TextMuted) },
                        label = "Pelanggan",
                        value = selectedCustomer?.name ?: "Umum",
                        valueTint = if (selectedCustomer != null) ApoPrimary else TextSecondary,
                        onClick = { showCustomerDialog = true },
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(horizontal = 12.dp))

                    // Voucher/Diskon
                    val activeDiscount = selectedDiscountId?.let { id -> discounts.find { it.id == id } }
                    InfoActionRow(
                        icon = { Icon(Icons.Filled.LocalOffer, null, modifier = Modifier.size(16.dp), tint = if (activeDiscount != null) Success else TextMuted) },
                        label = "Voucher",
                        value = activeDiscount?.let { d ->
                            when (d.type?.lowercase()) {
                                "percentage" -> "${d.value?.toInt() ?: 0}% off"
                                else -> "- ${formatIDR(d.value ?: 0.0)}"
                            }
                        } ?: if (discounts.isNotEmpty()) "${discounts.size} tersedia" else "Tidak ada",
                        valueTint = if (activeDiscount != null) Success else TextSecondary,
                        badge = if (activeDiscount != null) "Aktif" else null,
                        badgeColor = Success,
                        trailingClear = activeDiscount != null,
                        onClear = { viewModel.clearAppliedDiscount() },
                        onClick = { if (discounts.isNotEmpty()) showDiscountDialog = true },
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(horizontal = 12.dp))

                    // Promo
                    val totalPromos = promotions.size
                    InfoActionRow(
                        icon = { Icon(Icons.Filled.Star, null, modifier = Modifier.size(16.dp), tint = if (relevantPromoCount > 0) Warning else TextMuted) },
                        label = "Promo",
                        value = when {
                            relevantPromoCount > 0 -> "$relevantPromoCount cocok"
                            totalPromos > 0 -> "$totalPromos tersedia"
                            else -> "Tidak ada"
                        },
                        valueTint = if (relevantPromoCount > 0) Warning else TextSecondary,
                        badge = if (relevantPromoCount > 0) "$relevantPromoCount" else null,
                        badgeColor = Warning,
                        onClick = { if (totalPromos > 0) showPromoDialog = true },
                    )
                }
            }

            // TUNAI DITERIMA
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Tunai Diterima", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        if (cashPaid > 0.0) {
                            Text("Reset", fontSize = 10.sp, color = ApoPrimary, modifier = Modifier.clickable {
                                manualCashInput = ""
                                viewModel.clearCashAmount()
                            })
                        }
                    }
                    OutlinedTextField(
                        value = manualCashInput,
                        onValueChange = {
                            manualCashInput = it
                            val parsed = parseMoneyInputToDouble(it)
                            if (parsed != null && parsed > 0) viewModel.setCashAmount(parsed)
                            else if (it.isBlank()) viewModel.clearCashAmount()
                        },
                        placeholder = { Text("Masukkan nominal", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC), unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = ApoPrimary, unfocusedBorderColor = InputBorder,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    if (quickAmounts.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            quickAmounts.take(4).chunked(2).forEach { rowAmounts ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowAmounts.forEach { amount ->
                                        val selected = kotlin.math.abs(cashPaid - amount) < 0.5
                                        OutlinedButton(
                                            onClick = { viewModel.setCashAmount(amount); manualCashInput = amount.toLong().toString() },
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (selected) ApoPrimary else InputBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) ApoPrimary.copy(alpha = 0.08f) else Color.White),
                                        ) {
                                            Text(
                                                text = if (kotlin.math.abs(amount - total) < 0.5) "Pas" else compactCashLabel(amount),
                                                fontSize = 10.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (selected) ApoPrimary else TextPrimary,
                                            )
                                        }
                                    }
                                    if (rowAmounts.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // STATUS PEMBAYARAN
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Status Pembayaran", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    PaymentMetricRow("Diterima", formatIDR(totalPaid))
                    if (remaining > 0.0) {
                        PaymentMetricRow("Sisa", formatIDR(remaining), valueColor = Error, valueWeight = FontWeight.Bold)
                    } else {
                        PaymentMetricRow("Kembalian", formatIDR(maxOf(0.0, change)), valueColor = ApoPrimaryDark, valueWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // BAYAR BUTTON
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Button(
                    onClick = onCheckout,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isProcessing && cart.isNotEmpty() && change >= 0,
                    colors = ButtonDefaults.buttonColors(containerColor = ApoPrimary, disabledContainerColor = Color(0xFFD1D5DB)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Memproses...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Bayar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // DIALOGS
    if (showCustomerDialog) {
        CustomerPickerDialog(
            viewModel = viewModel,
            selectedCustomer = selectedCustomer,
            onSelect = { c -> viewModel.setSelectedCustomer(c); showCustomerDialog = false },
            onDismiss = { showCustomerDialog = false },
        )
    }

    if (showDiscountDialog) {
        DiscountPickerDialog(
            discounts = discounts,
            selectedDiscountId = selectedDiscountId,
            subtotal = subtotal,
            onApply = { d ->
                val err = viewModel.applyDiscount(d)
                if (err == null) showDiscountDialog = false
            },
            onClear = { viewModel.clearAppliedDiscount(); showDiscountDialog = false },
            onDismiss = { showDiscountDialog = false },
        )
    }

    if (showPromoDialog) {
        PromoInfoDialog(
            promotions = promotions,
            cartProductIds = cartProductIds,
            onDismiss = { showPromoDialog = false },
        )
    }
}

// COMPACT INFO ACTION ROW

@Composable
private fun InfoActionRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    valueTint: Color = TextSecondary,
    badge: String? = null,
    badgeColor: Color = ApoPrimary,
    trailingClear: Boolean = false,
    onClear: () -> Unit = {},
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text(value, fontSize = 11.sp, color = valueTint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 100.dp))
            if (trailingClear) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Hapus",
                    modifier = Modifier.size(15.dp).clickable { onClear() },
                    tint = TextMuted,
                )
            } else {
                Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(15.dp), tint = TextMuted)
            }
        }
    }
}

// CUSTOMER PICKER DIALOG

@Composable
private fun CustomerPickerDialog(
    viewModel: POSViewModel,
    selectedCustomer: PosCustomerDto?,
    onSelect: (PosCustomerDto?) -> Unit,
    onDismiss: () -> Unit,
) {
    val results by viewModel.customerResults.collectAsState()
    val searching by viewModel.customerSearching.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.searchCustomers("") }
    LaunchedEffect(query) {
        delay(350)
        viewModel.searchCustomers(query)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Pelanggan", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari nama / telepon...", fontSize = 12.sp) },
                    leadingIcon = {
                        if (searching) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ApoPrimary, unfocusedBorderColor = InputBorder),
                )
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CustomerRow("Umum", "Transaksi tanpa pelanggan terdaftar", selectedCustomer == null) { onSelect(null) }
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    if (results.isEmpty() && !searching) {
                        Text(
                            if (query.isBlank()) "Belum ada pelanggan" else "Tidak ditemukan",
                            fontSize = 11.sp, color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    results.forEach { c ->
                        CustomerRow(
                            name = c.name ?: "-",
                            sub = listOfNotNull(c.phone, c.email).joinToString(" · ").takeIf { it.isNotBlank() } ?: "",
                            selected = selectedCustomer?.id == c.id,
                            onClick = { onSelect(c) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

@Composable
private fun CustomerRow(name: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ApoPrimary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) ApoPrimary else TextPrimary)
            if (sub.isNotBlank()) Text(sub, fontSize = 10.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp), tint = ApoPrimary)
    }
}

// DISCOUNT PICKER DIALOG

@Composable
private fun DiscountPickerDialog(
    discounts: List<PosDiscountDto>,
    selectedDiscountId: Int?,
    subtotal: Double,
    onApply: (PosDiscountDto) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var applyError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Voucher", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selectedDiscountId != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Voucher aktif", fontSize = 11.sp, color = Success, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onClear, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                            Text("Hapus", fontSize = 11.sp, color = Error)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                }
                applyError?.let { err ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Error.copy(alpha = 0.08f)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(err, fontSize = 11.sp, color = Error)
                    }
                }
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    discounts.forEach { d ->
                        val isSelected = d.id == selectedDiscountId
                        val eligible = subtotal >= (d.minPurchase ?: 0.0)
                        val typeLabel = when (d.type?.lowercase()) {
                            "percentage" -> "Diskon ${d.value?.toInt() ?: 0}%"
                            else -> "Potongan ${formatIDR(d.value ?: 0.0)}"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Success.copy(alpha = 0.1f) else Color(0xFFF8FAFC))
                                .border(1.dp, if (isSelected) Success else Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                                .clickable(enabled = eligible && !isSelected) {
                                    applyError = null
                                    onApply(d)
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        d.name ?: d.code ?: "Voucher #${d.id}",
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (!eligible) TextMuted else TextPrimary,
                                    )
                                    if (d.code != null && d.name != null) {
                                        Spacer(Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0F2FE)).padding(horizontal = 4.dp, vertical = 1.dp),
                                        ) {
                                            Text(d.code, fontSize = 9.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(typeLabel, fontSize = 11.sp, color = if (!eligible) TextMuted else TextSecondary)
                                if ((d.minPurchase ?: 0.0) > 0) {
                                    Text("Min. ${formatIDR(d.minPurchase ?: 0.0)}", fontSize = 10.sp, color = if (!eligible) Error.copy(0.7f) else TextMuted)
                                }
                                if (d.maxDiscount != null) Text("Maks. ${formatIDR(d.maxDiscount)}", fontSize = 10.sp, color = TextMuted)
                                d.validUntil?.let { Text("s/d ${it.take(10)}", fontSize = 10.sp, color = TextMuted) }
                            }
                            Spacer(Modifier.width(8.dp))
                            if (isSelected) {
                                Icon(Icons.Filled.Check, null, modifier = Modifier.size(20.dp), tint = Success)
                            } else if (!eligible) {
                                Text("Kurang", fontSize = 9.sp, color = TextMuted)
                            } else {
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ApoPrimary).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("Pakai", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

// PROMO INFO DIALOG

@Composable
private fun PromoInfoDialog(
    promotions: List<PosPromotionDto>,
    cartProductIds: Set<String>,
    onDismiss: () -> Unit,
) {
    val relevant = promotions.filter { promo ->
        promo.promotionProducts?.any { pp -> pp.productId.toString() in cartProductIds } == true
    }
    val others = promotions.filter { it !in relevant }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promo Aktif", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (relevant.isEmpty() && others.isEmpty()) {
                    Text("Tidak ada promo saat ini.", fontSize = 12.sp, color = TextMuted)
                }
                if (relevant.isNotEmpty()) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Warning).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("Cocok di keranjang", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    relevant.forEach { promo -> PromoCard(promo, cartProductIds, highlight = true) }
                }
                if (others.isNotEmpty()) {
                    if (relevant.isNotEmpty()) HorizontalDivider(color = Color(0xFFE5E7EB))
                    Text("Promo lainnya", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                    others.forEach { promo -> PromoCard(promo, cartProductIds, highlight = false) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

@Composable
private fun PromoCard(promo: PosPromotionDto, cartProductIds: Set<String>, highlight: Boolean) {
    Surface(color = if (highlight) Warning.copy(alpha = 0.06f) else Color(0xFFF8FAFC), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(promo.name ?: "Promo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            promo.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 10.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            promo.promotionProducts?.take(5)?.forEach { pp ->
                val inCart = pp.productId.toString() in cartProductIds
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(if (inCart) "✓" else "•", fontSize = 10.sp, color = if (inCart) Success else TextMuted)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            buildString {
                                append(pp.name ?: "Produk")
                                pp.qtyRequired?.let { append(" (min $it)") }
                            },
                            fontSize = 10.sp,
                            color = if (inCart) TextPrimary else TextMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    pp.specialPrice?.let { sp -> Text(formatIDR(sp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Success) }
                }
            }
            promo.validUntil?.let { Text("Berlaku s/d ${it.take(10)}", fontSize = 9.sp, color = TextMuted) }
        }
    }
}

// HELPERS

@Composable
private fun PaymentMetricRow(
    label: String,
    value: String,
    labelWeight: FontWeight = FontWeight.Medium,
    valueWeight: FontWeight = FontWeight.SemiBold,
    valueColor: Color = TextPrimary,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, fontWeight = labelWeight, color = TextSecondary)
        Text(value, fontSize = 12.sp, fontWeight = valueWeight, color = valueColor)
    }
}

private fun compactCashLabel(amount: Double): String {
    val value = amount.toLong()
    return when {
        value >= 1_000_000L -> "Rp ${value / 1_000_000}jt"
        value >= 1_000L -> "Rp ${value / 1_000}rb"
        else -> "Rp $value"
    }
}
