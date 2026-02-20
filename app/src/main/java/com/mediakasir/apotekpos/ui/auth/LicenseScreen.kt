package com.mediakasir.apotekpos.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediakasir.apotekpos.ui.MainViewModel
import com.mediakasir.apotekpos.ui.theme.Primary
import com.mediakasir.apotekpos.ui.theme.Secondary

@Composable
fun LicenseScreen(
    viewModel: MainViewModel,
    onSuccess: () -> Unit
) {
    var licenseKey by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(error) {
        // error displayed in UI
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalPharmacy,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "MediKasir",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = "Sistem POS Apotek",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Divider()

                Text(
                    text = "Masukkan Kode Lisensi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = licenseKey,
                    onValueChange = {
                        licenseKey = it
                        viewModel.clearError()
                    },
                    label = { Text("Kode Lisensi") },
                    placeholder = { Text("APOTEK-DEMO-2024-XXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                Button(
                    onClick = {
                        if (licenseKey.isNotBlank()) {
                            viewModel.validateLicense(licenseKey.trim(), onSuccess)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading && licenseKey.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Aktivasi Lisensi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Demo hint
                TextButton(
                    onClick = { licenseKey = "APOTEK-DEMO-2024-1234" }
                ) {
                    Text("Gunakan Lisensi Demo", color = Primary)
                }
            }
        }
    }
}
