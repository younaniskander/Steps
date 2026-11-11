package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationGuide
import com.example.model.NavigationTarget
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
import kotlin.math.roundToInt

@Composable
fun NavigationScreen(
    uiState: StepSightUiState,
    viewModel: StepSightViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val activeGuide = uiState.navigationGuide

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Active Navigation Guidance Card (if navigating)
        if (activeGuide?.destination != null) {
            ActiveGuidanceCard(
                guide = activeGuide,
                compassAzimuth = uiState.compassAzimuth,
                onRepeatVoice = {
                    activeGuide.spokenInstruction?.let {
                        viewModel.audioHapticManager.speak(it, isUrgent = true)
                    }
                },
                onStop = { viewModel.setDestination(null) }
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Search Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search destination or street...", color = TextMuted) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("destination_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HighVisYellow,
                    unfocusedBorderColor = DarkSurfaceCard,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { viewModel.searchPlaces(searchQuery) },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("search_places_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HighVisYellow,
                    contentColor = TextDark
                )
            ) {
                if (uiState.isSearching) {
                    CircularProgressIndicator(
                        color = TextDark,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search places")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Preset or Search Results Title
        Text(
            text = if (uiState.searchResults.isNotEmpty()) "Search Results" else "Quick Destinations",
            color = HighVisYellow,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Places List
        val displayList = if (uiState.searchResults.isNotEmpty()) uiState.searchResults else viewModel.presetDestinations

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(displayList) { target ->
                PlaceCard(
                    target = target,
                    isSelected = activeGuide?.destination?.id == target.id,
                    onSelect = { viewModel.setDestination(target) }
                )
            }
        }
    }
}

@Composable
private fun ActiveGuidanceCard(
    guide: NavigationGuide,
    compassAzimuth: Float,
    onRepeatVoice: () -> Unit,
    onStop: () -> Unit
) {
    val bearing = guide.bearingDegrees ?: 0f
    val relativeAngle = (bearing - compassAzimuth)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, HighVisYellow, RoundedCornerShape(20.dp))
            .semantics {
                contentDescription = "Active guidance to ${guide.destination?.name}: ${guide.spokenInstruction}"
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = HighVisYellow,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = TextDark,
                                modifier = Modifier
                                    .size(28.dp)
                                    .rotate(relativeAngle)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = guide.destination?.name ?: "Destination",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = guide.relativeDirection ?: "Walking",
                            color = HighVisYellow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(38.dp)
                        .background(HazardRed.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop navigation",
                        tint = HazardRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Distance & Steps Estimate
            guide.distanceMeters?.let { dist ->
                val approxSteps = (dist / 0.75f).roundToInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${dist.roundToInt()} meters away",
                        color = TextHighContrast,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "~$approxSteps steps",
                        color = SafeGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Spoken Instruction Box
            guide.spokenInstruction?.let { instruction ->
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRepeatVoice() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speak direction",
                            tint = HighVisYellow,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = instruction,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceCard(
    target: NavigationTarget,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val icon: ImageVector = when (target.category.lowercase()) {
        "transit" -> Icons.Default.Train
        "health" -> Icons.Default.LocalPharmacy
        "shopping" -> Icons.Default.ShoppingCart
        "park" -> Icons.Default.Park
        else -> Icons.Default.DirectionsWalk
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HighVisYellow.copy(alpha = 0.15f) else DarkSurface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) HighVisYellow else DarkSurfaceCard,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelect() }
            .semantics {
                contentDescription = "${target.name}, ${target.category}, ${target.address}. Tap to navigate."
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (isSelected) HighVisYellow else DarkSurfaceCard,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) TextDark else HighVisYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = target.name,
                        color = TextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${target.category} · ${target.address}",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            Button(
                onClick = onSelect,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) SafeGreen else HighVisYellow,
                    contentColor = TextDark
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = if (isSelected) "GUIDING" else "START",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
