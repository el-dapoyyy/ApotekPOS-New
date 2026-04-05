package com.mediakasir.apotekpos.ui.main.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediakasir.apotekpos.ui.theme.Border
import com.mediakasir.apotekpos.ui.theme.Error
import com.mediakasir.apotekpos.ui.theme.PosWebPrimary
import com.mediakasir.apotekpos.ui.theme.PosWebPrimaryDark
import com.mediakasir.apotekpos.ui.theme.Primary
import com.mediakasir.apotekpos.ui.theme.PrimaryDark
import com.mediakasir.apotekpos.ui.theme.Secondary
import com.mediakasir.apotekpos.ui.theme.Subtle
import com.mediakasir.apotekpos.ui.theme.Success
import com.mediakasir.apotekpos.ui.theme.SurfaceColor
import com.mediakasir.apotekpos.ui.theme.TextMuted
import com.mediakasir.apotekpos.ui.theme.TextPrimary
import com.mediakasir.apotekpos.ui.theme.TextSecondary
import com.mediakasir.apotekpos.utils.formatIDR

val PAYMENT_METHODS = listOf("Tunai", "Transfer", "QRIS", "Debit", "Kredit")

@Composable
fun CartPanelContent(
    cart: List<CartItem>,
    payments: List<PaymentEntry>,
    discount: String,
    viewModel: POSViewModel,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
    showTopClose: Boolean = false,
    onDismiss: (() -> Unit)? = null,
) {
    val subtotal = viewModel.getSubtotal()
    val discountAmt = viewModel.getDiscountAmt()
    val total = viewModel.getTotal()
    val totalPaid = viewModel.getTotalPaid()
    val change = viewModel.getChange()
    val isProcessing by viewModel.isProcessing.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "TRANSAKSI ${viewModel.getCartCount()} ITEM",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            if (showTopClose && onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup")
                }
            }
        }
        HorizontalDivider()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextMuted)
                        Text("Keranjang kosong", color = TextMuted, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) { },
                    shape = RoundedCornerShape(10.dp),
                    color = Subtle,
                    border = BorderStroke(1.dp, Border),
                ) {
                    Text(
                        "PILIH PELANGGAN",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                    )
                }
                cart.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.product.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "${formatIDR(item.product.sellPrice)} × ${item.qty} = ${formatIDR(item.product.sellPrice * item.qty)}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(
                                onClick = { viewModel.updateQty(item.product.id, -1) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                item.qty.toString(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 24.dp),
                                textAlign = TextAlign.Center,
                            )
                            IconButton(
                                onClick = { viewModel.updateQty(item.product.id, 1) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { viewModel.removeFromCart(item.product.id) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = Error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    HorizontalDivider()
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", color = TextSecondary)
                    Text(formatIDR(subtotal), fontWeight = FontWeight.SemiBold)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("DISKON GLOBAL", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = discount,
                        onValueChange = { viewModel.setDiscount(it) },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.End),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TUSLA", color = TextSecondary, fontSize = 12.sp)
                    Text("—", color = TextMuted, fontSize = 13.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("EMBALSE", color = TextSecondary, fontSize = 12.sp)
                    Text("—", color = TextMuted, fontSize = 13.sp)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = PosWebPrimary.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                        Text(
                            formatIDR(total),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = PosWebPrimaryDark,
                        )
                    }
                }

                Text("Isi cepat (baris pertama)", fontSize = 12.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(50_000, 100_000, 200_000).forEach { v ->
                        TextButton(onClick = {
                            val first = payments.firstOrNull() ?: return@TextButton
                            viewModel.updatePayment(first.id, amount = v.toString())
                        }) {
                            Text(formatIDR(v.toDouble()), fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = {
                        val first = payments.firstOrNull() ?: return@TextButton
                        viewModel.updatePayment(first.id, amount = kotlin.math.ceil(total).toInt().toString())
                    }) {
                        Text("Pas", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("SISTEM PEMBAYARAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    TextButton(onClick = { viewModel.addPayment() }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("+ SPLIT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                payments.forEach { payment ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Subtle)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PAYMENT_METHODS.forEach { method ->
                                    Surface(
                                        modifier = Modifier.clickable { viewModel.updatePayment(payment.id, method = method) },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (payment.method == method) PosWebPrimary else SurfaceColor,
                                        border = if (payment.method != method) BorderStroke(1.dp, Border) else null,
                                    ) {
                                        Text(
                                            method,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontSize = 12.sp,
                                            color = if (payment.method == method) Color.White else TextSecondary,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = payment.amount,
                                    onValueChange = { viewModel.updatePayment(payment.id, amount = it) },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Jumlah") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Dibayar", color = TextSecondary)
                    Text(
                        formatIDR(totalPaid),
                        color = if (totalPaid >= total) Success else Error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kembalian", color = TextSecondary)
                    Text(
                        formatIDR(maxOf(0.0, change)),
                        color = if (change >= 0) Success else Error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (cart.isNotEmpty()) {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = onCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isProcessing && totalPaid >= total,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PosWebPrimary,
                        disabledContainerColor = TextMuted,
                    ),
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("SELESAIKAN PESANAN", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
