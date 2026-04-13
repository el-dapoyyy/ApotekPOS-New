package com.mediakasir.apotekpos.ui.main.pos

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediakasir.apotekpos.ui.theme.ApoPrimary
import com.mediakasir.apotekpos.ui.theme.ApoPrimaryDark
import com.mediakasir.apotekpos.ui.theme.Error
import com.mediakasir.apotekpos.ui.theme.InputBorder
import com.mediakasir.apotekpos.ui.theme.Success
import com.mediakasir.apotekpos.ui.theme.TextMuted
import com.mediakasir.apotekpos.ui.theme.TextPrimary
import com.mediakasir.apotekpos.ui.theme.TextSecondary
import com.mediakasir.apotekpos.utils.formatIDR
import com.mediakasir.apotekpos.utils.parseMoneyInputToDouble

/**
 * Payment panel composable for the right sidebar of POS screen.
 * Displays payment breakdown, quick cash buttons, and checkout action.
 */
@Composable
fun PaymentPanel(
    cart: List<CartItem>,
    payments: List<PaymentEntry>,
    discountLabel: String?,
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

    var manualCashInput by remember { mutableStateOf("") }

    val baseQuickAmounts = listOf(5_000.0, 10_000.0, 20_000.0, 50_000.0, 100_000.0, 200_000.0)
    val quickAmounts = buildList {
        if (remaining > 0.0) add(remaining)
        addAll(baseQuickAmounts.filter { it >= remaining && it > 0.0 })
    }.distinct().sorted()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                "Pembayaran",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${cart.size} Item",
                fontSize = 11.sp,
                color = TextSecondary,
            )
        }

        HorizontalDivider(Modifier.fillMaxWidth(), color = Color(0xFFE5E7EB))

        // Breakdown
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Subtotal", fontSize = 12.sp, color = TextSecondary)
                Text(formatIDR(subtotal), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }

            // Discount
            if (discountAmt > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Diskon${if (discountLabel != null) " (${discountLabel})" else ""}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                    Text(
                        "- ${formatIDR(discountAmt)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Success,
                    )
                }
            }

            HorizontalDivider(Modifier.fillMaxWidth(), color = Color(0xFFE5E7EB))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    formatIDR(total),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ApoPrimary,
                )
            }

            val activePromoLabels = cart.mapNotNull { it.product.promoLabel?.takeIf { v -> v.isNotBlank() } }.distinct()
            if (discountLabel != null || activePromoLabels.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Voucher/Promo Aktif", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        discountLabel?.let {
                            Text("Voucher: $it", fontSize = 11.sp, color = TextPrimary)
                        }
                        activePromoLabels.take(3).forEach { promo ->
                            Text("Promo: $promo", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        HorizontalDivider(Modifier.fillMaxWidth(), color = Color(0xFFE5E7EB))

        // Quick Cash Buttons (Indonesian denominations: 5K, 10K, 20K, 50K, 100K)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                "Uang Tunai Cepat",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
            )
            Spacer(Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(quickAmounts) { amount ->
                    OutlinedButton(
                        onClick = {
                            viewModel.setCashAmount(amount)
                            manualCashInput = ""
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
                    ) {
                        Text(
                            if (amount == remaining && remaining > 0.0) "Pas" else formatIDR(amount).replace("Rp.", "").trim(),
                            fontSize = 10.sp,
                            color = TextPrimary,
                        )
                    }
                }
            }
        }

        HorizontalDivider(Modifier.fillMaxWidth(), color = Color(0xFFE5E7EB))

        // Manual Cash Input
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                "atau Masukkan Manual",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
            )
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = manualCashInput,
                onValueChange = {
                    manualCashInput = it
                    val parsedAmount = parseMoneyInputToDouble(it)
                    if (parsedAmount != null && parsedAmount > 0) {
                        viewModel.setCashAmount(parsedAmount)
                    }
                },
                placeholder = { Text("Contoh: 100000", fontSize = 11.sp, color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF3F4F6),
                    unfocusedContainerColor = Color(0xFFF3F4F6),
                    focusedBorderColor = ApoPrimary,
                    unfocusedBorderColor = InputBorder,
                ),
                shape = RoundedCornerShape(10.dp),
            )
        }

        HorizontalDivider(Modifier.fillMaxWidth(), color = Color(0xFFE5E7EB))

        // Change calculation (Uang Pas)
        if (totalPaid > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Uang Diterima", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        formatIDR(totalPaid),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Kembalian", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        formatIDR(maxOf(0.0, change)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (change >= 0) ApoPrimaryDark else Error,
                    )
                }

                if (change < 0) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Error.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            "Kurang: ${formatIDR(kotlin.math.abs(change))}",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Error,
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.fillMaxWidth(), color = Color(0xFFE5E7EB))
        }

        // Bayar Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing && cart.isNotEmpty() && change >= 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ApoPrimary,
                    disabledContainerColor = Color(0xFFD1D5DB),
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Memproses...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bayar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
