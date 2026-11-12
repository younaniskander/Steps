package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppMode
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.NavigationScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WalkModeScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.HighVisYellow
import com.example.ui.theme.NavBlue
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: StepSightViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.currentMode != AppMode.WALK) {
        viewModel.setAppMode(AppMode.WALK)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(DarkBackground),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "StepSight",
                        fontWeight = FontWeight.ExtraBold,
                        color = HighVisYellow,
                        fontSize = 22.sp,
                        letterSpacing = 1.sp
                    )
                },
                actions = {
                    // Audio Guidance Indicator / Tap to repeat latest guidance
                    IconButton(
                        onClick = {
                            viewModel.audioHapticManager.speak(uiState.latestSpokenAlert, isUrgent = true)
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(44.dp)
                            .semantics {
                                contentDescription = "Repeat spoken guidance alert"
                            }
                    ) {
                        Surface(
                            color = if (uiState.audioEnabled) HighVisYellow else DarkSurface,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = if (uiState.audioEnabled) TextDark else TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(1.dp, HighVisYellow.copy(alpha = 0.2f))
            ) {
                NavigationBarItem(
                    selected = uiState.currentMode == AppMode.WALK,
                    onClick = { viewModel.setAppMode(AppMode.WALK) },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = "Walk Obstacle Mode",
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Walk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextDark,
                        selectedTextColor = HighVisYellow,
                        indicatorColor = HighVisYellow,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_walk_tab")
                )

                NavigationBarItem(
                    selected = uiState.currentMode == AppMode.NAVIGATE,
                    onClick = { viewModel.setAppMode(AppMode.NAVIGATE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Navigation Mode",
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Navigate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextDark,
                        selectedTextColor = NavBlue,
                        indicatorColor = NavBlue,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_navigate_tab")
                )

                NavigationBarItem(
                    selected = uiState.currentMode == AppMode.EXPLORE,
                    onClick = { viewModel.setAppMode(AppMode.EXPLORE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Explore Surroundings",
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Explore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextDark,
                        selectedTextColor = SafeGreen,
                        indicatorColor = SafeGreen,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_explore_tab")
                )

                NavigationBarItem(
                    selected = uiState.currentMode == AppMode.SETTINGS,
                    onClick = { viewModel.setAppMode(AppMode.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextDark,
                        selectedTextColor = HighVisYellow,
                        indicatorColor = HighVisYellow,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentMode) {
                AppMode.WALK -> WalkModeScreen(uiState = uiState, viewModel = viewModel)
                AppMode.NAVIGATE -> NavigationScreen(uiState = uiState, viewModel = viewModel)
                AppMode.EXPLORE -> ExploreScreen(uiState = uiState, viewModel = viewModel)
                AppMode.SETTINGS -> SettingsScreen(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}
