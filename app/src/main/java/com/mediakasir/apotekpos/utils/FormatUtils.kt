package com.mediakasir.apotekpos.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

fun formatIDR(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ").trim()
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
