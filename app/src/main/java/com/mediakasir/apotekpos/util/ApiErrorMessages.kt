package com.mediakasir.apotekpos.util

import android.content.Context
import com.mediakasir.apotekpos.R
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Context.mapNetworkOrGenericError(e: Throwable): String {
    val msg = e.message.orEmpty()
    return when {
        e is UnknownHostException -> getString(R.string.error_network_unreachable)
        msg.contains("Unable to resolve host", ignoreCase = true) ->
            getString(R.string.error_network_unreachable)
        e is SocketTimeoutException -> getString(R.string.error_network_timeout)
        msg.contains("timeout", ignoreCase = true) -> getString(R.string.error_network_timeout)
        else -> e.message?.takeIf { it.isNotBlank() } ?: getString(R.string.login_error_generic)
    }
}
