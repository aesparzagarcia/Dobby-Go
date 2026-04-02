package com.ares.ewe_man.presentation.ui.profile

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ares.ewe_man.data.remote.model.DeliveryMissionDto
import com.ares.ewe_man.presentation.viewmodel.profile.ProfileViewModel
import java.text.NumberFormat
import java.util.Locale

private fun formatInt(n: Int): String = NumberFormat.getInstance(Locale("es", "MX")).format(n)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                actions = {
                    TextButton(
                        onClick = { viewModel.loadProfile() },
                        enabled = !uiState.isLoading,
                    ) {
                        Text("Actualizar")
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading && uiState.profile == null -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null && uiState.profile == null -> {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadProfile() }) {
                        Text("Reintentar")
                    }
                }
            }
            else -> {
                val p = uiState.profile
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scroll)
                        .padding(20.dp),
                ) {
                    if (p != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val photoUrl = resolveProfileImageUrl(p.profilePhotoUrl)
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (photoUrl != null) {
                                Log.d("ArmandoLog","url de foto de perfil: $photoUrl")
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = p.name.ifBlank { "Foto de perfil" },
                                    modifier = Modifier
                                        .size(112.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(112.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = p.name.ifBlank { "Repartidor" },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val st = p.status.uppercase()
                        Text(
                            text = "Estado: ${connectionStatusLabel(st)}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        if (st == "ON_DELIVERY") {
                            Text(
                                text = "Termina la entrega en curso para cambiar tu disponibilidad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = if (st == "ONLINE") "Disponible para pedidos" else "No disponible",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Switch(
                                    checked = st == "ONLINE",
                                    onCheckedChange = { on ->
                                        if (!uiState.statusUpdating) viewModel.setConnectionStatus(on)
                                    },
                                    enabled = !uiState.statusUpdating,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nivel: ${levelEmoji(p.levelKey)} ${levelDisplayName(p.levelKey)}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val next = p.xpForNextLevel
                        val span = if (next != null) (next - p.xpAtCurrentLevel).coerceAtLeast(1) else 1
                        val within = (p.xp - p.xpAtCurrentLevel).coerceAtLeast(0)
                        val progress = if (next == null) 1f else (within.toFloat() / span).coerceIn(0f, 1f)
                        Text(
                            text = if (next != null) {
                                "XP: ${formatInt(p.xp)} / ${formatInt(next)}"
                            } else {
                                "XP: ${formatInt(p.xp)} (nivel máximo)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (next != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Rating: ⭐ ${String.format(Locale.US, "%.1f", p.rating)} (${p.ratingCount} valoraciones)",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Racha: 🔥 ${p.currentStreakDays} días",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Entregas: ${formatInt(p.totalDeliveries)}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Misiones",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        p.missions.forEach { m ->
                            MissionRow(mission = m)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cerrar sesión")
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionRow(mission: DeliveryMissionDto) {
    val icon = when {
        mission.completed -> "✅"
        else -> "⏳"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "$icon ${mission.title}",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${mission.progress} / ${mission.goal}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (mission.goal > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = {
                        (mission.progress.toFloat() / mission.goal).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                )
            }
        }
    }
}
