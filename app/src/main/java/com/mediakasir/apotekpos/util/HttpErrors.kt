package com.mediakasir.apotekpos.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.HttpException

/** Pesan singkat untuk UI dari respons error HTTP (JSON Laravel atau teks/HTML). */
fun formatHttpException(e: HttpException, gson: Gson = Gson()): String {
    val code = e.code()
    val raw = e.response()?.errorBody()?.string()?.trim().orEmpty()
    if (raw.isEmpty()) return "HTTP $code"
    val jsonMsg = runCatching {
        val jo = gson.fromJson(raw, JsonObject::class.java)
        jo.get("message")?.asString?.trim()?.takeIf { it.isNotEmpty() }
    }.getOrNull()
    if (jsonMsg != null) return "$jsonMsg (HTTP $code)"
    val short = raw.lines().firstOrNull()?.take(120)?.trim().orEmpty()
    return if (short.isNotEmpty()) "$short (HTTP $code)" else "HTTP $code"
}

/** Parse error message dari HTTP response dengan cek error_code untuk pesan user-friendly. */
fun parseLoginError(e: HttpException, gson: Gson = Gson()): String {
    val code = e.code()
    val raw = e.response()?.errorBody()?.string()?.trim().orEmpty()
    if (raw.isEmpty()) {
        return when (code) {
            401 -> "Email atau password salah. Silakan coba lagi."
            403 -> "Akun Anda tidak memiliki akses. Hubungi administrator."
            404 -> "Server tidak ditemukan. Periksa kembali alamat server."
            else -> "Gagal login (HTTP $code)"
        }
    }
    
    // Try parse JSON untuk cari error_code
    val errorObj = runCatching {
        gson.fromJson(raw, JsonObject::class.java)
    }.getOrNull()
    
    if (errorObj != null) {
        val errorCode = errorObj.get("error_code")?.asString?.uppercase().orEmpty()
        val message = errorObj.get("message")?.asString?.trim().orEmpty()
        
        return when {
            errorCode == "INVALID_CREDENTIALS" || errorCode == "CREDENTIALS" -> 
                "Email atau password salah. Silakan periksa kembali."
            errorCode == "ACCOUNT_DISABLED" -> 
                "Akun Anda telah dinonaktifkan. Hubungi administrator."
            errorCode == "ACCOUNT_NOT_VERIFIED" -> 
                "Akun Anda belum diverifikasi. Periksa email Anda."
            errorCode == "LICENSE_EXPIRED" -> 
                "Lisensi Anda telah kadaluarsa. Hubungi support."
            errorCode == "ACCOUNT_LOCKED" -> 
                "Akun Anda terkunci. Hubungi administrator."
            message.isNotEmpty() -> message
            code == 401 -> "Email atau password salah."
            code == 403 -> "Akun tidak memiliki akses."
            else -> "Gagal login. Silakan coba lagi."
        }
    }
    
    // Fallback: return raw message dengan pembatasan panjang
    val short = raw.lines().firstOrNull()?.take(150)?.trim().orEmpty()
    return if (short.isNotEmpty()) short else "Gagal login. Silakan coba lagi."
}

fun formatApiThrowable(e: Throwable, gson: Gson = Gson()): String = when (e) {
    is HttpException -> formatHttpException(e, gson)
    else -> e.message?.trim()?.takeIf { it.isNotEmpty() } ?: "Terjadi kesalahan"
}
