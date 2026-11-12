package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StepSightUiState
import com.example.ui.StepSightViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.HighVisYellow
import com.example.ui.theme.NavBlue
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextHighContrast
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun ExploreScreen(
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
        // Header
        Text(
            text = "Environment Reader",
            color = HighVisYellow,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "StepSight uses Gemini vision to describe doors, signs, crosswalks, text, and landmark objects.",
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        // Giant Primary Action Button (72dp height)
        Button(
            onClick = { viewModel.describeSurroundings() },
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .testTag("describe_surroundings_main_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = NavBlue,
                contentColor = TextDark
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            if (uiState.isDescribingScene) {
                CircularProgressIndicator(
                    color = TextDark,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ANALYZING SURROUNDINGS...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "DESCRIBE WHAT IS AHEAD",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Scene Description Result Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, NavBlue.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .semantics {
                    contentDescription = "Scene description: ${uiState.sceneDescription ?: "No scan performed yet. Tap describe button to analyze."}"
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
                    Text(
                        text = "SCENE NARRATION",
                        color = NavBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )

                    if (uiState.sceneDescription != null) {
                        Surface(
                            color = NavBlue.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Spoken out loud",
                                tint = NavBlue,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = uiState.sceneDescription
                        ?: "Hold your phone facing straight forward and tap 'Describe What Is Ahead'. StepSight will identify entrances, crosswalks, read street or door signs, and summarize your surroundings.",
                    color = if (uiState.sceneDescription != null) TextWhite else TextMuted,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    fontWeight = if (uiState.sceneDescription != null) FontWeight.SemiBold else FontWeight.Normal
                )

                if (uiState.sceneDescription != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.audioHapticManager.speak(uiState.sceneDescription!!, isUrgent = true)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = HighVisYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REPEAT SPOKEN DESCRIPTION",
                            color = HighVisYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Accessibility Tips Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Tips for Best Results",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Hold device steady at mid-chest height.\n• Angle slightly downward to capture ground curbs & obstacles.\n• Move camera slowly to read signs or doorway text.",
                        color = TextHighContrast,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
