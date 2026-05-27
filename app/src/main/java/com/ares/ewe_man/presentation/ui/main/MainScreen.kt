package com.ares.ewe_man.presentation.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.ewe_man.core.theme.DobbyGoColors
import com.ares.ewe_man.presentation.ui.orders.OrdersScreen
import com.ares.ewe_man.presentation.ui.profile.ProfileScreen

sealed class MainTab(
    val title: String,
    val icon: ImageVector,
) {
    data object Orders : MainTab("Pedidos", Icons.Default.List)
    data object Profile : MainTab("Perfil", Icons.Default.Person)
}

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onOrderClick: (orderId: String) -> Unit,
    refreshOrdersTrigger: Int = 0,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(MainTab.Orders, MainTab.Profile)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DobbyGoColors.Background),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            when (selectedTab) {
                0 -> OrdersScreen(
                    modifier = Modifier.fillMaxSize(),
                    onOrderClick = onOrderClick,
                    refreshTrigger = refreshOrdersTrigger,
                )
                1 -> ProfileScreen(onLogout = onLogout)
            }
        }
        DobbyGoBottomBar(
            tabs = tabs,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it },
        )
    }
}

@Composable
private fun DobbyGoBottomBar(
    tabs: List<MainTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = DobbyGoColors.Surface,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEachIndexed { index, tab ->
                DobbyGoBottomBarItem(
                    tab = tab,
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun DobbyGoBottomBarItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val iconColor = if (selected) DobbyGoColors.Purple else DobbyGoColors.TextSecondary
    val textColor = if (selected) DobbyGoColors.Purple else DobbyGoColors.TextSecondary
    val pillColor = if (selected) DobbyGoColors.PurpleLight else Color.Transparent

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(pillColor)
                .padding(horizontal = 18.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = iconColor,
            )
        }
        Text(
            text = tab.title,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
