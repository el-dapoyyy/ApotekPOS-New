package com.mediakasir.apotekpos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val ColorOffline = Color(0xFFD32F2F) // merah
private val ColorSyncing = Color(0xFFF57F17) // kuning/amber
private val ColorOnline  = Color(0xFF2E7D32) // hijau gelap

private val DotOffline   = Color(0xFFFF5252)
private val DotSyncing   = Color(0xFFFFD740)
private val DotOnline    = Color(0xFF69F0AE)

private enum class StatusState { OFFLINE, SYNCING, ONLINE }

/**
 * Small coloured dot to overlay near the bell icon.
 *  🟢 Online · 🔴 Offline · 🟡 Syncing
 */
@Composable
fun NetworkStatusDot(
    isOnline: Boolean,
    pendingCount: Int,
    modifier: Modifier = Modifier,
) {
    var isSyncing by remember { mutableStateOf(false) }
    var prevOnline by remember { mutableStateOf(isOnline) }

    LaunchedEffect(isOnline) {
        if (!prevOnline && isOnline && pendingCount > 0) {
            isSyncing = true
            delay(3_500L)
            isSyncing = false
        }
        prevOnline = isOnline
    }

    val state = when {
        !isOnline -> StatusState.OFFLINE
        isSyncing -> StatusState.SYNCING
        else      -> StatusState.ONLINE
    }

    val dotColor by animateColorAsState(
        targetValue = when (state) {
            StatusState.OFFLINE -> DotOffline
            StatusState.SYNCING -> DotSyncing
            StatusState.ONLINE  -> DotOnline
        },
        animationSpec = tween(500),
        label = "dotColor",
    )

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(dotColor),
    )
}

/**
 * Slim status bar always visible at the top of the app.
 *
 * States:
 *  - Offline  → merah  + dot merah  + teks offline + jumlah pending
 *  - Syncing  → kuning + dot kuning + "Menyinkronkan…"  (3 detik setelah kembali online)
 *  - Online   → hijau  + dot hijau  + "Online"
 */
@Composable
fun NetworkStatusBanner(
    isOnline: Boolean,
    pendingCount: Int,
) {
    var isSyncing by remember { mutableStateOf(false) }
    var prevOnline by remember { mutableStateOf(isOnline) }

    LaunchedEffect(isOnline) {
        if (!prevOnline && isOnline && pendingCount > 0) {
            isSyncing = true
            delay(3_500L)
            isSyncing = false
        }
        prevOnline = isOnline
    }

    val state = when {
        !isOnline -> StatusState.OFFLINE
        isSyncing -> StatusState.SYNCING
        else      -> StatusState.ONLINE
    }

    val bgColor by animateColorAsState(
        targetValue = when (state) {
            StatusState.OFFLINE -> ColorOffline
            StatusState.SYNCING -> ColorSyncing
            StatusState.ONLINE  -> ColorOnline
        },
        animationSpec = tween(500),
        label = "netBg",
    )
    val dotColor by animateColorAsState(
        targetValue = when (state) {
            StatusState.OFFLINE -> DotOffline
            StatusState.SYNCING -> DotSyncing
            StatusState.ONLINE  -> DotOnline
        },
        animationSpec = tween(500),
        label = "netDot",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(7.dp))
            Icon(
                imageVector = when (state) {
                    StatusState.OFFLINE -> Icons.Outlined.CloudOff
                    StatusState.SYNCING -> Icons.Outlined.Sync
                    StatusState.ONLINE  -> Icons.Outlined.CloudQueue
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = when (state) {
                    StatusState.OFFLINE -> if (pendingCount > 0)
                        "Offline  ·  $pendingCount transaksi tersimpan lokal"
                    else
                        "Offline  ·  Tidak ada koneksi internet"
                    StatusState.SYNCING -> "Menyinkronkan $pendingCount transaksi…"
                    StatusState.ONLINE  -> "Online"
                },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            )
        }

        if (state == StatusState.OFFLINE && pendingCount > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$pendingCount pending",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
