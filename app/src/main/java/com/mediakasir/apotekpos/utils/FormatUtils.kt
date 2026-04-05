package com.mediakasir.apotekpos.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

fun formatIDR(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ").trim()
}

/** Tampilan field angka (tanpa prefiks Rp), mis. 200000 → "200.000". */
fun formatDigitsAsIndonesianNumber(digitsOnly: String): String {
    if (digitsOnly.isEmpty()) return ""
    val n = digitsOnly.toLongOrNull() ?: return ""
    return NumberFormat.getNumberInstance(Locale("id", "ID")).format(n)
}

/** Terima input mentah / terformat; hanya digit yang dipakai (rupiah tanpa desimal). */
fun parseMoneyInputToDouble(raw: String): Double? {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return null
    return digits.toLongOrNull()?.toDouble()
}

fun formatDate(dateStr: String): String {
    return try {
        val inputFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFmt.parse(dateStr)
        if (date != null) outputFmt.format(date) else dateStr
    } catch (e: Exception) { dateStr }
}

fun formatDateTime(dateStr: String): String {
    return try {
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val outputFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = inputFmt.parse(dateStr)
        if (date != null) outputFmt.format(date) else dateStr
    } catch (e: Exception) { dateStr }
}
