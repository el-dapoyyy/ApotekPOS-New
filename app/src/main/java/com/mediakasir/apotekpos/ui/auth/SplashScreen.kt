package com.mediakasir.apotekpos.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mediakasir.apotekpos.R
import com.mediakasir.apotekpos.ui.MainViewModel
import com.mediakasir.apotekpos.ui.theme.ApoGold
import com.mediakasir.apotekpos.ui.theme.AuthGradientEnd
import com.mediakasir.apotekpos.ui.theme.AuthGradientMid1
import com.mediakasir.apotekpos.ui.theme.AuthGradientMid2
import com.mediakasir.apotekpos.ui.theme.AuthGradientMid3
import com.mediakasir.apotekpos.ui.theme.AuthGradientStart
import com.mediakasir.apotekpos.ui.theme.WordmarkAccent
import kotlin.math.max

@Composable
fun SplashScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 550),
        label = "splashContentFade",
    )

    LaunchedEffect(Unit) {
        visible = true
        val route = viewModel.resolveRouteAfterSplash()
        onNavigate(route)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to AuthGradientStart,
                            0.25f to AuthGradientMid1,
                            0.5f to AuthGradientMid2,
                            0.75f to AuthGradientMid3,
                            1f to AuthGradientEnd,
                        ),
                        start = Offset.Zero,
                        end = Offset(w, h * 1.15f),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ApoGold.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.2f, h * 0.15f),
                        radius = max(w, h) * 0.45f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            WordmarkAccent.copy(alpha = 0.1f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.85f, h * 0.85f),
                        radius = max(w, h) * 0.4f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalHospital,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = MaterialTheme.typography.displayLarge.fontSize,
                        ),
                    ) {
                        append(stringResource(R.string.wordmark_prefix))
                    }
                    withStyle(
                        style = SpanStyle(
                            color = WordmarkAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = MaterialTheme.typography.displayLarge.fontSize,
                        ),
                    ) {
                        append(stringResource(R.string.wordmark_suffix))
                    }
                },
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF99F6E4).copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.premium_edition),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
        }

        Text(
            text = stringResource(R.string.splash_footer),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            textAlign = TextAlign.Center,
        )
    }
}
