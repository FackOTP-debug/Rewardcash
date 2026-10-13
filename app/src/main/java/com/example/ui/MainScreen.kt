package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.viewmodel.RewardViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainScreen(
    viewModel: RewardViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.userFeedbackEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Stars, contentDescription = "Earn") },
                    label = { Text("Earn") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = Color(0xFFFFB300),
                        indicatorColor = Color(0xFFFFB300),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_earn")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Games, contentDescription = "Game Hub") },
                    label = { Text("Game Hub") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = Color(0xFFFFB300),
                        indicatorColor = Color(0xFFFFB300),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_games")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
                    label = { Text("Wallet") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = Color(0xFFFFB300),
                        indicatorColor = Color(0xFFFFB300),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_wallet")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = Color(0xFFFFB300),
                        indicatorColor = Color(0xFFFFB300),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            label = "tab_crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToGameHub = { selectedTab = 1 }
                )
                1 -> GameHubScreen(
                    viewModel = viewModel
                )
                2 -> WalletScreen(
                    viewModel = viewModel
                )
                3 -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
