package com.ares.ewe_man.presentation.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ares.ewe_man.core.theme.DobbyGoColors
import com.ares.ewe_man.data.remote.model.DeliveryMissionDto
import com.ares.ewe_man.data.remote.model.DeliveryProfileDto
import com.ares.ewe_man.presentation.viewmodel.profile.ProfileViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private fun formatInt(n: Int): String = NumberFormat.getInstance(Locale("es", "MX")).format(n)

private fun missionXpReward(missionId: String): Int = when (missionId) {
    "deliveries_today" -> 20
    "quick_deliveries_today" -> 30
    else -> 10
}

private fun missionSubtitle(missionId: String): String? = when (missionId) {
    "quick_deliveries_today" -> "Entregas en ≤ 30 min en ruta"
    else -> null
}

private fun missionProgressLabel(mission: DeliveryMissionDto): String =
    "${mission.progress} / ${mission.goal} entregas"

private fun successRatePercent(rating: Double, totalDeliveries: Int): Int? {
    if (totalDeliveries == 0) return null
    return ((rating / 5.0) * 100.0).roundToInt().coerceIn(0, 100)
}

private fun successRateLabel(percent: Int): String = when {
    percent >= 95 -> "excelente"
    percent >= 85 -> "muy buena"
    else -> "buena"
}

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onConnectionStatusChanged: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.connectionStatusUpdated.collect {
            onConnectionStatusChanged()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DobbyGoColors.Background,
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ProfileScreenHeader(
                isLoading = uiState.isLoading,
                onRefresh = { viewModel.loadProfile() },
            )

            when {
                uiState.isLoading && uiState.profile == null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = DobbyGoColors.Purple)
                    }
                }
                uiState.errorMessage != null && uiState.profile == null -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
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
                        Button(
                            onClick = { viewModel.loadProfile() },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = DobbyGoColors.Purple,
                            ),
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scroll)
                            .padding(horizontal = 20.dp),
                    ) {
                        uiState.profile?.let { profile ->
                            Spacer(modifier = Modifier.height(16.dp))
                            ProfileMainCard(
                                profile = profile,
                                statusUpdating = uiState.statusUpdating,
                                onToggleAvailability = { viewModel.setConnectionStatus(it) },
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Misiones",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = DobbyGoColors.TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            profile.missions.forEachIndexed { index, mission ->
                                ProfileMissionCard(mission = mission)
                                if (index < profile.missions.lastIndex) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DobbyGoColors.Purple),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = DobbyGoColors.Purple,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cerrar sesión",
                                fontWeight = FontWeight.SemiBold,
                                color = DobbyGoColors.Purple,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreenHeader(
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Perfil",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = DobbyGoColors.TextPrimary,
        )
        TextButton(
            onClick = onRefresh,
            enabled = !isLoading,
        ) {
            Text(
                text = "Actualizar",
                color = DobbyGoColors.Purple,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProfileMainCard(
    profile: DeliveryProfileDto,
    statusUpdating: Boolean,
    onToggleAvailability: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyGoColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileUserHeaderRow(
                profile = profile,
                statusUpdating = statusUpdating,
                onToggleAvailability = onToggleAvailability,
            )
            Spacer(modifier = Modifier.height(16.dp))
            ProfileLevelSection(profile = profile)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DobbyGoColors.Border)
            Spacer(modifier = Modifier.height(12.dp))
            ProfileStatsRow(profile = profile)
        }
    }
}

@Composable
private fun ProfileUserHeaderRow(
    profile: DeliveryProfileDto,
    statusUpdating: Boolean,
    onToggleAvailability: (Boolean) -> Unit,
) {
    val status = profile.status.uppercase()
    val isOnline = status == "ONLINE"
    val isOnDelivery = status == "ON_DELIVERY"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatarWithStatus(
            name = profile.name,
            photoUrl = profile.profilePhotoUrl,
            showOnlineDot = isOnline || isOnDelivery,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name.ifBlank { "Repartidor" },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DobbyGoColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            ConnectionStatusBadge(status = status)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (isOnDelivery) {
                Text(
                    text = "En reparto",
                    style = MaterialTheme.typography.labelSmall,
                    color = DobbyGoColors.TextSecondary,
                    textAlign = TextAlign.End,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No disponible",
                    style = MaterialTheme.typography.labelSmall,
                    color = DobbyGoColors.Orange,
                    textAlign = TextAlign.End,
                )
            } else {
                Text(
                    text = "Disponible\npara pedidos",
                    style = MaterialTheme.typography.labelSmall,
                    color = DobbyGoColors.TextSecondary,
                    textAlign = TextAlign.End,
                    lineHeight = 14.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Switch(
                    checked = isOnline,
                    onCheckedChange = { on -> if (!statusUpdating) onToggleAvailability(on) },
                    enabled = !statusUpdating,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DobbyGoColors.Purple,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = DobbyGoColors.Border,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatarWithStatus(
    name: String,
    photoUrl: String?,
    showOnlineDot: Boolean,
) {
    Box {
        val resolvedUrl = resolveProfileImageUrl(photoUrl)
        if (resolvedUrl != null) {
            AsyncImage(
                model = resolvedUrl,
                contentDescription = name.ifBlank { "Foto de perfil" },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(DobbyGoColors.PurpleLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = DobbyGoColors.Purple,
                )
            }
        }
        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(DobbyGoColors.Green)
                    .border(2.dp, DobbyGoColors.Surface, CircleShape),
            )
        }
    }
}

@Composable
private fun ConnectionStatusBadge(status: String) {
    val (label, bg, dotColor, textColor) = when (status.uppercase()) {
        "ONLINE" -> StatusBadgeStyle(
            label = "Conectado",
            bg = DobbyGoColors.GreenLight,
            dotColor = DobbyGoColors.Green,
            textColor = DobbyGoColors.Green,
        )
        "ON_DELIVERY" -> StatusBadgeStyle(
            label = "En reparto",
            bg = DobbyGoColors.BlueLight,
            dotColor = DobbyGoColors.Blue,
            textColor = DobbyGoColors.Blue,
        )
        else -> StatusBadgeStyle(
            label = "Desconectado",
            bg = DobbyGoColors.Background,
            dotColor = DobbyGoColors.TextSecondary,
            textColor = DobbyGoColors.TextSecondary,
        )
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
        }
    }
}

private data class StatusBadgeStyle(
    val label: String,
    val bg: Color,
    val dotColor: Color,
    val textColor: Color,
)

@Composable
private fun ProfileLevelSection(profile: DeliveryProfileDto) {
    val next = profile.xpForNextLevel
    val span = if (next != null) (next - profile.xpAtCurrentLevel).coerceAtLeast(1) else 1
    val within = (profile.xp - profile.xpAtCurrentLevel).coerceAtLeast(0)
    val progress = if (next == null) 1f else (within.toFloat() / span).coerceIn(0f, 1f)
    val progressPercent = (progress * 100).roundToInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DobbyGoColors.PurpleLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = DobbyGoColors.Purple,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Nivel: ${levelDisplayName(profile.levelKey)}",
                    fontWeight = FontWeight.Bold,
                    color = DobbyGoColors.Purple,
                    fontSize = 15.sp,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DobbyGoColors.Purple,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (next != null) {
                    "XP: ${formatInt(profile.xp)} / ${formatInt(next)}"
                } else {
                    "XP: ${formatInt(profile.xp)} (nivel máximo)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = DobbyGoColors.TextSecondary,
            )
            if (next != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = DobbyGoColors.Purple,
                        trackColor = DobbyGoColors.PurpleLight,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$progressPercent%",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = DobbyGoColors.Purple,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatsRow(profile: DeliveryProfileDto) {
    val successRate = successRatePercent(profile.rating, profile.totalDeliveries)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        ProfileStatItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Star,
            iconTint = DobbyGoColors.Purple,
            value = String.format(Locale.US, "%.1f", profile.rating),
            label = "Rating",
            caption = "${profile.ratingCount} valoraciones",
        )
        VerticalDivider(
            modifier = Modifier.height(56.dp),
            color = DobbyGoColors.Border,
            thickness = 1.dp,
        )
        ProfileStatItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.LocalFireDepartment,
            iconTint = DobbyGoColors.Orange,
            value = formatInt(profile.currentStreakDays),
            label = "Racha",
            caption = "días",
        )
        VerticalDivider(
            modifier = Modifier.height(56.dp),
            color = DobbyGoColors.Border,
            thickness = 1.dp,
        )
        ProfileStatItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ShoppingBag,
            iconTint = DobbyGoColors.Purple,
            value = formatInt(profile.totalDeliveries),
            label = "Entregas",
            caption = "completadas",
        )
        VerticalDivider(
            modifier = Modifier.height(56.dp),
            color = DobbyGoColors.Border,
            thickness = 1.dp,
        )
        ProfileStatItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Verified,
            iconTint = DobbyGoColors.Green,
            value = successRate?.let { "$it%" } ?: "—",
            label = "Tasa de éxito",
            caption = successRate?.let { successRateLabel(it) } ?: "—",
        )
    }
}

