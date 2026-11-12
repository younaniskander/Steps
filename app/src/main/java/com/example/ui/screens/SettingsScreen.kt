package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AIModelEngine
import com.example.ui.StepSightUiState
import com.example.ui.StepSightViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.HazardRed
import com.example.ui.theme.HighVisYellow
import com.example.ui.theme.NavBlue
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextHighContrast
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun SettingsScreen(
    uiState: StepSightUiState,
    viewModel: StepSightViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Assistant Settings",
            color = HighVisYellow,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )

        // AI Vision & OCR Engine Selection Card
        var inputToken by remember(uiState.hfApiToken) { mutableStateOf(uiState.hfApiToken) }
        var inputEndpoint by remember(uiState.customOcrEndpoint) { mutableStateOf(uiState.customOcrEndpoint) }

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HighVisYellow.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = HighVisYellow,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AI Vision & OCR Model",
                        color = TextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Choose which model analyzes camera obstacles and performs OCR on signs:",
                    color = TextMuted,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedModelEngine == AIModelEngine.GEMINI_3_8_FLASH,
                        onClick = { viewModel.setAiModelEngine(AIModelEngine.GEMINI_3_8_FLASH) },
                        label = { Text("Gemini 3.8 Flash (Free)", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HighVisYellow,
                            selectedLabelColor = TextDark,
                            containerColor = DarkSurfaceCard,
                            labelColor = TextWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = uiState.selectedModelEngine == AIModelEngine.NEXT_OCR_THELAMAPI,
                        onClick = { viewModel.setAiModelEngine(AIModelEngine.NEXT_OCR_THELAMAPI) },
                        label = { Text("Next-OCR (thelamapi)", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavBlue,
                            selectedLabelColor = TextDark,
                            containerColor = DarkSurfaceCard,
                            labelColor = TextWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.selectedModelEngine == AIModelEngine.NEXT_OCR_THELAMAPI) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceCard, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Next-OCR (thelamapi/next-ocr) Settings",
                            color = NavBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Connect via Hugging Face Inference API or your local Python backend server:",
                            color = TextHighContrast,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = inputToken,
                            onValueChange = { inputToken = it },
                            label = { Text("Hugging Face API Token (hf_...)", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavBlue,
                                unfocusedBorderColor = DarkSurface,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = inputEndpoint,
                            onValueChange = { inputEndpoint = it },
                            label = { Text("Model Endpoint URL", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavBlue,
                                unfocusedBorderColor = DarkSurface,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.setHfApiToken(inputToken)
                                viewModel.setCustomOcrEndpoint(inputEndpoint)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavBlue,
                                contentColor = TextDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SAVE NEXT-OCR CONFIG", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Feedback Toggles Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Spoken Voice Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = HighVisYellow,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Spoken Guidance (TTS)",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Speaks obstacle alerts and navigation",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Switch(
                        checked = uiState.audioEnabled,
                        onCheckedChange = { viewModel.toggleVoice(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextDark,
                            checkedTrackColor = HighVisYellow,
                            uncheckedTrackColor = DarkSurfaceCard
                        )
                    )
                }

                // Haptic Feedback Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = NavBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Haptic Vibrations",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tactile alerts for obstacles and turns",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Switch(
                        checked = uiState.hapticEnabled,
                        onCheckedChange = { viewModel.toggleHaptic(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextDark,
                            checkedTrackColor = NavBlue,
                            uncheckedTrackColor = DarkSurfaceCard
                        )
                    )
                }
            }
        }

        // Camera Scan Frequency Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = HighVisYellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Automatic Scan Interval",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Controls how often the camera analyzes the forward path hands-free.",
                    color = TextMuted,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 3, 5, 8).forEach { seconds ->
                        FilterChip(
                            selected = uiState.scanIntervalSeconds == seconds,
                            onClick = { viewModel.setScanInterval(seconds) },
                            label = { Text("${seconds}s", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HighVisYellow,
                                selectedLabelColor = TextDark,
                                containerColor = DarkSurfaceCard,
                                labelColor = TextWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Speech Rate Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Voice Speech Speed",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.85f to "Slow", 1.0f to "Normal", 1.25f to "Fast", 1.5f to "Very Fast").forEach { (rate, label) ->
                        FilterChip(
                            selected = uiState.speechRate == rate,
                            onClick = { viewModel.setSpeechRate(rate) },
                            label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SafeGreen,
                                selectedLabelColor = TextDark,
                                containerColor = DarkSurfaceCard,
                                labelColor = TextWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Tactile Vibration Tests
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Test Vibration Alerts",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.audioHapticManager.vibrateCritical() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HazardRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Danger (3x)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.audioHapticManager.vibrateCaution() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HighVisYellow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Caution (2x)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.audioHapticManager.vibrateClear() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SafeGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clear (1x)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // About Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = HighVisYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "StepSight v1.0",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Powered by Google Gemini 3.8 Flash & Firebase AI Logic. Designed with TalkBack accessibility.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
