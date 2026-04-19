package com.mediakasir.apotekpos.ui.main.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mediakasir.apotekpos.data.model.CompoundRecipeDto
import com.mediakasir.apotekpos.ui.theme.Background
import com.mediakasir.apotekpos.ui.theme.Primary
import com.mediakasir.apotekpos.ui.theme.TextMuted
import com.mediakasir.apotekpos.ui.theme.TextPrimary
import com.mediakasir.apotekpos.ui.theme.TextSecondary
import com.mediakasir.apotekpos.utils.formatIDR

@Composable
fun RacikanDialog(
    recipes: List<CompoundRecipeDto>,
    isLoading: Boolean,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onAdd: (CompoundRecipeDto) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, recipes) {
        if (search.isBlank()) recipes
        else recipes.filter { it.name.contains(search, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(Color.Transparent),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Pilih Resep Racikan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Tutup", color = TextMuted)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Search bar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Background,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        BasicTextField(
                            value = search,
                            onValueChange = { search = it },
                            singleLine = true,
                            decorationBox = { inner ->
                                if (search.isEmpty()) {
                                    Text("Cari resep...", color = TextMuted, fontSize = 13.sp)
                                }
                                inner()
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            text = errorMessage,
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        FilledTonalButton(
                            onClick = onRetry,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Primary.copy(alpha = 0.12f)),
                        ) {
                            Text("Coba Lagi", color = Primary, fontSize = 13.sp)
                        }
                    }
                } else if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Tidak ada resep ditemukan", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.id }) { recipe ->
                            RacikanRecipeCard(recipe = recipe, onAdd = onAdd)
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RacikanRecipeCard(
    recipe: CompoundRecipeDto,
    onAdd: (CompoundRecipeDto) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                recipe.aturanPakai?.let {
                    Text(text = it, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (recipe.items.isNotEmpty()) {
                    Text(
                        text = recipe.items.joinToString(", ") { it.productName },
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatIDR(recipe.totalPrice),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                )
            }
            Spacer(Modifier.size(8.dp))
            FilledTonalButton(
                onClick = { onAdd(recipe) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Primary.copy(alpha = 0.1f)),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Tambah", tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("Tambah", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
