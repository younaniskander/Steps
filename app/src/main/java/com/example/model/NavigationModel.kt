package com.example.model

enum class AppMode {
    WALK,        // Live camera obstacle scanning and quick alerts
    NAVIGATE,    // Turn-by-turn walking guidance to destination
    EXPLORE,     // Deep environment scene description
    SETTINGS     // Accessibility, speech rate, haptic settings
}

data class NavigationTarget(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String
)

data class NavigationGuide(
    val destination: NavigationTarget?,
    val distanceMeters: Float?,
    val bearingDegrees: Float?,
    val relativeDirection: String?,
    val spokenInstruction: String?
)
