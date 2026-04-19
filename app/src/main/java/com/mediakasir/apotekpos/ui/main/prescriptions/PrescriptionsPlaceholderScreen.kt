package com.mediakasir.apotekpos.ui.main.prescriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PrescriptionsPlaceholderScreen(
    viewModel: PrescriptionsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Antrian & resep", style = MaterialTheme.typography.titleLarge)
        Text(
            "Data diambil dari endpoint resep API POS.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.LocalPharmacy, contentDescription = null, modifier = Modifier.padding(bottom = 8.dp))
                    Text("Belum ada data resep")
                }
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(items) { row ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text(row.prescriptionNumber ?: "Resep #${row.id}", fontWeight = FontWeight.SemiBold)
                    Text("Pasien: ${row.customerName ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    Text("Dokter: ${row.doctorName ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    Text("Status: ${row.status ?: "-"}", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
            }
        }
    }
}
