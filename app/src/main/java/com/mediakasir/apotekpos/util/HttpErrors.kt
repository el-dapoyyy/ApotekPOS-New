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

fun formatApiThrowable(e: Throwable, gson: Gson = Gson()): String = when (e) {
    is HttpException -> formatHttpException(e, gson)
    else -> e.message?.trim()?.takeIf { it.isNotEmpty() } ?: "Terjadi kesalahan"
}
