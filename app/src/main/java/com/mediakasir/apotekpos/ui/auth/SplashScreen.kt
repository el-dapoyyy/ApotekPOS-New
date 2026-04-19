package com.mediakasir.apotekpos.ui.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalHospital
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
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
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
    var progressStep by remember { mutableStateOf("") }
    var progressValue by remember { mutableStateOf(0f) }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 550),
        label = "splashContentFade",
    )
    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "logoScale",
    )
    val progressAnim by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = tween(durationMillis = 350),
        label = "progressAnim",
    )

    LaunchedEffect(Unit) {
        visible = true
        progressStep = "Menyiapkan aplikasi…"
        progressValue = 0.2f
        delay(250L)
        progressStep = "Memeriksa sesi login…"
        progressValue = 0.5f
        val route = viewModel.resolveRouteAfterSplash()
        progressStep = "Memuat data apotek…"
        progressValue = 0.85f
        delay(300L)
        progressStep = "Siap!"
        progressValue = 1f
        delay(180L)
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
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo_horizontal),
                contentDescription = "App Logo",
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(110.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    },
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim)
                        .height(3.dp)
                        .background(ApoGold),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = progressStep.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(150)),
            ) {
                Text(
                    text = progressStep,
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
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
