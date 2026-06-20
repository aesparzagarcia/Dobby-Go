package com.ares.ewe_man.presentation.ui.auth.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe_man.core.theme.DobbyPureScale
import com.ares.ewe_man.presentation.viewmodel.auth.PhoneViewModel
import kotlinx.coroutines.delay

private val SubtitleBlack = Color(0xFF111111)

@Composable
fun PhoneScreen(
    onCodeSent: (phone: String) -> Unit,
    viewModel: PhoneViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val phoneFieldFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        phoneFieldFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Ingresa tu número celular",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 24.sp,
                lineHeight = 30.sp,
            ),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "TE ENVIAREMOS UN CÓDIGO PARA CONFIRMARLO",
            style = MaterialTheme.typography.labelSmall.copy(
                color = SubtitleBlack,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
                fontSize = 11.sp,
            ),
        )

        Spacer(modifier = Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.12f),
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "🇲🇽",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "+52",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                    ),
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            BasicTextField(
                value = uiState.nationalDigits,
                onValueChange = viewModel::onPhoneChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(phoneFieldFocusRequester),
                singleLine = true,
                enabled = !uiState.isLoading,
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (uiState.nationalDigits.isEmpty()) {
                            Text(
                                text = "Tu número celular",
                                style = TextStyle(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        inner()
                    }
                },
            )
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = DobbyPureScale.Onyx)
            }
        } else {
            Button(
                onClick = { viewModel.sendCode(onCodeSent) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DobbyPureScale.Onyx,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sms,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Recibir código por SMS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
