package com.mediakasir.apotekpos.data.network

import com.mediakasir.apotekpos.data.repository.SessionRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Setiap request (setelah login): header `Authorization: Bearer` + token Sanctum, plus `Accept: application/json`.
 * Scoping cabang/partner di backend mengikuti token — bukan dari parameter cabang manual di URL.
 */
class AuthInterceptor @Inject constructor(
    private val sessionRepository: SessionRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sessionRepository.getToken() }
        val original = chain.request()

        val builder = original.newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")

        if (token != null && token.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $token")
        }

        // Tambahkan X-Device-ID jika tersedia dari SessionRepository
        val deviceId = runBlocking { sessionRepository.getDeviceId() }
        if (!deviceId.isNullOrEmpty()) {
            builder.addHeader("X-Device-ID", deviceId)
        }

        val request = builder.build()
        return chain.proceed(request)
    }
}
