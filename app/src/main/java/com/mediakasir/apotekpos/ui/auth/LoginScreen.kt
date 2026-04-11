package com.mediakasir.apotekpos.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mediakasir.apotekpos.BuildConfig
import com.mediakasir.apotekpos.R
import com.mediakasir.apotekpos.ui.MainViewModel
import com.mediakasir.apotekpos.ui.theme.ApoPrimary
import com.mediakasir.apotekpos.ui.theme.ApoPrimaryDark
import com.mediakasir.apotekpos.ui.theme.ApoSecondaryTeal
import com.mediakasir.apotekpos.ui.theme.AuthGradientEnd
import com.mediakasir.apotekpos.ui.theme.AuthGradientMid1
import com.mediakasir.apotekpos.ui.theme.AuthGradientMid2
import com.mediakasir.apotekpos.ui.theme.AuthGradientMid3
import com.mediakasir.apotekpos.ui.theme.AuthGradientStart
import com.mediakasir.apotekpos.ui.theme.CardSubtitle
import com.mediakasir.apotekpos.ui.theme.CardTitle
import com.mediakasir.apotekpos.ui.theme.InputFillEnd
import com.mediakasir.apotekpos.ui.theme.InputFillStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onSuccess: () -> Unit,
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as ComponentActivity
    val googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val useWideLayout = maxWidth >= 600.dp

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
                        end = Offset(w, h * 1.1f),
                    ),
                ),
        ) {
            if (useWideLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    LoginBrandingCarousel(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 28.dp, vertical = 24.dp)
                            .fillMaxWidth()
                            .widthIn(max = 400.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    ) {
                        LoginFormBody(
                            email = email,
                            onEmailChange = { email = it; viewModel.clearError() },
                            password = password,
                            onPasswordChange = { password = it; viewModel.clearError() },
                            passwordVisible = passwordVisible,
                            onPasswordVisibleToggle = { passwordVisible = !passwordVisible },
                            error = error,
                            isLoading = isLoading,
                            focusManager = focusManager,
                            googleWebClientId = googleWebClientId,
                            onGoogleClick = {
                                viewModel.clearError()
                                scope.launch {
                                    val res = requestGoogleIdToken(activity, googleWebClientId)
                                    res.fold(
                                        onSuccess = { token ->
                                            if (token != null) {
                                                viewModel.loginWithGoogleIdToken(token, onSuccess)
                                            }
                                        },
                                        onFailure = { e ->
                                            viewModel.reportAuthError(
                                                e.message ?: activity.getString(R.string.login_error_generic),
                                            )
                                        },
                                    )
                                }
                            },
                            onSubmit = {
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    viewModel.login(email.trim(), password, onSuccess)
                                }
                            },
                        )
                    }
                }
                }
            } else {
                Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LoginBrandingCarousel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp),
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 400.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                ) {
                    LoginFormBody(
                        email = email,
                        onEmailChange = { email = it; viewModel.clearError() },
                        password = password,
                        onPasswordChange = { password = it; viewModel.clearError() },
                        passwordVisible = passwordVisible,
                        onPasswordVisibleToggle = { passwordVisible = !passwordVisible },
                        error = error,
                        isLoading = isLoading,
                        focusManager = focusManager,
                        googleWebClientId = googleWebClientId,
                        onGoogleClick = {
                            viewModel.clearError()
                            scope.launch {
                                val res = requestGoogleIdToken(activity, googleWebClientId)
                                res.fold(
                                    onSuccess = { token ->
                                        if (token != null) {
                                            viewModel.loginWithGoogleIdToken(token, onSuccess)
                                        }
                                    },
                                    onFailure = { e ->
                                        viewModel.reportAuthError(
                                            e.message ?: activity.getString(R.string.login_error_generic),
                                        )
                                    },
                                )
                            }
                        },
                        onSubmit = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                viewModel.login(email.trim(), password, onSuccess)
                            }
                        },
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun LoginBrandingCarousel(modifier: Modifier = Modifier) {
    var slide by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4500)
            slide = (slide + 1) % 3
        }
    }

    val slides = listOf(
        Triple(
            Icons.Filled.LocalHospital,
            stringResource(R.string.app_name),
            stringResource(R.string.app_tagline),
        ),
        Triple(
            Icons.Filled.Vaccines,
            stringResource(R.string.premium_edition),
            stringResource(R.string.login_subtitle),
        ),
        Triple(
            Icons.Filled.Favorite,
            stringResource(R.string.wordmark_prefix) + stringResource(R.string.wordmark_suffix),
            stringResource(R.string.splash_footer),
        ),
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = slide,
            transitionSpec = {
                fadeIn(tween(480)) togetherWith fadeOut(tween(480))
            },
            label = "login_brand",
        ) { index ->
            val (icon, title, subtitle) = slides[index]
            BrandSlide(icon = icon, title = title, subtitle = subtitle)
        }
    }
}

@Composable
private fun BrandSlide(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(88.dp),
            tint = Color.White.copy(alpha = 0.92f),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoginFormBody(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleToggle: () -> Unit,
    error: String?,
    isLoading: Boolean,
    focusManager: androidx.compose.ui.focus.FocusManager,
    googleWebClientId: String,
    onGoogleClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.login_welcome_back),
            style = MaterialTheme.typography.headlineSmall,
            color = CardTitle,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = CardSubtitle,
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.login_email_label)) },
            placeholder = { Text(stringResource(R.string.login_email_placeholder)) },
            leadingIcon = {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = if (error != null) MaterialTheme.colorScheme.error else CardSubtitle,
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ApoPrimary,
                focusedLeadingIconColor = ApoPrimary,
                unfocusedContainerColor = InputFillStart,
                focusedContainerColor = Color.White,
                cursorColor = ApoPrimary,
            ),
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.login_password_label)) },
            placeholder = { Text(stringResource(R.string.login_password_placeholder)) },
            leadingIcon = {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (error != null) MaterialTheme.colorScheme.error else CardSubtitle,
                )
            },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibleToggle) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                    )
                }
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSubmit()
                },
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error, color = MaterialTheme.colorScheme.error) }
            } else {
                null
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ApoPrimary,
                focusedLeadingIconColor = ApoPrimary,
                unfocusedContainerColor = InputFillEnd,
                focusedContainerColor = Color.White,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = ApoPrimary,
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
            contentPadding = PaddingValues(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(ApoPrimary, ApoSecondaryTeal, ApoPrimaryDark),
                            start = Offset(0f, 0f),
                            end = Offset(500f, 500f),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        stringResource(R.string.login_submit),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.login_or),
                style = MaterialTheme.typography.labelMedium,
                color = CardSubtitle,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        val googleEnabled = googleWebClientId.isNotEmpty() && !isLoading
        OutlinedButton(
            onClick = onGoogleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = googleEnabled,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                stringResource(R.string.login_with_google),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        }
        if (googleWebClientId.isEmpty()) {
            Text(
                text = stringResource(R.string.login_google_setup_hint),
                style = MaterialTheme.typography.bodySmall,
                color = CardSubtitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
