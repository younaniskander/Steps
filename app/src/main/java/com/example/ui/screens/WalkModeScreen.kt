package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.HazardUrgency
import com.example.ui.StepSightUiState
import com.example.ui.StepSightViewModel
import com.example.ui.components.CameraPreviewView
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.HazardRed
import com.example.ui.theme.HighVisYellow
import com.example.ui.theme.NavBlue
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextHighContrast
import com.example.ui.theme.TextWhite

@Composable
fun WalkModeScreen(
    uiState: StepSightUiState,
    viewModel: StepSightViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        // Camera Viewfinder Background
        if (hasCameraPermission && !uiState.isSafetySimMode) {
            CameraPreviewView(
                onFrameCaptured = { bitmap ->
                    viewModel.onNewCameraFrame(bitmap)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Simulated / No-Camera background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF101720), Color(0xFF080C10))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (uiState.isSafetySimMode) Icons.Default.Warning else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = if (uiState.isSafetySimMode) CautionAmber else HighVisYellow,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (uiState.isSafetySimMode) "Safety Simulation Active (${uiState.activeSimScenario})"
                        else "Camera Initializing...",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Semi-transparent scrim to guarantee high contrast for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // Main UI Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Location Bar & Live Compass
            Column {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.92f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, HighVisYellow.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .semantics {
                            contentDescription = "Current location: ${uiState.currentAddress}. Heading: ${uiState.compassAzimuth.toInt()} degrees."
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location Pin",
                                tint = HighVisYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.currentAddress,
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        // Compass Heading Chip
                        Surface(
                            color = DarkSurfaceCard,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = "Compass",
                                    tint = NavBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${uiState.compassAzimuth.toInt()}°",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulation Scenario Selector (Essential for testing & zero-camera verification)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !uiState.isSafetySimMode,
                        onClick = { viewModel.setSimulatedScenario(null) },
                        label = { Text("Live Camera", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HighVisYellow,
                            selectedLabelColor = TextDark,
                            containerColor = DarkSurfaceCard,
                            labelColor = TextWhite
                        )
                    )
                    FilterChip(
                        selected = uiState.activeSimScenario == "curb_dropoff",
                        onClick = { viewModel.setSimulatedScenario("curb_dropoff") },
                        label = { Text("Sim: Curb Drop", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CautionAmber,
                            selectedLabelColor = TextDark,
                            containerColor = DarkSurfaceCard,
                            labelColor = TextWhite
                        )
                    )
                    FilterChip(
                        selected = uiState.activeSimScenario == "stairs_down",
                        onClick = { viewModel.setSimulatedScenario("stairs_down") },
                        label = { Text("Sim: Stairs Down", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HazardRed,
                            selectedLabelColor = TextWhite,
                            containerColor = DarkSurfaceCard,
                            labelColor = TextWhite
                        )
                    )
                    FilterChip(
                        selected = uiState.activeSimScenario == "bicycle_hazard",
                        onClick = { viewModel.setSimulatedScenario("bicycle_hazard") },
                        label = { Text("Sim: Obstacle", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CautionAmber,
                            selectedLabelColor = TextDark,
                            containerColor = DarkSurfaceCard,
                            labelColor = TextWhite
                        )
                    )
                    FilterChip(
                        selected = uiState.activeSimScenario == "clear_path",
                        onClick = { viewModel.setSimulatedScenario("clear_path") },
                        label = { Text("Sim: Clear Path", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SafeGreen,
                            selectedLabelColor = TextDark,
                            containerColor = DarkSurfaceCard,
                            labelColor = TextWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Center Section: Primary High-Visibility Obstacle Assessment Card
            val urgency = uiState.lastObstacle.urgency
            val (bannerBg, bannerFg, bannerBorder) = when {
                !uiState.isScanningActive -> Triple(Color(0xFF282828), TextWhite, Color.Gray)
                urgency == HazardUrgency.CRITICAL -> Triple(HazardRed, TextWhite, Color.White)
                urgency == HazardUrgency.CAUTION -> Triple(HighVisYellow, TextDark, Color.Black)
                urgency == HazardUrgency.INFO -> Triple(NavBlue, TextDark, Color.White)
                else -> Triple(SafeGreen, TextDark, Color.Black)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = bannerBg),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, bannerBorder, RoundedCornerShape(20.dp))
                    .clickable { viewModel.audioHapticManager.speak(uiState.latestSpokenAlert, isUrgent = true) }
                    .testTag("obstacle_status_card")
                    .semantics {
                        contentDescription = "Obstacle status: ${uiState.latestSpokenAlert}. Tap to hear again."
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (urgency) {
                                    HazardUrgency.CRITICAL, HazardUrgency.CAUTION -> Icons.Default.Warning
                                    HazardUrgency.INFO -> Icons.Default.Explore
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = bannerFg,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when {
                                    !uiState.isScanningActive -> "SCANNING PAUSED"
                                    urgency == HazardUrgency.CRITICAL -> "HAZARD DETECTED"
                                    urgency == HazardUrgency.CAUTION -> "CAUTION AHEAD"
                                    urgency == HazardUrgency.INFO -> "ENVIRONMENT INFO"
                                    else -> "PATH CLEAR"
                                },
                                color = bannerFg,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Scanning Indicator Pulse or Audio Button
                        IconButton(
                            onClick = {
                                viewModel.audioHapticManager.speak(uiState.latestSpokenAlert, isUrgent = true)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(bannerFg.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Hear alert aloud",
                                tint = bannerFg
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Giant Spoken Alert Message
                    Text(
                        text = uiState.latestSpokenAlert,
                        color = bannerFg,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Direction & Recommendation Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = bannerFg.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Direction: ${uiState.lastObstacle.clockPosition}",
                                color = bannerFg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            color = bannerFg.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = uiState.lastObstacle.distanceEstimate,
                                color = bannerFg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (uiState.lastObstacle.recommendation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Guidance: ${uiState.lastObstacle.recommendation}",
                            color = bannerFg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Section: Giant Accessible Action Controls (Minimum 68dp touch targets)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Giant Scan Now Button
                Button(
                    onClick = { viewModel.triggerImmediateScan() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .testTag("scan_now_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HighVisYellow,
                        contentColor = TextDark
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (uiState.isProcessingFrame) {
                        CircularProgressIndicator(
                            color = TextDark,
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ANALYZING PATH...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SCAN PATH NOW",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Row with Pause/Resume and Describe Surroundings Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Toggle Scan Pause/Resume
                    OutlinedButton(
                        onClick = { viewModel.toggleScanning() },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("toggle_scan_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DarkSurfaceCard,
                            contentColor = TextWhite
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(HighVisYellow, NavBlue))
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.isScanningActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (uiState.isScanningActive) CautionAmber else SafeGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isScanningActive) "PAUSE SCAN" else "RESUME",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick Surroundings Describer
                    Button(
                        onClick = { viewModel.describeSurroundings() },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("describe_scene_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavBlue,
                            contentColor = TextDark
                        )
                    ) {
                        if (uiState.isDescribingScene) {
                            CircularProgressIndicator(
                                color = TextDark,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DESCRIBE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
