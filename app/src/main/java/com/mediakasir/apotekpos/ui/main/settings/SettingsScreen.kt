package com.mediakasir.apotekpos.ui.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.data.network.ApiService
import com.mediakasir.apotekpos.data.model.ChangePinRequest
import com.mediakasir.apotekpos.ui.MainViewModel
import com.mediakasir.apotekpos.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    license: LicenseInfo?,
    user: UserInfo?,
    viewModel: MainViewModel,
    api: ApiService,
    onLogout: () -> Unit,
    onClearLicense: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .padding(16.dp)
        ) {
            Text("Pengaturan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalPharmacy, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Informasi Apotek", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoRow("Apotek", license?.pharmacyName ?: "-")
                    InfoRow("Cabang", license?.branchName ?: "-")
                    InfoRow("Alamat", license?.address ?: "-")
                    InfoRow("Telepon", license?.phone ?: "-")
                }
            }

            // User Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Informasi Pengguna", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoRow("Username", user?.username ?: "-")
                    InfoRow("Nama", user?.fullName ?: "-")
                    InfoRow("Role", user?.role?.uppercase() ?: "-")
                }
            }

            // Change PIN
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ganti PIN", fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { if (it.length <= 8) oldPin = it },
                        label = { Text("PIN Lama") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 8) newPin = it },
                        label = { Text("PIN Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 8) confirmPin = it },
                        label = { Text("Konfirmasi PIN Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = pinError != null,
                        supportingText = pinError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                    )

                    if (pinMessage != null) {
                        Text(pinMessage!!, color = Success, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            pinError = null
                            pinMessage = null
                            if (newPin != confirmPin) {
                                pinError = "PIN baru tidak cocok"
                                return@Button
                            }
                            if (newPin.length < 4) {
                                pinError = "PIN minimal 4 digit"
                                return@Button
                            }
                            scope.launch {
                                try {
                                    api.changePin(ChangePinRequest(user?.username ?: "", oldPin, newPin))
                                    pinMessage = "PIN berhasil diubah"
                                    oldPin = ""; newPin = ""; confirmPin = ""
                                } catch (e: Exception) {
                                    pinError = e.message ?: "Gagal mengubah PIN"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = oldPin.isNotBlank() && newPin.isNotBlank() && confirmPin.isNotBlank()
                    ) {
                        Text("Ganti PIN")
                    }
                }
            }

            // Actions
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Warning)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Logout")
            }

            OutlinedButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hapus Data Lisensi")
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Anda yakin ingin keluar?") },
            confirmButton = {
                Button(onClick = { showLogoutDialog = false; onLogout() }) { Text("Logout") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") } }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Hapus Data Lisensi") },
            text = { Text("Ini akan menghapus semua data sesi dan kembali ke layar aktivasi lisensi.") },
            confirmButton = {
                Button(
                    onClick = { showClearDialog = false; onClearLicense() },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text("$label:", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
    }
}
