package com.ares.ewe_man.presentation.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ares.ewe_man.BuildConfig

@Composable
fun ProfileBuildFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF),
        )
        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { throw RuntimeException("Test Crash") },
            ) {
                Text(
                    text = "Forzar crash (debug)",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
