package com.mediakasir.apotekpos.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun NetworkCapabilities.isValidatedInternet(): Boolean =
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        // Emit current state immediately (only if validated)
        val current = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            ?.isValidatedInternet() == true
        trySend(current)

        val callback = object : ConnectivityManager.NetworkCallback() {
            // onAvailable fires before validation — don't emit true here;
            // wait for onCapabilitiesChanged to confirm NET_CAPABILITY_VALIDATED
            override fun onAvailable(network: Network) = Unit

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.isValidatedInternet())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)

        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
