package com.mediakasir.apotekpos.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStatus @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

fun Throwable.isLikelyNetworkFailure(): Boolean {
    if (this is java.net.UnknownHostException) return true
    if (this is java.io.IOException) return true
    val msg = message?.lowercase().orEmpty()
    return msg.contains("unable to resolve host") ||
        msg.contains("failed to connect") ||
        msg.contains("timeout") ||
        msg.contains("network")
}
