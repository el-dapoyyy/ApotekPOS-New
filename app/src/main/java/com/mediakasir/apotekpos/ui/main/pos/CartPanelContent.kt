package com.mediakasir.apotekpos.ui.main.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.ShoppingCart
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

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Transaksi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    "${viewModel.getCartCount()} Item",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
            if (showTopClose && onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup")
                }
            }
        }
        
        // MIDDLE CONTENT (Scrollable)
        Box(modifier = Modifier.weight(1f)) {
            if (cart.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = TextMuted
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Keranjang kosong", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
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
                                    modifier = Modifier.size(28.dp),
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
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.removeFromCart(item.product.id) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = Subtle)
                    }

                    Spacer(Modifier.height(8.dp))
                    // DISKON GLOBAL & PEMBAYARAN HANYA TAMPIL JIKA ADA BARANG
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("DISKON", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        OutlinedTextField(
                            value = discount,
                            onValueChange = { viewModel.setDiscount(it) },
                            modifier = Modifier.width(120.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.End),
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("PEMBAYARAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        Text("Total Dibayarkan", color = TextSecondary)
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
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // BOTTOM SUMMARY & CHECKOUT BUTTON (ALWAYS VISIBLE)
        Surface(
            color = Color.White,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                HorizontalDivider(color = Subtle)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", color = TextPrimary, fontSize = 15.sp)
                    Text(if (cart.isEmpty()) "—" else formatIDR(subtotal), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(if (cart.isEmpty()) "—" else formatIDR(total), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !isProcessing && cart.isNotEmpty() && totalPaid >= total,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PosWebPrimary.copy(alpha = if (cart.isEmpty()) 0.5f else 1f),
                        disabledContainerColor = PosWebPrimary.copy(alpha = 0.4f),
                        disabledContentColor = Color.White
                    ),
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Checkout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
