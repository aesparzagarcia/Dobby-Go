package com.ares.ewe_man.presentation.ui.auth.otp

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe_man.presentation.viewmodel.auth.OtpViewModel

private val PhoneTeal = Color(0xFF14B8A6)
private val BackButtonSurface = Color(0xFFECECEC)
private val CircleOuterBorder = Color(0xFFD1D5DB)
private val CircleInnerBorder = Color(0xFFE5E7EB)
private val TimerLabelGray = Color(0xFF6B7280)
private val TimerValueGray = Color(0xFF374151)

private fun formatPhoneForDisplay(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    if (digits.length == 10) {
        return "+52 $digits"
    }
    val p = phone.trim()
    if (p.startsWith("+52") && p.length > 3) {
        return "+52 ${p.drop(3).filter { it.isDigit() }}"
    }
    return p.ifBlank { phone }
}

private fun formatMmSs(totalSeconds: Int): String {
    val m = totalSeconds.coerceAtLeast(0) / 60
    val s = totalSeconds.coerceAtLeast(0) % 60
    return "%d:%02d".format(m, s)
}

@Composable
fun OtpScreen(
    onVerified: () -> Unit,
    viewModel: OtpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val focusRequesters = remember { List(6) { FocusRequester() } }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }

    LaunchedEffect(uiState.code, uiState.isLoading, uiState.errorMessage) {
        if (uiState.code.length != 6) return@LaunchedEffect
        if (uiState.isLoading) return@LaunchedEffect
        if (uiState.errorMessage != null) return@LaunchedEffect
        viewModel.verifyCode(onVerified = onVerified)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            IconButton(
                onClick = { backDispatcher?.onBackPressed() },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BackButtonSurface),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Introduce el código que enviamos 👀",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                ),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "A TU NÚMERO DE CELULAR",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp,
                    fontSize = 11.sp,
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatPhoneForDisplay(viewModel.phone),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = PhoneTeal,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (index in 0 until 6) {
                    OtpDigitCell(
                        digit = uiState.digitSlots.getOrElse(index) { "" },
                        enabled = !uiState.isLoading,
                        focusRequester = focusRequesters[index],
                        onValueChange = { newValue ->
                            val nextFocus = viewModel.onSlotInput(index, newValue)
                            nextFocus?.let { focusRequesters[it].requestFocus() }
                        },
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            if (uiState.isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PhoneTeal)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Podrás solicitar un nuevo código en",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TimerLabelGray,
                    fontWeight = FontWeight.Normal,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatMmSs(uiState.remainingSeconds),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TimerValueGray,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun OtpDigitCell(
    digit: String,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
) {
    val displayChar = digit.take(1)

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, CircleOuterBorder, CircleShape)
            .padding(3.dp)
            .border(1.dp, CircleInnerBorder, CircleShape)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = displayChar,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    inner()
                }
            },
        )
    }
}