@Composable
private fun ProfileStatItem(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = DobbyGoColors.TextPrimary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = DobbyGoColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = DobbyGoColors.Purple,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun ProfileMissionCard(mission: DeliveryMissionDto) {
    val progress = if (mission.goal > 0) {
        (mission.progress.toFloat() / mission.goal).coerceIn(0f, 1f)
    } else {
        0f
    }
    val missionIcon = when (mission.id) {
        "quick_deliveries_today" -> Icons.Default.Bolt
        else -> Icons.Default.Schedule
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DobbyGoColors.Background,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DobbyGoColors.PurpleLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = missionIcon,
                        contentDescription = null,
                        tint = DobbyGoColors.Purple,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.title,
                        fontWeight = FontWeight.Bold,
                        color = DobbyGoColors.TextPrimary,
                        fontSize = 15.sp,
                    )
                    missionSubtitle(mission.id)?.let { subtitle ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = DobbyGoColors.TextSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = missionProgressLabel(mission),
                        style = MaterialTheme.typography.bodySmall,
                        color = DobbyGoColors.TextSecondary,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DobbyGoColors.PurpleLight,
                ) {
                    Text(
                        text = "+${missionXpReward(mission.id)} XP",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = DobbyGoColors.Purple,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DobbyGoColors.TextSecondary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(22.dp),
                )
            }
            if (mission.goal > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = DobbyGoColors.Purple,
                    trackColor = DobbyGoColors.PurpleLight,
                )
            }
        }
    }
}
