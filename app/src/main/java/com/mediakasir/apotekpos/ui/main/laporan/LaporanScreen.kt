package com.mediakasir.apotekpos.ui.main.laporan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.data.model.LicenseInfo

@Composable
fun LaporanScreen(
    license: LicenseInfo?,
    user: UserInfo?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Halaman Laporan",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
